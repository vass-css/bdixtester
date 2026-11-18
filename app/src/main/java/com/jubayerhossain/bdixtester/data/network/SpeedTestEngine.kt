package com.jubayerhossain.bdixtester.data.network

import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.SpeedTerminationReason
import com.jubayerhossain.bdixtester.core.model.SpeedTestOutcome
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min

data class SpeedTestProgress(
  val transferredBytes: Long = 0L,
  val maxBytes: Long = 0L,
  val elapsedMs: Long = 0L,
  val liveMbps: Double? = null,
)

sealed interface SpeedTestExecutionOutcome {
  data class CrossHostRedirectRequiresConfirmation(
    val originalUrl: String,
    val targetUrl: String,
    val targetHost: String,
  ) : SpeedTestExecutionOutcome

  data class Completed(
    val result: SpeedTestResultSummary,
  ) : SpeedTestExecutionOutcome
}

data class SpeedTestResultSummary(
  val outcome: SpeedTestOutcome,
  val terminationReason: SpeedTerminationReason,
  val transferredBytes: Long,
  val elapsedNanos: Long,
  val downloadMbps: Double?,
  val downloadMibPerSec: Double?,
  val httpStatusCode: Int?,
  val finalUrl: String,
  val message: String,
)

/**
 * Dedicated, explicit opt-in HTTP/HTTPS download throughput tester.
 *
 * Guarantees:
 * - Never executes automatically during Quick Scan or Full Scan.
 * - Uses `Accept-Encoding: identity` and rejects unexpected `Content-Encoding`.
 * - Tracks duration via monotonic `System.nanoTime()`.
 * - Uses fixed 32 KB streaming buffer and stops immediately upon reaching `maxBytes`
 *   without reading an extra byte to probe for EOF.
 * - Rejects HTTPS-to-HTTP downgrades and requests user confirmation on cross-host redirects.
 */
