from __future__ import annotations

import http.client
import json
import random
import socket
import ssl
import struct
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from typing import Any

from .models import (
  DnsObservation,
  FtpObservation,
  HttpObservation,
  RegistrationObservation,
  ResolverResult,
)
from .url_utils import (
  extract_tld,
  is_blocked_public_probe_ip,
  parse_ip,
  validate_public_probe_target,
)

DOH_RESOLVERS: list[tuple[str, str]] = [
  ("cloudflare", "https://cloudflare-dns.com/dns-query"),
  ("google", "https://dns.google/resolve"),
  ("quad9", "https://dns9.quad9.net:5053/dns-query"),
]

RCODE_MAP = {
  0: "NOERROR",
  1: "FORMERR",
  2: "SERVFAIL",
  3: "NXDOMAIN",
  4: "NOTIMP",
  5: "REFUSED",
}

QTYPE_MAP = {
  "A": 1,
  "NS": 2,
  "CNAME": 5,
  "SOA": 6,
  "AAAA": 28,
}

IANA_RDAP_BOOTSTRAP_URL = "https://data.iana.org/rdap/dns.json"


def utc_now_iso() -> str:
  return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def query_doh_json(
  domain: str,
  record_type: str,
  resolver_name: str,
  endpoint: str,
  timeout_sec: float = 4.0,
) -> ResolverResult:
  url = f"{endpoint}?name={urllib.parse.quote(domain)}&type={urllib.parse.quote(record_type)}"
  req = urllib.request.Request(
    url,
    headers={"Accept": "application/dns-json", "User-Agent": "BDIX-Tester-Verifier/1.0"},
  )
  now = utc_now_iso()
  try:
    with urllib.request.urlopen(req, timeout=timeout_sec) as resp:
      body = json.loads(resp.read(65536).decode("utf-8", errors="replace"))
    rcode_num = int(body.get("Status", 0))
    rcode = RCODE_MAP.get(rcode_num, f"RCODE_{rcode_num}")
    answers: list[str] = []
    min_ttl: int | None = None
    for ans in body.get("Answer", []) or []:
      data = str(ans.get("data", "")).strip().rstrip(".")
      if data:
        answers.append(data)
      ttl = ans.get("TTL")
      if isinstance(ttl, int):
        min_ttl = ttl if min_ttl is None else min(min_ttl, ttl)
    return ResolverResult(
      resolver_name=resolver_name,
      resolver_endpoint=endpoint,
      record_type=record_type,
      rcode=rcode,
      answers=answers,
      ttl_seconds=min_ttl,
      checked_at_utc=now,
    )
  except urllib.error.HTTPError as exc:
    return ResolverResult(
      resolver_name=resolver_name,
      resolver_endpoint=endpoint,
      record_type=record_type,
      rcode="ERROR",
      error=f"HTTP {exc.code}",
      checked_at_utc=now,
    )
  except (TimeoutError, socket.timeout):
    return ResolverResult(
      resolver_name=resolver_name,
      resolver_endpoint=endpoint,
      record_type=record_type,
      rcode="TIMEOUT",
      error="DoH request timed out",
      checked_at_utc=now,
    )
  except Exception as exc:
    return ResolverResult(
      resolver_name=resolver_name,
      resolver_endpoint=endpoint,
      record_type=record_type,
      rcode="ERROR",
      error=str(exc)[:180],
      checked_at_utc=now,
    )


def _encode_dns_name(name: str) -> bytes:
  out = bytearray()
  for label in name.strip(".").split("."):
    encoded = label.encode("ascii", errors="ignore")[:63]
    out.append(len(encoded))
    out.extend(encoded)
  out.append(0)
  return bytes(out)


def _decode_dns_name(packet: bytes, offset: int) -> tuple[str, int]:
  labels: list[str] = []
  jumped = False
  next_offset = offset
  seen_offsets: set[int] = set()
  while offset < len(packet):
    if offset in seen_offsets:
      break
    seen_offsets.add(offset)
    length = packet[offset]
    if length == 0:
      if not jumped:
        next_offset = offset + 1
      break
    if (length & 0xC0) == 0xC0:
      if offset + 1 >= len(packet):
        break
      ptr = ((length & 0x3F) << 8) | packet[offset + 1]
      if not jumped:
        next_offset = offset + 2
      offset = ptr
      jumped = True
      continue
    offset += 1
    labels.append(packet[offset : offset + length].decode("ascii", errors="replace"))
    offset += length
  return ".".join(labels), next_offset


