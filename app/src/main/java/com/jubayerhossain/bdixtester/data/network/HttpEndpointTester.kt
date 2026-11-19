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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProtocolException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class EndpointProbeConfig(
  val scanMode: ScanMode,
  val connectTimeoutMs: Int,
  val readTimeoutMs: Int,
  val overallDeadlineMs: Int,
  val networkProfileLabel: String? = null,
)

interface EndpointProbeExecutor {
  suspend fun probe(
    endpoint: ServerEndpoint,
    config: EndpointProbeConfig,
  ): ServerTestResult
}

internal class ProbeTimingTracker {
  @Volatile var dnsStartNanos: Long = 0L

  @Volatile var dnsEndNanos: Long = 0L

  @Volatile var connectStartNanos: Long = 0L

  @Volatile var connectEndNanos: Long = 0L

  @Volatile var tlsStartNanos: Long = 0L

  @Volatile var tlsEndNanos: Long = 0L

  @Volatile var callStartNanos: Long = 0L

  @Volatile var responseHeadersEndNanos: Long = 0L

  val dnsTimeMs: Long?
    get() = durationMs(dnsStartNanos, dnsEndNanos)
  val tcpConnectTimeMs: Long?
    get() = durationMs(connectStartNanos, connectEndNanos)
  val tlsHandshakeTimeMs: Long?
    get() = durationMs(tlsStartNanos, tlsEndNanos)
  val httpResponseLatencyMs: Long?
    get() = durationMs(callStartNanos, responseHeadersEndNanos)

  private fun durationMs(
    start: Long,
    end: Long,
  ): Long? {
    if (start <= 0L || end <= start) return null
    return TimeUnit.NANOSECONDS.toMillis(end - start).coerceAtLeast(1L)
  }
}

private class ProbeTimingEventListener(
  private val tracker: ProbeTimingTracker?,
) : EventListener() {
  override fun callStart(call: Call) {
    if (tracker != null && tracker.callStartNanos == 0L) {
      tracker.callStartNanos = System.nanoTime()
    }
  }

  override fun dnsStart(
    call: Call,
    domainName: String,
  ) {
    if (tracker != null && tracker.dnsStartNanos == 0L) {
      tracker.dnsStartNanos = System.nanoTime()
    }
  }

  override fun dnsEnd(
    call: Call,
    domainName: String,
    inetAddressList: List<InetAddress>,
  ) {
    if (tracker != null && tracker.dnsEndNanos == 0L) {
      tracker.dnsEndNanos = System.nanoTime()
    }
  }

  override fun connectStart(
    call: Call,
    inetSocketAddress: InetSocketAddress,
    proxy: Proxy,
  ) {
    if (tracker != null && tracker.connectStartNanos == 0L) {
      tracker.connectStartNanos = System.nanoTime()
    }
  }

  override fun secureConnectStart(call: Call) {
    if (tracker != null && tracker.tlsStartNanos == 0L) {
      tracker.tlsStartNanos = System.nanoTime()
    }
  }

  override fun secureConnectEnd(
    call: Call,
    handshake: Handshake?,
  ) {
    if (tracker != null && tracker.tlsEndNanos == 0L) {
      tracker.tlsEndNanos = System.nanoTime()
    }
  }

  override fun connectEnd(
    call: Call,
    inetSocketAddress: InetSocketAddress,
    proxy: Proxy,
    protocol: Protocol?,
  ) {
    if (tracker != null && tracker.connectEndNanos == 0L) {
      tracker.connectEndNanos = System.nanoTime()
    }
  }

  override fun responseHeadersEnd(
    call: Call,
    response: Response,
  ) {
    tracker?.responseHeadersEndNanos = System.nanoTime()
  }
}

class HttpsDowngradeException(
  message: String,
) : IOException(message)

class UnsupportedRedirectSchemeException(
  message: String,
) : IOException(message)

class TooManyRedirectsException(
  message: String,
) : IOException(message)

class SsrfRedirectBlockedException(
  message: String,
) : IOException(message)

