from __future__ import annotations

import ipaddress
import re
import socket
from urllib.parse import urlparse

SUPPORTED_SCHEMES = {"http", "https", "ftp", "ftps"}

# Common second-level public suffixes relevant to Bangladesh & global OTT/media hosts
MULTI_PART_PUBLIC_SUFFIXES = {
  "com.bd",
  "net.bd",
  "org.bd",
  "edu.bd",
  "gov.bd",
  "ac.bd",
  "mil.bd",
  "co.uk",
  "org.uk",
  "co.in",
  "com.au",
  "com.sg",
}

METADATA_HOSTS = {"metadata.google.internal", "169.254.169.254", "100.100.100.200"}


def parse_ip(host: str) -> ipaddress.IPv4Address | ipaddress.IPv6Address | None:
  cleaned = host.strip().strip("[]")
  try:
    return ipaddress.ip_address(cleaned)
  except ValueError:
    return None


def is_rfc1918_private_ip(host: str) -> bool:
  ip = parse_ip(host)
  if ip is None or ip.version != 4:
    return False
  octets = ip.packed
  first, second = octets[0], octets[1]
  if first == 10:
    return True
  if first == 172 and 16 <= second <= 31:
    return True
  if first == 192 and second == 168:
    return True
  return False


def is_blocked_public_probe_ip(ip_str: str) -> tuple[bool, str | None]:
  ip = parse_ip(ip_str)
  if ip is None:
    return True, "unparseable_ip"
  if ip.is_loopback:
    return True, "loopback_address"
  if ip.is_link_local:
    return True, "link_local_or_metadata_address"
  if ip.is_private:
    return True, "private_network_address"
  if ip.is_multicast:
    return True, "multicast_address"
  if ip.is_unspecified:
    return True, "unspecified_address"
  if ip.is_reserved:
    return True, "reserved_address"
  # CGNAT 100.64.0.0/10
  if ip.version == 4 and ip in ipaddress.IPv4Network("100.64.0.0/10"):
    return True, "cgnat_shared_address"
  return False, None


def validate_public_probe_target(raw_url: str, resolve_dns: bool = False) -> tuple[bool, str | None]:
  parsed = urlparse(raw_url.strip())
  scheme = (parsed.scheme or "").lower()
  if scheme not in {"http", "https", "ftp"}:
    return False, f"unsupported_scheme:{scheme or 'none'}"
  host = (parsed.hostname or "").strip().lower()
  if not host:
    return False, "missing_host"
  if host in {"localhost", "localhost.localdomain"} or host in METADATA_HOSTS:
    return False, "blocked_internal_or_metadata_host"

  ip = parse_ip(host)
  if ip is not None:
    blocked, reason = is_blocked_public_probe_ip(str(ip))
    return (not blocked), reason

  if resolve_dns:
    try:
      infos = socket.getaddrinfo(host, parsed.port or (443 if scheme == "https" else 80), type=socket.SOCK_STREAM)
      for info in infos:
        resolved_ip = info[4][0]
        blocked, reason = is_blocked_public_probe_ip(resolved_ip)
        if blocked:
          return False, f"dns_resolved_to_{reason}:{resolved_ip}"
    except OSError:
      # DNS lookup failure handled separately by DNS verifier
      pass

  return True, None


def canonical_key(raw_url: str) -> str:
  trimmed = raw_url.strip()
  if not trimmed:
    return ""
  try:
    parsed = urlparse(trimmed)
  except ValueError:
    return trimmed.lower()
  if not parsed.scheme or not parsed.hostname:
    return trimmed.lower()
  scheme = parsed.scheme.lower()
  host = parsed.hostname.lower()
  if host.startswith("www."):
    host = host[4:]
  default_ports = {"http": 80, "https": 443, "ftp": 21, "ftps": 990}
  port = parsed.port if parsed.port and parsed.port != default_ports.get(scheme) else None
  normalized_path = re.sub(r"/+", "/", parsed.path or "").rstrip("/")
  query_part = f"?{parsed.query}" if parsed.query else ""
  port_part = f":{port}" if port else ""
  return f"{scheme}://{host}{port_part}{normalized_path}{query_part}"


def extract_registrable_domain(host: str) -> str | None:
  cleaned = host.strip().lower().rstrip(".")
  if not cleaned or parse_ip(cleaned) is not None or "." not in cleaned:
    return None
  parts = [p for p in cleaned.split(".") if p]
  if len(parts) < 2:
    return None
  last_two = ".".join(parts[-2:])
  if last_two in MULTI_PART_PUBLIC_SUFFIXES and len(parts) >= 3:
    return ".".join(parts[-3:])
  return last_two


def extract_tld(domain: str) -> str | None:
  cleaned = domain.strip().lower().rstrip(".")
  if "." not in cleaned:
    return None
  return cleaned.split(".")[-1]