def query_udp_dns(
  nameserver_host: str,
  domain: str,
  record_type: str = "A",
  timeout_sec: float = 3.0,
) -> ResolverResult:
  now = utc_now_iso()
  qtype = QTYPE_MAP.get(record_type.upper(), 1)
  txid = random.randint(1, 65535)
  header = struct.pack("!HHHHHH", txid, 0x0000, 1, 0, 0, 0)  # RD=0 for authoritative query
  question = _encode_dns_name(domain) + struct.pack("!HH", qtype, 1)
  packet = header + question

  try:
    infos = socket.getaddrinfo(nameserver_host, 53, type=socket.SOCK_DGRAM)
    target_ip = None
    family = socket.AF_INET
    for info in infos:
      cand = info[4][0]
      blocked, _ = is_blocked_public_probe_ip(cand)
      if not blocked:
        target_ip = cand
        family = info[0]
        break
    if not target_ip:
      return ResolverResult(
        resolver_name=f"authoritative:{nameserver_host}",
        resolver_endpoint=nameserver_host,
        record_type=record_type,
        rcode="ERROR",
        error="Nameserver resolved to blocked or non-public IP",
        checked_at_utc=now,
      )
    with socket.socket(family, socket.SOCK_DGRAM) as sock:
      sock.settimeout(timeout_sec)
      sock.sendto(packet, (target_ip, 53))
      resp, _ = sock.recvfrom(4096)
    if len(resp) < 12:
      return ResolverResult(
        resolver_name=f"authoritative:{nameserver_host}",
        resolver_endpoint=target_ip,
        record_type=record_type,
        rcode="ERROR",
        error="Short DNS response",
        checked_at_utc=now,
      )
    _, flags, qdcount, ancount, _, _ = struct.unpack("!HHHHHH", resp[:12])
    rcode = RCODE_MAP.get(flags & 0x0F, f"RCODE_{flags & 0x0F}")
    pos = 12
    for _ in range(qdcount):
      _, pos = _decode_dns_name(resp, pos)
      pos += 4
    answers: list[str] = []
    min_ttl: int | None = None
    for _ in range(ancount):
      if pos >= len(resp):
        break
      _, pos = _decode_dns_name(resp, pos)
      if pos + 10 > len(resp):
        break
      rtype, _, ttl, rdlen = struct.unpack("!HHIH", resp[pos : pos + 10])
      pos += 10
      rdata = resp[pos : pos + rdlen]
      pos += rdlen
      min_ttl = ttl if min_ttl is None else min(min_ttl, ttl)
      if rtype == 1 and rdlen == 4:
        answers.append(socket.inet_ntop(socket.AF_INET, rdata))
      elif rtype == 28 and rdlen == 16:
        answers.append(socket.inet_ntop(socket.AF_INET6, rdata))
      elif rtype in (2, 5):
        cname_or_ns, _ = _decode_dns_name(resp, pos - rdlen)
        if cname_or_ns:
          answers.append(cname_or_ns)
    return ResolverResult(
      resolver_name=f"authoritative:{nameserver_host}",
      resolver_endpoint=target_ip,
      record_type=record_type,
      rcode=rcode,
      answers=answers,
      ttl_seconds=min_ttl,
      checked_at_utc=now,
    )
  except (TimeoutError, socket.timeout):
    return ResolverResult(
      resolver_name=f"authoritative:{nameserver_host}",
      resolver_endpoint=nameserver_host,
      record_type=record_type,
      rcode="TIMEOUT",
      error="Authoritative DNS query timed out",
      checked_at_utc=now,
    )
  except Exception as exc:
    return ResolverResult(
      resolver_name=f"authoritative:{nameserver_host}",
      resolver_endpoint=nameserver_host,
      record_type=record_type,
      rcode="ERROR",
      error=str(exc)[:160],
      checked_at_utc=now,
    )