class HttpEndpointTester(
  baseClient: OkHttpClient = OkHttpClient(),
  private val clock: () -> Long = { System.currentTimeMillis() },
) : EndpointProbeExecutor {
  private val client: OkHttpClient =
    baseClient
      .newBuilder()
      .followRedirects(false)
      .followSslRedirects(false)
      .retryOnConnectionFailure(false)
      .eventListenerFactory { call ->
        ProbeTimingEventListener(call.request().tag(ProbeTimingTracker::class.java))
      }.build()

  override suspend fun probe(
    endpoint: ServerEndpoint,
    config: EndpointProbeConfig,
  ): ServerTestResult {
    val now = clock()
    if (endpoint.protocol != ServerProtocol.HTTP && endpoint.protocol != ServerProtocol.HTTPS) {
      return ServerTestResult(
        serverId = endpoint.id,
        serverNameSnapshot = endpoint.name,
        urlSnapshot = endpoint.originalUrl,
        testedAtEpochMs = now,
        scanMode = config.scanMode,
        protocol = endpoint.protocol,
        status = TestStatus.UNSUPPORTED,
        errorClassification = ErrorClassification.UNSUPPORTED_SCHEME,
        failureMessage = "HTTP tester does not handle ${endpoint.protocol.displayLabel}.",
        networkProfileLabel = config.networkProfileLabel,
      )
    }

    val initialUrl = endpoint.originalUrl.toHttpUrlOrNull()
    if (initialUrl == null) {
      return ServerTestResult(
        serverId = endpoint.id,
        serverNameSnapshot = endpoint.name,
        urlSnapshot = endpoint.originalUrl,
        testedAtEpochMs = now,
        scanMode = config.scanMode,
        protocol = endpoint.protocol,
        status = TestStatus.UNSUPPORTED,
        errorClassification = ErrorClassification.INVALID_URL,
        failureMessage = "Endpoint URL is invalid or could not be parsed.",
        networkProfileLabel = config.networkProfileLabel,
      )
    }

    return withContext(Dispatchers.IO) {
      val tracker = ProbeTimingTracker()
      val probeClient =
        client
          .newBuilder()
          .connectTimeout(config.connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
          .readTimeout(config.readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
          .callTimeout(config.overallDeadlineMs.toLong(), TimeUnit.MILLISECONDS)
          .build()

      val outcome =
        withTimeoutOrNull(config.overallDeadlineMs.toLong()) {
          executeHeadThenBoundedGetWithRedirects(
            probeClient = probeClient,
            endpoint = endpoint,
            config = config,
            tracker = tracker,
          )
        }

      outcome ?: ServerTestResult(
        serverId = endpoint.id,
        serverNameSnapshot = endpoint.name,
        urlSnapshot = endpoint.originalUrl,
        testedAtEpochMs = clock(),
        scanMode = config.scanMode,
        protocol = endpoint.protocol,
        status = TestStatus.TIMED_OUT,
        dnsTimeMs = tracker.dnsTimeMs,
        tcpConnectTimeMs = tracker.tcpConnectTimeMs,
        tlsHandshakeTimeMs = tracker.tlsHandshakeTimeMs,
        errorClassification = ErrorClassification.TIMEOUT,
        failureMessage =
          if (config.scanMode == ScanMode.QUICK_SCAN) {
            "Quick Scan time budget (${config.overallDeadlineMs} ms) exceeded; result is inconclusive."
          } else {
            "Overall probe deadline (${config.overallDeadlineMs} ms) exceeded."
          },
        networkProfileLabel = config.networkProfileLabel,
        isQuickScanInconclusive = config.scanMode == ScanMode.QUICK_SCAN,
      )
    }
  }

  private suspend fun executeHeadThenBoundedGetWithRedirects(
    probeClient: OkHttpClient,
    endpoint: ServerEndpoint,
    config: EndpointProbeConfig,
    tracker: ProbeTimingTracker,
  ): ServerTestResult {
    var currentUrl = endpoint.originalUrl
    var hops = 0
    var lastRedirectCode: Int? = null

    try {
      while (true) {
        val headReq =
          Request
            .Builder()
            .url(currentUrl)
            .head()
            .header("User-Agent", "BDIX-Tester/1.0")
            .header("Connection", "close")
            .tag(ProbeTimingTracker::class.java, tracker)
            .build()

        val outcome =
          try {
            executeAndHandleResponse(
              probeClient = probeClient,
              request = headReq,
              isRangeFallback = false,
            )
          } catch (_: ProtocolException) {
            // Some lightweight embedded servers reject HEAD framing; fall back to bounded GET
            null
          }

        val effective =
          if (outcome == null ||
            outcome.code == 405 ||
            outcome.code == 501
          ) {
            val getReq =
              Request
                .Builder()
                .url(currentUrl)
                .get()
                .header("Range", "bytes=0-1023")
                .header("Accept-Encoding", "identity")
                .header("User-Agent", "BDIX-Tester/1.0")
                .header("Connection", "close")
                .tag(ProbeTimingTracker::class.java, tracker)
                .build()
            executeAndHandleResponse(
              probeClient = probeClient,
              request = getReq,
              isRangeFallback = true,
            )
          } else {
            outcome
          }

        if (effective.code in 300..399 && effective.locationHeader != null) {
          lastRedirectCode = effective.code
          val nextUrl =
            resolveAndValidateRedirect(
              fromUrl = currentUrl,
              locationHeader = effective.locationHeader,
            )
          hops++
          if (hops > MAX_REDIRECT_HOPS) {
            throw TooManyRedirectsException("Exceeded $MAX_REDIRECT_HOPS HTTP redirects.")
          }
          currentUrl = nextUrl
          continue
        }

        val classification =
          classifyHttpStatus(
            statusCode = effective.code,
            redirected = hops > 0,
          )
        val finalUrlRecorded = if (currentUrl != endpoint.originalUrl) currentUrl else null
        return ServerTestResult(
          serverId = endpoint.id,
          serverNameSnapshot = endpoint.name,
          urlSnapshot = endpoint.originalUrl,
          testedAtEpochMs = clock(),
          scanMode = config.scanMode,
          protocol = endpoint.protocol,
          status = classification.first,
          dnsTimeMs = tracker.dnsTimeMs,
          tcpConnectTimeMs = tracker.tcpConnectTimeMs,
          tlsHandshakeTimeMs = tracker.tlsHandshakeTimeMs,
          httpResponseLatencyMs = tracker.httpResponseLatencyMs,
          httpStatusCode = effective.code,
          finalUrl = finalUrlRecorded,
          errorClassification = classification.second,
          failureMessage =
            if (hops > 0 && lastRedirectCode != null) {
              "Redirected ($lastRedirectCode -> ${effective.code})"
            } else {
              null
            },
          networkProfileLabel = config.networkProfileLabel,
        )
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      return mapFailureResult(
        endpoint = endpoint,
        config = config,
        tracker = tracker,
        finalUrl = if (currentUrl != endpoint.originalUrl) currentUrl else null,
        exception = e,
      )
    }
  }

  private data class HttpCallOutcome(
    val code: Int,
    val locationHeader: String?,
  )

  private suspend fun executeAndHandleResponse(
    probeClient: OkHttpClient,
    request: Request,
    isRangeFallback: Boolean,
  ): HttpCallOutcome {
    val response = probeClient.newCall(request).await()
    return response.use { res ->
      if (isRangeFallback) {
        val body = res.body
        // Hard truncation safeguard: consume at most 1024 bytes even if server ignores Range
        val source = body.source()
        val buffer = ByteArray(1024)
        var remaining = MAX_RANGE_PROBE_BYTES
        while (remaining > 0) {
          val read = source.read(buffer, 0, minOf(buffer.size, remaining))
          if (read == -1) break
          remaining -= read
        }
      }
      HttpCallOutcome(
        code = res.code,
        locationHeader = res.header("Location"),
      )
    }
  }

  private fun resolveAndValidateRedirect(
    fromUrl: String,
    locationHeader: String,
  ): String {
    val base =
      fromUrl.toHttpUrlOrNull()
        ?: throw UnsupportedRedirectSchemeException("Invalid origin URL.")
    val resolved =
      base.resolve(locationHeader)
        ?: throw UnsupportedRedirectSchemeException("Unsupported or invalid redirect target.")
    if (base.scheme.equals("https", ignoreCase = true) &&
      resolved.scheme.equals("http", ignoreCase = true)
    ) {
      throw HttpsDowngradeException(
        "Blocked HTTPS-to-HTTP downgrade redirect from ${base.host} to ${resolved.host}.",
      )
    }
    val ssrfResult = SsrfGuard.validateRedirectTarget(base, resolved)
    if (ssrfResult is SsrfCheckResult.Blocked) {
      throw SsrfRedirectBlockedException(ssrfResult.reason)
    }
    return resolved.toString()
  }

  private fun classifyHttpStatus(
    statusCode: Int,
    redirected: Boolean,
  ): Pair<TestStatus, ErrorClassification> =
    when (statusCode) {
      in 200..299 -> {
        TestStatus.REACHABLE to if (redirected) ErrorClassification.HTTP_REDIRECT else ErrorClassification.NONE
      }
      in 300..399 -> TestStatus.REACHABLE to ErrorClassification.HTTP_REDIRECT
      401 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_AUTH_REQUIRED
      403 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_FORBIDDEN
      404 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_NOT_FOUND
      410 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_GONE
      429 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_RATE_LIMITED
      in 400..499 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_CLIENT_ERROR
      in 500..599 -> TestStatus.RESTRICTED to ErrorClassification.HTTP_SERVER_ERROR
      else -> TestStatus.RESTRICTED to ErrorClassification.HTTP_CLIENT_ERROR
    }

  private fun mapFailureResult(
    endpoint: ServerEndpoint,
    config: EndpointProbeConfig,
    tracker: ProbeTimingTracker,
    finalUrl: String?,
    exception: Exception,
  ): ServerTestResult {
    val (status, errorClass, message) =
      when (exception) {
        is HttpsDowngradeException ->
          Triple(
            TestStatus.UNREACHABLE,
            ErrorClassification.PROTOCOL_DOWNGRADE_BLOCKED,
            exception.message ?: "HTTPS downgrade redirect blocked.",
          )
        is SsrfRedirectBlockedException ->
          Triple(
            TestStatus.UNREACHABLE,
            ErrorClassification.SSRF_REDIRECT_BLOCKED,
            exception.message ?: "Unsafe redirect to non-public target blocked.",
          )
        is UnsupportedRedirectSchemeException ->
          Triple(
            TestStatus.UNSUPPORTED,
            ErrorClassification.UNSUPPORTED_SCHEME,
            exception.message ?: "Redirect to unsupported scheme blocked.",
          )
        is TooManyRedirectsException ->
          Triple(
            TestStatus.RESTRICTED,
            ErrorClassification.HTTP_REDIRECT,
            exception.message ?: "Too many redirects.",
          )
        is UnknownHostException ->
          Triple(
            TestStatus.UNREACHABLE,
            ErrorClassification.DNS_FAILURE,
            "DNS resolution failed for ${endpoint.host}.",
          )
        is SSLException ->
          Triple(
            TestStatus.UNREACHABLE,
            ErrorClassification.TLS_FAILURE,
            exception.message ?: "TLS certificate or handshake verification failed.",
          )
        is ConnectException ->
          Triple(
            TestStatus.UNREACHABLE,
            ErrorClassification.CONNECTION_REFUSED,
            exception.message ?: "Connection refused.",
          )
        is SocketTimeoutException ->
          Triple(
            TestStatus.TIMED_OUT,
            ErrorClassification.TIMEOUT,
            if (config.scanMode == ScanMode.QUICK_SCAN) {
              "Quick Scan timeout exceeded; host may require a longer Full Scan timeout."
            } else {
              exception.message ?: "Connection or response timed out."
            },
          )
        is InterruptedIOException -> {
          if (exception.message?.contains("timeout", ignoreCase = true) == true) {
            Triple(
              TestStatus.TIMED_OUT,
              ErrorClassification.TIMEOUT,
              exception.message ?: "Request deadline timed out.",
            )
          } else {
            Triple(
              TestStatus.UNREACHABLE,
              ErrorClassification.UNKNOWN_FAILURE,
              exception.message ?: "Network I/O interrupted.",
            )
          }
        }
        else ->
          Triple(
            TestStatus.UNREACHABLE,
            ErrorClassification.UNKNOWN_FAILURE,
            exception.message ?: "Connection failed.",
          )
      }
    return ServerTestResult(
      serverId = endpoint.id,
      serverNameSnapshot = endpoint.name,
      urlSnapshot = endpoint.originalUrl,
      testedAtEpochMs = clock(),
      scanMode = config.scanMode,
      protocol = endpoint.protocol,
      status = status,
      dnsTimeMs = tracker.dnsTimeMs,
      tcpConnectTimeMs = tracker.tcpConnectTimeMs,
      tlsHandshakeTimeMs = tracker.tlsHandshakeTimeMs,
      finalUrl = finalUrl,
      errorClassification = errorClass,
      failureMessage = message,
      networkProfileLabel = config.networkProfileLabel,
      isQuickScanInconclusive = config.scanMode == ScanMode.QUICK_SCAN && status == TestStatus.TIMED_OUT,
    )
  }

  private suspend fun Call.await(): Response =
    suspendCancellableCoroutine { cont ->
      cont.invokeOnCancellation { cancel() }
      enqueue(
        object : Callback {
          override fun onFailure(
            call: Call,
            e: IOException,
          ) {
            if (cont.isActive) {
              cont.resumeWithException(e)
            }
          }

          override fun onResponse(
            call: Call,
            response: Response,
          ) {
            if (cont.isActive) {
              cont.resume(response)
            } else {
              response.close()
            }
          }
        },
      )
    }

  companion object {
    private const val MAX_REDIRECT_HOPS = 5
    private const val MAX_RANGE_PROBE_BYTES = 1024
  }
}