class SpeedTestEngine(
  private val baseClient: OkHttpClient = OkHttpClient(),
  private val catalogRepository: CatalogRepository? = null,
  private val clock: () -> Long = { System.currentTimeMillis() },
  private val minSampleBytes: Long = DEFAULT_MIN_SAMPLE_BYTES,
  private val minSampleNanos: Long = DEFAULT_MIN_SAMPLE_NANOS,
) {
  private val client: OkHttpClient =
    baseClient
      .newBuilder()
      .followRedirects(false)
      .followSslRedirects(false)
      .retryOnConnectionFailure(false)
      .build()

  suspend fun runSpeedTest(
    endpoint: ServerEndpoint,
    targetUrl: String,
    maxBytes: Long,
    readTimeoutMs: Int = 10_000,
    overallCallTimeoutMs: Int = 30_000,
    allowConfirmedCrossHost: Boolean = false,
    networkProfileLabel: String? = null,
    onProgress: (SpeedTestProgress) -> Unit = {},
  ): SpeedTestExecutionOutcome =
    withContext(Dispatchers.IO) {
      val initialHttpUrl = targetUrl.toHttpUrlOrNull()
      if (initialHttpUrl == null) {
        val summary =
          SpeedTestResultSummary(
            outcome = SpeedTestOutcome.TRANSFER_FAILED,
            terminationReason = SpeedTerminationReason.HTTP_ERROR,
            transferredBytes = 0L,
            elapsedNanos = 0L,
            downloadMbps = null,
            downloadMibPerSec = null,
            httpStatusCode = null,
            finalUrl = targetUrl,
            message = "Invalid speed-test URL.",
          )
        return@withContext SpeedTestExecutionOutcome.Completed(summary)
      }

      val speedClient =
        client
          .newBuilder()
          .connectTimeout(5_000L, TimeUnit.MILLISECONDS)
          .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
          .callTimeout(overallCallTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
          .build()

      var currentUrl = targetUrl
      var hops = 0
      val originHost = initialHttpUrl.host

      try {
        while (true) {
          val request =
            Request
              .Builder()
              .url(currentUrl)
              .get()
              .header("Accept-Encoding", "identity")
              .header("User-Agent", "BDIX-Tester-SpeedTest/1.0")
              .build()

          val call = speedClient.newCall(request)
          val response: okhttp3.Response =
            try {
              kotlinx.coroutines.suspendCancellableCoroutine<okhttp3.Response> { cont ->
                cont.invokeOnCancellation { call.cancel() }
                call.enqueue(
                  object : okhttp3.Callback {
                    override fun onFailure(
                      call: okhttp3.Call,
                      e: IOException,
                    ) {
                      if (cont.isActive) cont.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(
                      call: okhttp3.Call,
                      response: okhttp3.Response,
                    ) {
                      if (cont.isActive) {
                        cont.resumeWith(Result.success(response))
                      } else {
                        response.close()
                      }
                    }
                  },
                )
              }
            } catch (ce: CancellationException) {
              val cancelledSummary =
                SpeedTestResultSummary(
                  outcome = SpeedTestOutcome.TRANSFER_CANCELLED,
                  terminationReason = SpeedTerminationReason.USER_CANCELLED,
                  transferredBytes = 0L,
                  elapsedNanos = 0L,
                  downloadMbps = null,
                  downloadMibPerSec = null,
                  httpStatusCode = null,
                  finalUrl = currentUrl,
                  message = "Speed test cancelled before transfer started.",
                )
              persistSpeedResult(endpoint, cancelledSummary, networkProfileLabel)
              throw ce
            }

          var shouldFollowRedirectTo: String? = null
          var redirectCheckCrossHost: SpeedTestExecutionOutcome.CrossHostRedirectRequiresConfirmation? = null
          var immediateSummary: SpeedTestResultSummary? = null

          response.use { res ->
            if (res.code in 300..399) {
              val location =
                res.header("Location")
                  ?: run {
                    immediateSummary =
                      SpeedTestResultSummary(
                        outcome = SpeedTestOutcome.TRANSFER_FAILED,
                        terminationReason = SpeedTerminationReason.HTTP_ERROR,
                        transferredBytes = 0L,
                        elapsedNanos = 0L,
                        downloadMbps = null,
                        downloadMibPerSec = null,
                        httpStatusCode = res.code,
                        finalUrl = currentUrl,
                        message = "Redirect missing Location header.",
                      )
                    return@use
                  }
              val base = currentUrl.toHttpUrlOrNull()!!
              val next =
                base.resolve(location)
                  ?: run {
                    immediateSummary =
                      SpeedTestResultSummary(
                        outcome = SpeedTestOutcome.TRANSFER_FAILED,
                        terminationReason = SpeedTerminationReason.HTTP_ERROR,
                        transferredBytes = 0L,
                        elapsedNanos = 0L,
                        downloadMbps = null,
                        downloadMibPerSec = null,
                        httpStatusCode = res.code,
                        finalUrl = currentUrl,
                        message = "Unsupported or invalid redirect target.",
                      )
                    return@use
                  }
              if (base.scheme.equals("https", true) && next.scheme.equals("http", true)) {
                immediateSummary =
                  SpeedTestResultSummary(
                    outcome = SpeedTestOutcome.TRANSFER_FAILED,
                    terminationReason = SpeedTerminationReason.HTTPS_DOWNGRADE_BLOCKED,
                    transferredBytes = 0L,
                    elapsedNanos = 0L,
                    downloadMbps = null,
                    downloadMibPerSec = null,
                    httpStatusCode = res.code,
                    finalUrl = next.toString(),
                    message = "Blocked HTTPS-to-HTTP downgrade redirect.",
                  )
                return@use
              }
              if (!allowConfirmedCrossHost && !next.host.equals(originHost, ignoreCase = true)) {
                redirectCheckCrossHost =
                  SpeedTestExecutionOutcome.CrossHostRedirectRequiresConfirmation(
                    originalUrl = targetUrl,
                    targetUrl = next.toString(),
                    targetHost = next.host,
                  )
                return@use
              }
              hops++
              if (hops > MAX_REDIRECTS) {
                immediateSummary =
                  SpeedTestResultSummary(
                    outcome = SpeedTestOutcome.TRANSFER_FAILED,
                    terminationReason = SpeedTerminationReason.HTTP_ERROR,
                    transferredBytes = 0L,
                    elapsedNanos = 0L,
                    downloadMbps = null,
                    downloadMibPerSec = null,
                    httpStatusCode = res.code,
                    finalUrl = next.toString(),
                    message = "Exceeded maximum redirect hops ($MAX_REDIRECTS).",
                  )
                return@use
              }
              shouldFollowRedirectTo = next.toString()
              return@use
            }

            if (res.code !in 200..299) {
              immediateSummary =
                SpeedTestResultSummary(
                  outcome = SpeedTestOutcome.TRANSFER_FAILED,
                  terminationReason = SpeedTerminationReason.HTTP_ERROR,
                  transferredBytes = 0L,
                  elapsedNanos = 0L,
                  downloadMbps = null,
                  downloadMibPerSec = null,
                  httpStatusCode = res.code,
                  finalUrl = currentUrl,
                  message = "Server returned HTTP ${res.code}; cannot measure throughput.",
                )
              return@use
            }

            val contentEncoding = res.header("Content-Encoding")?.trim()
            if (!contentEncoding.isNullOrEmpty() && !contentEncoding.equals("identity", ignoreCase = true)) {
              immediateSummary =
                SpeedTestResultSummary(
                  outcome = SpeedTestOutcome.TRANSFER_FAILED,
                  terminationReason = SpeedTerminationReason.UNEXPECTED_CONTENT_ENCODING,
                  transferredBytes = 0L,
                  elapsedNanos = 0L,
                  downloadMbps = null,
                  downloadMibPerSec = null,
                  httpStatusCode = res.code,
                  finalUrl = currentUrl,
                  message = "Rejected unexpected Content-Encoding: $contentEncoding.",
                )
              return@use
            }

            val body = res.body
            val stream = body.byteStream()
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            var totalBytes = 0L
            var sawEof = false
            val startNanos = System.nanoTime()
            var lastProgressEmitNanos = startNanos

            try {
              while (totalBytes < maxBytes) {
                kotlinx.coroutines.yield()
                val toRead = min(buffer.size.toLong(), maxBytes - totalBytes).toInt()
                val read = stream.read(buffer, 0, toRead)
                if (read == -1) {
                  sawEof = true
                  break
                }
                totalBytes += read
                val nowNanos = System.nanoTime()
                if (nowNanos - lastProgressEmitNanos >= PROGRESS_THROTTLE_NANOS || totalBytes >= maxBytes) {
                  val elapsedNs = (nowNanos - startNanos).coerceAtLeast(1L)
                  onProgress(
                    SpeedTestProgress(
                      transferredBytes = totalBytes,
                      maxBytes = maxBytes,
                      elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNs),
                      liveMbps =
                        if (totalBytes >= minSampleBytes && elapsedNs >= minSampleNanos) {
                          computeMbps(totalBytes, elapsedNs)
                        } else {
                          null
                        },
                    ),
                  )
                  lastProgressEmitNanos = nowNanos
                }
              }

              val elapsedNanos = (System.nanoTime() - startNanos).coerceAtLeast(1L)
              val capReached = !sawEof && totalBytes >= maxBytes
              val sufficientSample = totalBytes >= minSampleBytes && elapsedNanos >= minSampleNanos

              immediateSummary =
                when {
                  capReached && sufficientSample ->
                    SpeedTestResultSummary(
                      outcome = SpeedTestOutcome.CAP_REACHED,
                      terminationReason = SpeedTerminationReason.BYTE_CAP_REACHED,
                      transferredBytes = totalBytes,
                      elapsedNanos = elapsedNanos,
                      downloadMbps = computeMbps(totalBytes, elapsedNanos),
                      downloadMibPerSec = computeMibPerSec(totalBytes, elapsedNanos),
                      httpStatusCode = res.code,
                      finalUrl = currentUrl,
                      message = "Transfer stopped cleanly at configured byte cap.",
                    )
                  capReached ->
                    SpeedTestResultSummary(
                      outcome = SpeedTestOutcome.INSUFFICIENT_SAMPLE,
                      terminationReason = SpeedTerminationReason.BYTE_CAP_REACHED,
                      transferredBytes = totalBytes,
                      elapsedNanos = elapsedNanos,
                      downloadMbps = null,
                      downloadMibPerSec = null,
                      httpStatusCode = res.code,
                      finalUrl = currentUrl,
                      message = "Byte cap reached too quickly for reliable Mbps calculation.",
                    )
                  sufficientSample ->
                    SpeedTestResultSummary(
                      outcome = SpeedTestOutcome.TRANSFER_COMPLETED,
                      terminationReason = SpeedTerminationReason.EOF_REACHED,
                      transferredBytes = totalBytes,
                      elapsedNanos = elapsedNanos,
                      downloadMbps = computeMbps(totalBytes, elapsedNanos),
                      downloadMibPerSec = computeMibPerSec(totalBytes, elapsedNanos),
                      httpStatusCode = res.code,
                      finalUrl = currentUrl,
                      message = "Test resource transferred completely.",
                    )
                  else ->
                    SpeedTestResultSummary(
                      outcome = SpeedTestOutcome.INSUFFICIENT_SAMPLE,
                      terminationReason = SpeedTerminationReason.EOF_BEFORE_MIN_SAMPLE,
                      transferredBytes = totalBytes,
                      elapsedNanos = elapsedNanos,
                      downloadMbps = null,
                      downloadMibPerSec = null,
                      httpStatusCode = res.code,
                      finalUrl = currentUrl,
                      message = "Transferred bytes or duration below reliable measurement threshold.",
                    )
                }
            } catch (ce: CancellationException) {
              val elapsedNanos = (System.nanoTime() - startNanos).coerceAtLeast(0L)
              val cancelled =
                SpeedTestResultSummary(
                  outcome = SpeedTestOutcome.TRANSFER_CANCELLED,
                  terminationReason = SpeedTerminationReason.USER_CANCELLED,
                  transferredBytes = totalBytes,
                  elapsedNanos = elapsedNanos,
                  downloadMbps = null,
                  downloadMibPerSec = null,
                  httpStatusCode = res.code,
                  finalUrl = currentUrl,
                  message = "Speed test cancelled by user.",
                )
              persistSpeedResult(endpoint, cancelled, networkProfileLabel)
              throw ce
            }
          }

          redirectCheckCrossHost?.let { return@withContext it }
          immediateSummary?.let { summary ->
            persistSpeedResult(endpoint, summary, networkProfileLabel)
            return@withContext SpeedTestExecutionOutcome.Completed(summary)
          }
          if (shouldFollowRedirectTo != null) {
            currentUrl = shouldFollowRedirectTo
            continue
          }
        }
        error("Unreachable")
      } catch (ce: CancellationException) {
        throw ce
      } catch (e: SocketTimeoutException) {
        val summary =
          SpeedTestResultSummary(
            outcome = SpeedTestOutcome.TRANSFER_FAILED,
            terminationReason = SpeedTerminationReason.TIMEOUT,
            transferredBytes = 0L,
            elapsedNanos = 0L,
            downloadMbps = null,
            downloadMibPerSec = null,
            httpStatusCode = null,
            finalUrl = currentUrl,
            message = e.message ?: "Speed test timed out.",
          )
        persistSpeedResult(endpoint, summary, networkProfileLabel)
        return@withContext SpeedTestExecutionOutcome.Completed(summary)
      } catch (e: IOException) {
        val summary =
          SpeedTestResultSummary(
            outcome = SpeedTestOutcome.TRANSFER_FAILED,
            terminationReason = SpeedTerminationReason.NETWORK_ERROR,
            transferredBytes = 0L,
            elapsedNanos = 0L,
            downloadMbps = null,
            downloadMibPerSec = null,
            httpStatusCode = null,
            finalUrl = currentUrl,
            message = e.message ?: "Speed test transfer failed.",
          )
        persistSpeedResult(endpoint, summary, networkProfileLabel)
        return@withContext SpeedTestExecutionOutcome.Completed(summary)
      }
    }

  private suspend fun persistSpeedResult(
    endpoint: ServerEndpoint,
    summary: SpeedTestResultSummary,
    networkProfileLabel: String?,
  ) {
    val repo = catalogRepository ?: return
    val status =
      when (summary.outcome) {
        SpeedTestOutcome.TRANSFER_COMPLETED,
        SpeedTestOutcome.CAP_REACHED,
        SpeedTestOutcome.INSUFFICIENT_SAMPLE,
        -> TestStatus.REACHABLE
        SpeedTestOutcome.TRANSFER_CANCELLED -> TestStatus.CANCELLED
        SpeedTestOutcome.TRANSFER_FAILED -> TestStatus.UNREACHABLE
      }
    val errorClass =
      when (summary.terminationReason) {
        SpeedTerminationReason.TIMEOUT -> ErrorClassification.TIMEOUT
        SpeedTerminationReason.HTTPS_DOWNGRADE_BLOCKED -> ErrorClassification.PROTOCOL_DOWNGRADE_BLOCKED
        SpeedTerminationReason.USER_CANCELLED -> ErrorClassification.CANCELLED
        SpeedTerminationReason.HTTP_ERROR,
        SpeedTerminationReason.NETWORK_ERROR,
        SpeedTerminationReason.UNEXPECTED_CONTENT_ENCODING,
        SpeedTerminationReason.CROSS_HOST_REDIRECT_DECLINED,
        -> ErrorClassification.UNKNOWN_FAILURE
        else -> ErrorClassification.NONE
      }
    repo.recordTestResults(
      listOf(
        ServerTestResult(
          serverId = endpoint.id,
          scanRunId = "speed_${UUID.randomUUID()}",
          probeGroupId = null,
          isCoalescedReplica = false,
          serverNameSnapshot = endpoint.name,
          urlSnapshot = summary.finalUrl,
          testedAtEpochMs = clock(),
          scanMode = ScanMode.FULL_SCAN,
          protocol =
            if (summary.finalUrl.startsWith("https://", ignoreCase = true)) {
              ServerProtocol.HTTPS
            } else {
              ServerProtocol.HTTP
            },
          status = status,
          httpStatusCode = summary.httpStatusCode,
          finalUrl = summary.finalUrl.takeIf { it != endpoint.originalUrl },
          errorClassification = errorClass,
          failureMessage = summary.message,
          downloadMbps = summary.downloadMbps,
          downloadMibPerSec = summary.downloadMibPerSec,
          transferredBytes = summary.transferredBytes,
          speedElapsedNanos = summary.elapsedNanos,
          speedTestState = summary.outcome,
          speedTerminationReason = summary.terminationReason,
          networkProfileLabel = networkProfileLabel,
        ),
      ),
    )
  }

  companion object {
    const val STREAM_BUFFER_BYTES = 32 * 1024
    const val DEFAULT_MIN_SAMPLE_BYTES = 64L * 1024L
    const val DEFAULT_MIN_SAMPLE_NANOS = 100_000_000L // 100 ms
    private const val PROGRESS_THROTTLE_NANOS = 100_000_000L
    private const val MAX_REDIRECTS = 5

    fun computeMbps(
      transferredBytes: Long,
      elapsedNanos: Long,
    ): Double {
      if (transferredBytes <= 0L || elapsedNanos <= 0L) return 0.0
      val elapsedSeconds = elapsedNanos.toDouble() / 1_000_000_000.0
      return (transferredBytes.toDouble() * 8.0) / (elapsedSeconds * 1_000_000.0)
    }

    fun computeMibPerSec(
      transferredBytes: Long,
      elapsedNanos: Long,
    ): Double {
      if (transferredBytes <= 0L || elapsedNanos <= 0L) return 0.0
      val elapsedSeconds = elapsedNanos.toDouble() / 1_000_000_000.0
      return transferredBytes.toDouble() / (elapsedSeconds * 1_048_576.0)
    }
  }
}