def verify_domain_dns(
  domain: str,
  registrable_domain: str | None = None,
  timeout_sec: float = 4.0,
) -> DnsObservation:
  now = utc_now_iso()
  resolver_results: list[ResolverResult] = []
  for name, endpoint in DOH_RESOLVERS:
    resolver_results.append(query_doh_json(domain, "A", name, endpoint, timeout_sec=timeout_sec))

  # Collect NS/SOA/AAAA/CNAME from primary healthy resolver
  ns_res = query_doh_json(registrable_domain or domain, "NS", "cloudflare", DOH_RESOLVERS[0][1], timeout_sec=timeout_sec)
  soa_res = query_doh_json(registrable_domain or domain, "SOA", "cloudflare", DOH_RESOLVERS[0][1], timeout_sec=timeout_sec)
  aaaa_res = query_doh_json(domain, "AAAA", "cloudflare", DOH_RESOLVERS[0][1], timeout_sec=timeout_sec)
  cname_res = query_doh_json(domain, "CNAME", "cloudflare", DOH_RESOLVERS[0][1], timeout_sec=timeout_sec)

  rcodes = [r.rcode for r in resolver_results]
  has_a_answers = any(r.rcode == "NOERROR" and bool(r.answers) for r in resolver_results)
  has_any_noerror = (
    any(r.rcode == "NOERROR" for r in resolver_results)
    or bool(aaaa_res.answers)
    or bool(cname_res.answers)
  )
  nx_count = sum(1 for r in resolver_results if r.rcode == "NXDOMAIN")
  temp_fail_count = sum(1 for r in resolver_results if r.rcode in {"SERVFAIL", "REFUSED", "TIMEOUT"})

  authoritative_results: list[ResolverResult] = []
  if nx_count >= 1 and ns_res.answers:
    for ns_host in ns_res.answers[:2]:
      authoritative_results.append(query_udp_dns(ns_host, domain, "A", timeout_sec=min(timeout_sec, 3.0)))

  if has_a_answers or bool(aaaa_res.answers) or bool(cname_res.answers):
    summary = "healthy"
  elif has_any_noerror and nx_count == 0 and temp_fail_count == 0:
    summary = "nodata"
  elif nx_count >= 2 and temp_fail_count == 0 and not has_any_noerror:
    if authoritative_results and all(a.rcode == "NXDOMAIN" for a in authoritative_results):
      summary = "auth_nxdomain"
    elif authoritative_results and any(a.rcode == "NOERROR" for a in authoritative_results):
      summary = "disagreed"
    else:
      summary = "multi_nxdomain"
  elif nx_count == 1 and has_any_noerror:
    summary = "disagreed"
  elif nx_count == 1:
    summary = "single_nxdomain"
  elif temp_fail_count > 0:
    summary = "servfail" if "SERVFAIL" in rcodes else "timeout"
  else:
    summary = "not_checked"

  all_results = resolver_results + [r for r in (aaaa_res, cname_res) if r.rcode != "NOERROR" or r.answers]
  return DnsObservation(
    domain=domain,
    resolver_results=all_results,
    authoritative_results=authoritative_results,
    ns_records=ns_res.answers,
    soa_records=soa_res.answers,
    summary=summary,
    checked_at_utc=now,
  )


