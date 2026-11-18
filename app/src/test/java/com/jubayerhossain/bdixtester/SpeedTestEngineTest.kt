package com.jubayerhossain.bdixtester

import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.SpeedTerminationReason
import com.jubayerhossain.bdixtester.core.model.SpeedTestOutcome
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.data.network.SpeedTestEngine
import com.jubayerhossain.bdixtester.data.network.SpeedTestExecutionOutcome
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.zip.GZIPOutputStream

class SpeedTestEngineTest {
  private lateinit var server: MockWebServer

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  private fun sampleEndpoint(url: String) =
    ServerEndpoint(
      id = "builtin:0",
      name = "test",
      originalUrl = url,
      host = "127.0.0.1",
      explicitPort = null,
      protocol = ServerProtocol.HTTP,
      category = ServerCategory.UNCATEGORIZED,
      isFeatured = false,
      sourceType = CatalogSourceType.BUILT_IN,
      sourceIndex = 0,
      verificationState = VerificationState.UNKNOWN,
      lastReviewedAtEpochMs = null,
      speedTestUrl = url,
      isPrivateIp = false,
    )

  @Test
  fun capReachedStopsExactlyAtMaxBytesWithReliableMetrics() =
    runTest {
      val totalServerBytes = 512 * 1024
      val capBytes = 128L * 1024L
      server.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(Buffer().write(ByteArray(totalServerBytes) { 0x42 })),
      )

      val engine =
        SpeedTestEngine(
          baseClient = OkHttpClient(),
          minSampleBytes = 32L * 1024L,
          minSampleNanos = 1L, // deterministic even on fast loopback
        )
      val url = server.url("/payload.bin").toString()
      val outcome =
        engine.runSpeedTest(
          endpoint = sampleEndpoint(url),
          targetUrl = url,
          maxBytes = capBytes,
        )

      assertTrue(outcome is SpeedTestExecutionOutcome.Completed)
      val summary = (outcome as SpeedTestExecutionOutcome.Completed).result
      assertEquals(SpeedTestOutcome.CAP_REACHED, summary.outcome)
      assertEquals(SpeedTerminationReason.BYTE_CAP_REACHED, summary.terminationReason)
      assertEquals(capBytes, summary.transferredBytes)
      assertNotNull(summary.downloadMbps)
      assertEquals("identity", server.takeRequest().getHeader("Accept-Encoding"))
    }

  @Test
  fun fastCapBelowMinTimeThresholdPreservesByteCapReachedReasonWithInsufficientSample() =
    runTest {
      val capBytes = 128L * 1024L
      server.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(Buffer().write(ByteArray(256 * 1024) { 0x41 })),
      )

      val engine =
        SpeedTestEngine(
          baseClient = OkHttpClient(),
          minSampleBytes = 64L * 1024L,
          minSampleNanos = 10_000_000_000L, // force insufficient-duration threshold on loopback
        )
      val url = server.url("/fast.bin").toString()
      val outcome =
        engine.runSpeedTest(
          endpoint = sampleEndpoint(url),
          targetUrl = url,
          maxBytes = capBytes,
        ) as SpeedTestExecutionOutcome.Completed

      assertEquals(SpeedTestOutcome.INSUFFICIENT_SAMPLE, outcome.result.outcome)
      assertEquals(SpeedTerminationReason.BYTE_CAP_REACHED, outcome.result.terminationReason)
      assertNull(outcome.result.downloadMbps)
    }

  @Test
  fun eofCompletedTransferReportsTransferCompleted() =
    runTest {
      val bodyBytes = 96 * 1024
      server.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(Buffer().write(ByteArray(bodyBytes) { 0x55 })),
      )
      val engine =
        SpeedTestEngine(
          baseClient = OkHttpClient(),
          minSampleBytes = 32L * 1024L,
          minSampleNanos = 1L,
        )
      val url = server.url("/small.bin").toString()
      val outcome =
        engine.runSpeedTest(
          endpoint = sampleEndpoint(url),
          targetUrl = url,
          maxBytes = 512L * 1024L,
        ) as SpeedTestExecutionOutcome.Completed

      assertEquals(SpeedTestOutcome.TRANSFER_COMPLETED, outcome.result.outcome)
      assertEquals(SpeedTerminationReason.EOF_REACHED, outcome.result.terminationReason)
      assertEquals(bodyBytes.toLong(), outcome.result.transferredBytes)
    }

  @Test
  fun unexpectedGzipEncodingAndCrossHostRedirectsAreEnforced() =
    runTest {
      // 1. Gzip rejected
      val gzOut = java.io.ByteArrayOutputStream()
      GZIPOutputStream(gzOut).use { it.write("hello".toByteArray()) }
      server.enqueue(
        MockResponse()
          .setResponseCode(200)
          .addHeader("Content-Encoding", "gzip")
          .setBody(Buffer().write(gzOut.toByteArray())),
      )
      val engine = SpeedTestEngine(baseClient = OkHttpClient())
      val url = server.url("/gz.bin").toString()
      val gzOutcome =
        engine.runSpeedTest(
          sampleEndpoint(url),
          url,
          1024 * 1024,
        ) as SpeedTestExecutionOutcome.Completed
      assertEquals(SpeedTestOutcome.TRANSFER_FAILED, gzOutcome.result.outcome)
      assertEquals(SpeedTerminationReason.UNEXPECTED_CONTENT_ENCODING, gzOutcome.result.terminationReason)

      // 2. Cross-host redirect prompts confirmation
      val secondServer = MockWebServer()
      secondServer.start()
      try {
        val target2 = "http://127.0.0.1:${secondServer.port}/mirror.bin"
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", target2))
        secondServer.enqueue(
          MockResponse().setResponseCode(200).setBody(Buffer().write(ByteArray(128 * 1024))),
        )
        val promptOutcome =
          engine.runSpeedTest(
            sampleEndpoint(url),
            url,
            1024 * 1024,
            allowConfirmedCrossHost = false,
          )
        assertTrue(promptOutcome is SpeedTestExecutionOutcome.CrossHostRedirectRequiresConfirmation)

        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", target2))
        val confirmedEngine = SpeedTestEngine(OkHttpClient(), minSampleBytes = 32 * 1024, minSampleNanos = 1L)
        val completed =
          confirmedEngine.runSpeedTest(
            endpoint = sampleEndpoint(url),
            targetUrl = url,
            maxBytes = 1024 * 1024,
            allowConfirmedCrossHost = true,
          ) as SpeedTestExecutionOutcome.Completed
        assertEquals(SpeedTestOutcome.TRANSFER_COMPLETED, completed.result.outcome)
      } finally {
        secondServer.shutdown()
      }
    }
}
