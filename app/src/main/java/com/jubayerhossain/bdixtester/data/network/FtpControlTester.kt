package com.jubayerhossain.bdixtester.data.network

import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Unauthenticated RFC 959 FTP control-connection probe (`ftp://`).
 *
 * Strictly establishes TCP connection, reads the server greeting (`220` or `120`->`220`),
 * sends `QUIT`, reads optional `221`, and closes the socket. Never performs directory listing,
 * file transfer, or credential submission. Explicitly reports `ftps://` as unsupported without
 * downgrading to plain FTP.
 */
class FtpControlTester(
  private val clock: () -> Long = { System.currentTimeMillis() },
) : EndpointProbeExecutor {
  override suspend fun probe(
    endpoint: ServerEndpoint,
    config: EndpointProbeConfig,
  ): ServerTestResult {
    if (endpoint.protocol == ServerProtocol.FTPS) {
      return ServerTestResult(
        serverId = endpoint.id,
        serverNameSnapshot = endpoint.name,
        urlSnapshot = endpoint.originalUrl,
        testedAtEpochMs = clock(),
        scanMode = config.scanMode,
        protocol = ServerProtocol.FTPS,
        status = TestStatus.UNSUPPORTED,
        errorClassification = ErrorClassification.UNSUPPORTED_SCHEME,
        failureMessage = "FTPS (RFC 4217) is not supported in this version; plain FTP fallback is never performed.",
        networkProfileLabel = config.networkProfileLabel,
      )
    }
    if (endpoint.protocol != ServerProtocol.FTP) {
      return ServerTestResult(
        serverId = endpoint.id,
        serverNameSnapshot = endpoint.name,
        urlSnapshot = endpoint.originalUrl,
        testedAtEpochMs = clock(),
        scanMode = config.scanMode,
        protocol = endpoint.protocol,
        status = TestStatus.UNSUPPORTED,
        errorClassification = ErrorClassification.UNSUPPORTED_SCHEME,
        failureMessage = "Unsupported scheme for FTP probe.",
        networkProfileLabel = config.networkProfileLabel,
      )
    }

    val port = endpoint.explicitPort ?: DEFAULT_FTP_PORT
    return withContext(Dispatchers.IO) {
      val result =
        withTimeoutOrNull(config.overallDeadlineMs.toLong()) {
          runCancellableFtpProbe(endpoint, port, config)
        }
      result ?: ServerTestResult(
        serverId = endpoint.id,
        serverNameSnapshot = endpoint.name,
        urlSnapshot = endpoint.originalUrl,
        testedAtEpochMs = clock(),
        scanMode = config.scanMode,
        protocol = ServerProtocol.FTP,
        status = TestStatus.TIMED_OUT,
        errorClassification = ErrorClassification.TIMEOUT,
        failureMessage = "FTP overall deadline (${config.overallDeadlineMs} ms) exceeded.",
        networkProfileLabel = config.networkProfileLabel,
        isQuickScanInconclusive = config.scanMode == ScanMode.QUICK_SCAN,
      )
    }
  }

  private suspend fun runCancellableFtpProbe(
    endpoint: ServerEndpoint,
    port: Int,
    config: EndpointProbeConfig,
  ): ServerTestResult =
    suspendCancellableCoroutine { cont ->
      val socket = Socket()
      cont.invokeOnCancellation {
        try {
          socket.close()
        } catch (_: IOException) {
        }
      }

      var dnsMs: Long? = null
      var tcpMs: Long? = null
      var greetingMs: Long? = null

      val outcome =
        try {
          val dnsStart = System.nanoTime()
          val address = InetAddress.getByName(endpoint.host)
          dnsMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - dnsStart).coerceAtLeast(1L)

          val tcpStart = System.nanoTime()
          socket.soTimeout = config.readTimeoutMs
          socket.connect(InetSocketAddress(address, port), config.connectTimeoutMs)
          tcpMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tcpStart).coerceAtLeast(1L)

          val greetingStart = System.nanoTime()
          val reply = readFtpReplySequence(BufferedInputStream(socket.getInputStream()))
          greetingMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - greetingStart).coerceAtLeast(1L)

          when (reply) {
            is FtpReplyOutcome.ValidGreeting -> {
              sendQuitBestEffort(socket)
              when (reply.code) {
                220 ->
                  ServerTestResult(
                    serverId = endpoint.id,
                    serverNameSnapshot = endpoint.name,
                    urlSnapshot = endpoint.originalUrl,
                    testedAtEpochMs = clock(),
                    scanMode = config.scanMode,
                    protocol = ServerProtocol.FTP,
                    status = TestStatus.REACHABLE,
                    dnsTimeMs = dnsMs,
                    tcpConnectTimeMs = tcpMs,
                    ftpConnectLatencyMs = greetingMs,
                    ftpGreetingCode = 220,
                    errorClassification = ErrorClassification.NONE,
                    failureMessage = "FTP service reachable (RFC 959 220 greeting)",
                    networkProfileLabel = config.networkProfileLabel,
                  )
                421 ->
                  ServerTestResult(
                    serverId = endpoint.id,
                    serverNameSnapshot = endpoint.name,
                    urlSnapshot = endpoint.originalUrl,
                    testedAtEpochMs = clock(),
                    scanMode = config.scanMode,
                    protocol = ServerProtocol.FTP,
                    status = TestStatus.RESTRICTED,
                    dnsTimeMs = dnsMs,
                    tcpConnectTimeMs = tcpMs,
                    ftpConnectLatencyMs = greetingMs,
                    ftpGreetingCode = 421,
                    errorClassification = ErrorClassification.FTP_PROTOCOL_ERROR,
                    failureMessage = "FTP service temporarily unavailable (421).",
                    networkProfileLabel = config.networkProfileLabel,
                  )
                else ->
                  ServerTestResult(
                    serverId = endpoint.id,
                    serverNameSnapshot = endpoint.name,
                    urlSnapshot = endpoint.originalUrl,
                    testedAtEpochMs = clock(),
                    scanMode = config.scanMode,
                    protocol = ServerProtocol.FTP,
                    status = TestStatus.RESTRICTED,
                    dnsTimeMs = dnsMs,
                    tcpConnectTimeMs = tcpMs,
                    ftpConnectLatencyMs = greetingMs,
                    ftpGreetingCode = reply.code,
                    errorClassification = ErrorClassification.FTP_PROTOCOL_ERROR,
                    failureMessage = "Unexpected FTP greeting code ${reply.code}.",
                    networkProfileLabel = config.networkProfileLabel,
                  )
              }
            }
            is FtpReplyOutcome.ProtocolError ->
              ServerTestResult(
                serverId = endpoint.id,
                serverNameSnapshot = endpoint.name,
                urlSnapshot = endpoint.originalUrl,
                testedAtEpochMs = clock(),
                scanMode = config.scanMode,
                protocol = ServerProtocol.FTP,
                status = TestStatus.UNREACHABLE,
                dnsTimeMs = dnsMs,
                tcpConnectTimeMs = tcpMs,
                errorClassification = ErrorClassification.FTP_PROTOCOL_ERROR,
                failureMessage = reply.reason,
                networkProfileLabel = config.networkProfileLabel,
              )
          }
        } catch (ce: CancellationException) {
          throw ce
        } catch (e: UnknownHostException) {
          ServerTestResult(
            serverId = endpoint.id,
            serverNameSnapshot = endpoint.name,
            urlSnapshot = endpoint.originalUrl,
            testedAtEpochMs = clock(),
            scanMode = config.scanMode,
            protocol = ServerProtocol.FTP,
            status = TestStatus.UNREACHABLE,
            errorClassification = ErrorClassification.DNS_FAILURE,
            failureMessage = "DNS resolution failed for ${endpoint.host}.",
            networkProfileLabel = config.networkProfileLabel,
          )
        } catch (e: ConnectException) {
          ServerTestResult(
            serverId = endpoint.id,
            serverNameSnapshot = endpoint.name,
            urlSnapshot = endpoint.originalUrl,
            testedAtEpochMs = clock(),
            scanMode = config.scanMode,
            protocol = ServerProtocol.FTP,
            status = TestStatus.UNREACHABLE,
            dnsTimeMs = dnsMs,
            errorClassification = ErrorClassification.CONNECTION_REFUSED,
            failureMessage = e.message ?: "FTP port $port refused connection.",
            networkProfileLabel = config.networkProfileLabel,
          )
        } catch (e: SocketTimeoutException) {
          ServerTestResult(
            serverId = endpoint.id,
            serverNameSnapshot = endpoint.name,
            urlSnapshot = endpoint.originalUrl,
            testedAtEpochMs = clock(),
            scanMode = config.scanMode,
            protocol = ServerProtocol.FTP,
            status = TestStatus.TIMED_OUT,
            dnsTimeMs = dnsMs,
            tcpConnectTimeMs = tcpMs,
            errorClassification = ErrorClassification.TIMEOUT,
            failureMessage = "FTP greeting or connection timed out.",
            networkProfileLabel = config.networkProfileLabel,
            isQuickScanInconclusive = config.scanMode == ScanMode.QUICK_SCAN,
          )
        } catch (e: IOException) {
          ServerTestResult(
            serverId = endpoint.id,
            serverNameSnapshot = endpoint.name,
            urlSnapshot = endpoint.originalUrl,
            testedAtEpochMs = clock(),
            scanMode = config.scanMode,
            protocol = ServerProtocol.FTP,
            status = TestStatus.UNREACHABLE,
            dnsTimeMs = dnsMs,
            tcpConnectTimeMs = tcpMs,
            errorClassification = ErrorClassification.FTP_PROTOCOL_ERROR,
            failureMessage = e.message ?: "FTP socket read error.",
            networkProfileLabel = config.networkProfileLabel,
          )
        } finally {
          try {
            socket.close()
          } catch (_: IOException) {
          }
        }

      if (cont.isActive) {
        cont.resume(outcome)
      }
    }

  private sealed class FtpReplyOutcome {
    data class ValidGreeting(
      val code: Int,
    ) : FtpReplyOutcome()

    data class ProtocolError(
      val reason: String,
    ) : FtpReplyOutcome()
  }

  private fun readFtpReplySequence(input: BufferedInputStream): FtpReplyOutcome {
    var totalBytesRead = 0
    var totalLines = 0
    var activeMultilineCode: Int? = null

    while (totalLines < MAX_GREETING_LINES) {
      val line =
        readBoundedLine(input, MAX_GREETING_BYTES - totalBytesRead)
          ?: return FtpReplyOutcome.ProtocolError("FTP greeting truncated or exceeded byte limit.")
      totalBytesRead += line.length + 2
      totalLines++

      if (line.length < 3 || !line[0].isDigit() || !line[1].isDigit() || !line[2].isDigit()) {
        if (activeMultilineCode != null) {
          // RFC 959 allows intermediate multiline text without repeating digits
          continue
        }
        return FtpReplyOutcome.ProtocolError("Malformed FTP reply line.")
      }

      val code = line.substring(0, 3).toInt()
      val separator = line.getOrNull(3) ?: ' '

      if (activeMultilineCode == null) {
        when (separator) {
          '-' -> {
            activeMultilineCode = code
            continue
          }
          ' ' -> {
            if (code == 120) {
              // 120 Service ready in nnn minutes -> wait for subsequent 220
              continue
            }
            return FtpReplyOutcome.ValidGreeting(code)
          }
          else -> return FtpReplyOutcome.ProtocolError("Malformed FTP reply delimiter.")
        }
      } else {
        if (code == activeMultilineCode && separator == ' ') {
          activeMultilineCode = null
          if (code == 120) continue
          return FtpReplyOutcome.ValidGreeting(code)
        }
      }
    }
    return FtpReplyOutcome.ProtocolError("FTP greeting exceeded maximum allowed lines ($MAX_GREETING_LINES).")
  }

  private fun readBoundedLine(
    input: BufferedInputStream,
    maxBytes: Int,
  ): String? {
    if (maxBytes <= 0) return null
    val sb = StringBuilder()
    var readCount = 0
    while (readCount < maxBytes) {
      val b = input.read()
      if (b == -1) {
        return if (sb.isEmpty()) null else sb.toString()
      }
      readCount++
      if (b == '\n'.code) {
        if (sb.isNotEmpty() && sb[sb.length - 1] == '\r') {
          sb.setLength(sb.length - 1)
        }
        return sb.toString()
      }
      if (b < 0x20 && b != '\r'.code && b != '\t'.code) {
        return null
      }
      sb.append(b.toChar())
    }
    return null
  }

  private fun sendQuitBestEffort(socket: Socket) {
    try {
      socket.soTimeout = QUIT_WAIT_TIMEOUT_MS
      val out = socket.getOutputStream()
      out.write("QUIT\r\n".toByteArray(Charsets.US_ASCII))
      out.flush()
      readBoundedLine(BufferedInputStream(socket.getInputStream()), 512)
    } catch (_: IOException) {
      // Best-effort clean shutdown
    }
  }

  companion object {
    private const val DEFAULT_FTP_PORT = 21
    private const val MAX_GREETING_BYTES = 8 * 1024
    private const val MAX_GREETING_LINES = 32
    private const val QUIT_WAIT_TIMEOUT_MS = 800
  }
}