class RdapClient:
  def __init__(self, timeout_sec: float = 5.0):
    self.timeout_sec = timeout_sec
    self._bootstrap: dict[str, list[str]] | None = None

  def _load_bootstrap(self) -> dict[str, list[str]]:
    if self._bootstrap is not None:
      return self._bootstrap
    req = urllib.request.Request(
      IANA_RDAP_BOOTSTRAP_URL,
      headers={"Accept": "application/json", "User-Agent": "BDIX-Tester-Verifier/1.0"},
    )
    mapping: dict[str, list[str]] = {}
    with urllib.request.urlopen(req, timeout=self.timeout_sec) as resp:
      data = json.loads(resp.read(512 * 1024).decode("utf-8", errors="replace"))
    for service in data.get("services", []):
      if not isinstance(service, list) or len(service) < 2:
        continue
      tlds, urls = service[0], service[1]
      for tld in tlds:
        mapping[str(tld).lower().lstrip(".")] = [str(u).rstrip("/") for u in urls]
    self._bootstrap = mapping
    return mapping

  def lookup_domain(self, registrable_domain: str | None) -> RegistrationObservation:
    now = utc_now_iso()
    if not registrable_domain or parse_ip(registrable_domain) is not None:
      return RegistrationObservation(
        domain=registrable_domain or "",
        lookup_status="not_applicable",
        limitation_note="IP literal or empty domain has no DNS registration record.",
        checked_at_utc=now,
      )

    tld = extract_tld(registrable_domain)
    if tld == "bd":
      return RegistrationObservation(
        domain=registrable_domain,
        lookup_status="registration_lookup_unavailable",
        authoritative_source="BTCL (.bd registry)",
        limitation_note=(
          "Official ICANN RDAP is not published for .bd domains (operated by BTCL); "
          "preserving conservative status without fabricated registration data."
        ),
        checked_at_utc=now,
      )

    try:
      bootstrap = self._load_bootstrap()
    except Exception as exc:
      return RegistrationObservation(
        domain=registrable_domain,
        lookup_status="lookup_error",
        authoritative_source=IANA_RDAP_BOOTSTRAP_URL,
        limitation_note=f"Could not fetch IANA RDAP bootstrap: {exc}",
        checked_at_utc=now,
      )

    base_urls = bootstrap.get(tld or "")
    if not base_urls:
      return RegistrationObservation(
        domain=registrable_domain,
        lookup_status="registration_lookup_unavailable",
        authoritative_source="IANA RDAP Bootstrap",
        limitation_note=f"TLD '.{tld}' does not advertise a public RDAP server in IANA bootstrap.",
        checked_at_utc=now,
      )

    rdap_url = f"{base_urls[0]}/domain/{urllib.parse.quote(registrable_domain)}"
    req = urllib.request.Request(
      rdap_url,
      headers={"Accept": "application/rdap+json, application/json", "User-Agent": "BDIX-Tester-Verifier/1.0"},
    )
    try:
      with urllib.request.urlopen(req, timeout=self.timeout_sec) as resp:
        payload = json.loads(resp.read(256 * 1024).decode("utf-8", errors="replace"))
      return parse_rdap_domain_payload(registrable_domain, base_urls[0], payload, now)
    except urllib.error.HTTPError as exc:
      if exc.code == 404:
        return RegistrationObservation(
          domain=registrable_domain,
          lookup_status="unregistered_404",
          authoritative_source=base_urls[0],
          limitation_note="Authoritative RDAP server returned HTTP 404 (domain not registered).",
          checked_at_utc=now,
        )
      return RegistrationObservation(
        domain=registrable_domain,
        lookup_status="lookup_error",
        authoritative_source=base_urls[0],
        limitation_note=f"RDAP server returned HTTP {exc.code}.",
        checked_at_utc=now,
      )
    except Exception as exc:
      return RegistrationObservation(
        domain=registrable_domain,
        lookup_status="lookup_error",
        authoritative_source=base_urls[0],
        limitation_note=f"RDAP query failed: {exc}",
        checked_at_utc=now,
      )


def parse_rdap_domain_payload(
  domain: str,
  source: str,
  payload: dict[str, Any],
  checked_at_utc: str,
) -> RegistrationObservation:
  epp_statuses = [str(s) for s in (payload.get("status") or [])]
  norm = {s.lower().replace("_", " ").strip() for s in epp_statuses}

  created_at: str | None = None
  expires_at: str | None = None
  updated_at: str | None = None
  for ev in payload.get("events", []) or []:
    action = str(ev.get("eventAction", "")).lower()
    date_val = ev.get("eventDate")
    if action == "registration":
      created_at = date_val
    elif action == "expiration":
      expires_at = date_val
    elif action in ("last changed", "last update of rdap database"):
      updated_at = date_val

  registrar: str | None = None
  for ent in payload.get("entities", []) or []:
    roles = [str(r).lower() for r in (ent.get("roles") or [])]
    if "registrar" in roles:
      vcard = ent.get("vcardArray")
      if isinstance(vcard, list) and len(vcard) >= 2 and isinstance(vcard[1], list):
        for prop in vcard[1]:
          if isinstance(prop, list) and len(prop) >= 4 and prop[0] == "fn":
            registrar = str(prop[3])
            break
      if not registrar and ent.get("handle"):
        registrar = str(ent.get("handle"))

  if any("pending delete" in s or "pendingdelete" in s for s in norm):
    lookup_status = "pending_delete"
  elif any("redemption period" in s or "redemptionperiod" in s for s in norm):
    lookup_status = "redemption_period"
  elif any("client hold" in s or "server hold" in s or "clienthold" in s or "serverhold" in s for s in norm):
    lookup_status = "hold"
  else:
    lookup_status = "registered"
    if expires_at:
      try:
        exp_dt = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
        if exp_dt < datetime.now(timezone.utc):
          lookup_status = "expired_grace"
      except ValueError:
        pass

  return RegistrationObservation(
    domain=domain,
    lookup_status=lookup_status,
    authoritative_source=source,
    epp_statuses=epp_statuses,
    registrar=registrar,
    created_at=created_at,
    expires_at=expires_at,
    updated_at=updated_at,
    checked_at_utc=checked_at_utc,
  )


def probe_http_endpoint(
  raw_url: str,
  timeout_sec: float = 5.0,
  max_redirects: int = 5,
) -> HttpObservation:
  now = utc_now_iso()
  current_url = raw_url
  redirect_chain: list[str] = []
  start = time.monotonic()

  for hop in range(max_redirects + 1):
    safe, reason = validate_public_probe_target(current_url, resolve_dns=True)
    if not safe:
      return HttpObservation(
        outcome="ssrf_blocked",
        final_url=current_url if redirect_chain else None,
        redirect_chain=redirect_chain,
        error_detail=f"SSRF safety guard blocked target: {reason}",
        checked_at_utc=now,
      )

    parsed = urllib.parse.urlparse(current_url)
    host = parsed.hostname or ""
    port = parsed.port or (443 if parsed.scheme.lower() == "https" else 80)
    path = parsed.path or "/"
    if parsed.query:
      path = f"{path}?{parsed.query}"

    conn: http.client.HTTPConnection | None = None
    try:
      if parsed.scheme.lower() == "https":
        ctx = ssl.create_default_context()
        conn = http.client.HTTPSConnection(host, port=port, timeout=timeout_sec, context=ctx)
      else:
        conn = http.client.HTTPConnection(host, port=port, timeout=timeout_sec)

      conn.request("HEAD", path, headers={"User-Agent": "BDIX-Tester-Verifier/1.0", "Connection": "close"})
      resp = conn.getresponse()
      resp.read(1024)
      code = resp.status
      location = resp.getheader("Location")

      if code in (405, 501):
        conn.close()
        if parsed.scheme.lower() == "https":
          conn = http.client.HTTPSConnection(host, port=port, timeout=timeout_sec, context=ssl.create_default_context())
        else:
          conn = http.client.HTTPConnection(host, port=port, timeout=timeout_sec)
        conn.request(
          "GET",
          path,
          headers={"User-Agent": "BDIX-Tester-Verifier/1.0", "Range": "bytes=0-1023", "Connection": "close"},
        )
        resp = conn.getresponse()
        resp.read(1024)
        code = resp.status
        location = resp.getheader("Location")

      elapsed_ms = int((time.monotonic() - start) * 1000)

      if 300 <= code <= 399 and location:
        next_url = urllib.parse.urljoin(current_url, location)
        next_parsed = urllib.parse.urlparse(next_url)
        if parsed.scheme.lower() == "https" and next_parsed.scheme.lower() == "http":
          return HttpObservation(
            outcome="ssrf_blocked",
            status_code=code,
            final_url=next_url,
            redirect_chain=redirect_chain,
            response_time_ms=elapsed_ms,
            error_detail="Blocked HTTPS-to-HTTP downgrade redirect.",
            checked_at_utc=now,
          )
        redirect_chain.append(next_url)
        if hop >= max_redirects:
          return HttpObservation(
            outcome="redirected",
            status_code=code,
            final_url=next_url,
            redirect_chain=redirect_chain,
            response_time_ms=elapsed_ms,
            error_detail="Exceeded maximum redirect hops.",
            checked_at_utc=now,
          )
        current_url = next_url
        continue

      return HttpObservation(
        outcome=_classify_http_code(code, redirected=bool(redirect_chain)),
        status_code=code,
        final_url=current_url if redirect_chain else None,
        redirect_chain=redirect_chain,
        response_time_ms=elapsed_ms,
        checked_at_utc=now,
      )
    except ssl.SSLError as exc:
      return HttpObservation(
        outcome="tls_error",
        final_url=current_url if redirect_chain else None,
        redirect_chain=redirect_chain,
        tls_error=str(exc)[:180],
        error_detail=str(exc)[:180],
        checked_at_utc=now,
      )
    except (TimeoutError, socket.timeout):
      return HttpObservation(
        outcome="timeout",
        final_url=current_url if redirect_chain else None,
        redirect_chain=redirect_chain,
        error_detail="HTTP probe timed out.",
        checked_at_utc=now,
      )
    except ConnectionRefusedError as exc:
      return HttpObservation(
        outcome="connection_refused",
        final_url=current_url if redirect_chain else None,
        redirect_chain=redirect_chain,
        error_detail=str(exc)[:180],
        checked_at_utc=now,
      )
    except Exception as exc:
      return HttpObservation(
        outcome="error",
        final_url=current_url if redirect_chain else None,
        redirect_chain=redirect_chain,
        error_detail=str(exc)[:180],
        checked_at_utc=now,
      )
    finally:
      if conn is not None:
        try:
          conn.close()
        except Exception:
          pass

  return HttpObservation(outcome="error", error_detail="Unexpected redirect termination", checked_at_utc=now)


def _classify_http_code(code: int, redirected: bool) -> str:
  if 200 <= code <= 299:
    return "redirected" if redirected else "reachable_2xx"
  if 300 <= code <= 399:
    return "redirected"
  if code == 401:
    return "auth_401"
  if code == 403:
    return "forbidden_403"
  if code == 404:
    return "not_found_404"
  if code == 410:
    return "gone_410"
  if code == 429:
    return "rate_limited_429"
  if 500 <= code <= 599:
    return "server_error_5xx"
  return "error"


def probe_ftp_endpoint(raw_url: str, timeout_sec: float = 5.0) -> FtpObservation:
  now = utc_now_iso()
  parsed = urllib.parse.urlparse(raw_url)
  if parsed.scheme.lower() == "ftps":
    return FtpObservation(
      outcome="unsupported_protocol",
      error_detail="FTPS is explicitly unsupported without plain FTP fallback.",
      checked_at_utc=now,
    )
  safe, reason = validate_public_probe_target(raw_url, resolve_dns=True)
  if not safe:
    return FtpObservation(
      outcome="error",
      error_detail=f"SSRF guard blocked FTP target: {reason}",
      checked_at_utc=now,
    )
  host = parsed.hostname or ""
  port = parsed.port or 21
  start = time.monotonic()
  try:
    with socket.create_connection((host, port), timeout=timeout_sec) as sock:
      sock.settimeout(timeout_sec)
      banner = sock.recv(1024).decode("ascii", errors="replace").strip()
      elapsed_ms = int((time.monotonic() - start) * 1000)
      try:
        sock.sendall(b"QUIT\r\n")
      except OSError:
        pass
    code: int | None = None
    if len(banner) >= 3 and banner[:3].isdigit():
      code = int(banner[:3])
    if code == 220:
      outcome = "reachable_220"
    elif code in (331, 530):
      outcome = "auth_required" if code == 331 else "login_rejected"
    elif code == 421:
      outcome = "server_unavailable_421"
    elif code == 550:
      outcome = "directory_unavailable_550"
    else:
      outcome = "error"
    return FtpObservation(
      outcome=outcome,
      greeting_code=code,
      banner_excerpt=banner[:160] if banner else None,
      response_time_ms=elapsed_ms,
      checked_at_utc=now,
    )
  except (TimeoutError, socket.timeout):
    return FtpObservation(outcome="timeout", error_detail="FTP greeting timed out.", checked_at_utc=now)
  except ConnectionRefusedError as exc:
    return FtpObservation(outcome="connection_refused", error_detail=str(exc)[:160], checked_at_utc=now)
  except Exception as exc:
    return FtpObservation(outcome="error", error_detail=str(exc)[:160], checked_at_utc=now)
