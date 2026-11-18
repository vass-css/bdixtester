package com.jubayerhossain.bdixtester

import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.data.network.EndpointProbeConfig
import com.jubayerhossain.bdixtester.data.network.FtpControlTester
import com.jubayerhossain.bdixtester.data.network.HttpEndpointTester
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class NetworkProtocolTest {
  private lateinit var mockServer: MockWebServer
  private val httpTester = HttpEndpointTester(OkHttpClient())
  private val ftpTester = FtpControlTester()

  @Before
  fun setUp() {
    mockServer = MockWebServer()
    mockServer.start()
  }

  @After
  fun tearDown() {
    mockServer.shutdown()
  }

  private fun httpEndpoint(
    url: String,
    protocol: ServerProtocol = ServerProtocol.HTTP,
  ): ServerEndpoint {
    val parsed =
      com.jubayerhossain.bdixtester.core.validation.UrlParser
        .parse(url)
    return ServerEndpoint(
      id = "test_ep",
      name = "test",
      originalUrl = url,
      host = parsed.host ?: "127.0.0.1",
      explicitPort = parsed.explicitPort,
      protocol = protocol,
      category = ServerCategory.UNCATEGORIZED,
      isFeatured = false,
      sourceType = CatalogSourceType.BUILT_IN,
      verificationState = VerificationState.UNKNOWN,
      lastReviewedAtEpochMs = null,
      speedTestUrl = null,
      isPrivateIp = parsed.isPrivateIp,
    )
  }

  private fun fullConfig() =
    EndpointProbeConfig(
      scanMode = ScanMode.FULL_SCAN,
      connectTimeoutMs = 3_000,
      readTimeoutMs = 3_000,
      overallDeadlineMs = 6_000,
      networkProfileLabel = "Test ISP",
    )

  @Test
  fun httpHead200ReturnsReachableWithStageLatencies() =
    runTest {
      mockServer.enqueue(MockResponse().setResponseCode(200))
      val url = mockServer.url("/").toString()
      val res = httpTester.probe(httpEndpoint(url), fullConfig())

      assertEquals(TestStatus.REACHABLE, res.status)
      assertEquals(200, res.httpStatusCode)
      assertEquals("Test ISP", res.networkProfileLabel)
      assertNotNull(res.httpResponseLatencyMs)
      assertEquals("HEAD", mockServer.takeRequest().method)
    }

  @Test
  fun httpHead405FallsBackToRangeGetWith1024ByteLimit() =
    runTest {
      mockServer.enqueue(MockResponse().setResponseCode(405))
      mockServer.enqueue(MockResponse().setResponseCode(200).setBody("A".repeat(16_384)))
      val url = mockServer.url("/movies/").toString()
      val res = httpTester.probe(httpEndpoint(url), fullConfig())

      assertEquals(TestStatus.REACHABLE, res.status)
      val first = mockServer.takeRequest()
      val second = mockServer.takeRequest()
      assertEquals("HEAD", first.method)
      assertEquals("GET", second.method)
      assertEquals("bytes=0-1023", second.getHeader("Range"))
    }

  @Test
  fun http401And403And404And500ClassifyAsRestricted() =
    runTest {
      listOf(
        401 to ErrorClassification.HTTP_AUTH_REQUIRED,
        403 to ErrorClassification.HTTP_FORBIDDEN,
        404 to ErrorClassification.HTTP_NOT_FOUND,
        500 to ErrorClassification.HTTP_SERVER_ERROR,
      ).forEach { (code, expectedErr) ->
        mockServer.enqueue(MockResponse().setResponseCode(code))
        val res = httpTester.probe(httpEndpoint(mockServer.url("/").toString()), fullConfig())
        assertEquals(TestStatus.RESTRICTED, res.status)
        assertEquals(expectedErr, res.errorClassification)
        assertEquals(code, res.httpStatusCode)
      }
    }

  @Test
  fun quickScan2500MsTimeoutMarksInconclusive() =
    runTest {
      mockServer.enqueue(MockResponse().setResponseCode(200).setHeadersDelay(3_500, TimeUnit.MILLISECONDS))
      val quickConfig =
        EndpointProbeConfig(
          scanMode = ScanMode.QUICK_SCAN,
          connectTimeoutMs = 400,
          readTimeoutMs = 400,
          overallDeadlineMs = 400,
        )
      val res = httpTester.probe(httpEndpoint(mockServer.url("/").toString()), quickConfig)
      assertEquals(TestStatus.TIMED_OUT, res.status)
      assertTrue(res.isQuickScanInconclusive)
    }

  @Test
  fun ftp220GreetingAndCleanQuitAreVerified() =
    runTest {
      val observedClientQuit = AtomicReference<String?>(null)
      val server = ServerSocket(0)
      val serverThread =
        thread {
          server.accept().use { sock ->
            val out = PrintWriter(sock.getOutputStream(), true)
            val inp = BufferedReader(InputStreamReader(sock.getInputStream()))
            out.print("220 FTPBD Ready\r\n")
            out.flush()
            observedClientQuit.set(inp.readLine())
            out.print("221 Goodbye\r\n")
            out.flush()
          }
        }
      try {
        val res =
          ftpTester.probe(
            httpEndpoint("ftp://127.0.0.1:${server.localPort}", ServerProtocol.FTP),
            fullConfig(),
          )
        serverThread.join(2000)
        assertEquals(TestStatus.REACHABLE, res.status)
        assertEquals(220, res.ftpGreetingCode)
        assertEquals("QUIT", observedClientQuit.get())
      } finally {
        server.close()
      }
    }

  @Test
  fun ftp120FollowedBy220AndMultiline220WorkProperly() =
    runTest {
      // Case A: 120 followed by 220
      val s1 = ServerSocket(0)
      val t1 =
        thread {
          s1.accept().use { sock ->
            val out = PrintWriter(sock.getOutputStream(), true)
            out.print("120 Service ready in 2 seconds\r\n")
            out.flush()
            Thread.sleep(50)
            out.print("220 FTP Ready\r\n")
            out.flush()
          }
        }
      val res1 = ftpTester.probe(httpEndpoint("ftp://127.0.0.1:${s1.localPort}", ServerProtocol.FTP), fullConfig())
      t1.join(2000)
      s1.close()
      assertEquals(TestStatus.REACHABLE, res1.status)
      assertEquals(220, res1.ftpGreetingCode)

      // Case B: Multiline 220-
      val s2 = ServerSocket(0)
      val t2 =
        thread {
          s2.accept().use { sock ->
            val out = PrintWriter(sock.getOutputStream(), true)
            out.print("220-Welcome to BDIX FTP\r\n220-Line 2\r\n220 Ready for user\r\n")
            out.flush()
          }
        }
      val res2 = ftpTester.probe(httpEndpoint("ftp://127.0.0.1:${s2.localPort}", ServerProtocol.FTP), fullConfig())
      t2.join(2000)
      s2.close()
      assertEquals(TestStatus.REACHABLE, res2.status)
      assertEquals(220, res2.ftpGreetingCode)
    }

  @Test
  fun ftp421MalformedTimeoutAndFloodAreRejectedAppropriately() =
    runTest {
      // 421 busy
      val s421 = ServerSocket(0)
      val t421 =
        thread {
          s421.accept().use { sock ->
            sock.getOutputStream().write("421 Too many users\r\n".toByteArray())
            sock.getOutputStream().flush()
          }
        }
      val r421 = ftpTester.probe(httpEndpoint("ftp://127.0.0.1:${s421.localPort}", ServerProtocol.FTP), fullConfig())
      t421.join(2000)
      s421.close()
      assertEquals(TestStatus.RESTRICTED, r421.status)
      assertEquals(421, r421.ftpGreetingCode)

      // Malformed HTTP banner on FTP port
      val sBad = ServerSocket(0)
      val tBad =
        thread {
          sBad.accept().use { sock ->
            sock.getOutputStream().write("HTTP/1.1 200 OK\r\n".toByteArray())
            sock.getOutputStream().flush()
          }
        }
      val rBad = ftpTester.probe(httpEndpoint("ftp://127.0.0.1:${sBad.localPort}", ServerProtocol.FTP), fullConfig())
      tBad.join(2000)
      sBad.close()
      assertEquals(TestStatus.UNREACHABLE, rBad.status)
      assertEquals(ErrorClassification.FTP_PROTOCOL_ERROR, rBad.errorClassification)

      // Flood > 8 KiB
      val sFlood = ServerSocket(0)
      val tFlood =
        thread {
          runCatching {
            sFlood.accept().use { sock ->
              val line = "220-" + "X".repeat(300) + "\r\n"
              repeat(40) {
                sock.getOutputStream().write(line.toByteArray())
              }
              sock.getOutputStream().flush()
            }
          }
        }
      val rFlood =
        ftpTester.probe(
          httpEndpoint("ftp://127.0.0.1:${sFlood.localPort}", ServerProtocol.FTP),
          fullConfig(),
        )
      tFlood.join(2000)
      sFlood.close()
      assertEquals(TestStatus.UNREACHABLE, rFlood.status)
      assertEquals(ErrorClassification.FTP_PROTOCOL_ERROR, rFlood.errorClassification)

      // Greeting timeout
      val sSlow = ServerSocket(0)
      val tSlow =
        thread {
          runCatching {
            sSlow.accept().use {
              Thread.sleep(1500)
            }
          }
        }
      val fastConfig = fullConfig().copy(readTimeoutMs = 250, overallDeadlineMs = 400)
      val rSlow = ftpTester.probe(httpEndpoint("ftp://127.0.0.1:${sSlow.localPort}", ServerProtocol.FTP), fastConfig)
      tSlow.join(2000)
      sSlow.close()
      assertEquals(TestStatus.TIMED_OUT, rSlow.status)
    }

  @Test
  fun ftpCancellationAndFtpsUnsupportedBehaveCorrectly() =
    runTest {
      val ftpsRes = ftpTester.probe(httpEndpoint("ftps://127.0.0.1:990", ServerProtocol.FTPS), fullConfig())
      assertEquals(TestStatus.UNSUPPORTED, ftpsRes.status)

      val acceptedLatch = java.util.concurrent.CountDownLatch(1)
      val closedByCancellation = AtomicBoolean(false)
      val sHang = ServerSocket(0)
      val tHang =
        thread {
          runCatching {
            sHang.accept().use { sock ->
              acceptedLatch.countDown()
              val b = sock.getInputStream().read()
              if (b == -1) closedByCancellation.set(true)
            }
          }.onFailure { closedByCancellation.set(true) }
        }
      val job =
        async(kotlinx.coroutines.Dispatchers.IO) {
          ftpTester.probe(
            httpEndpoint("ftp://127.0.0.1:${sHang.localPort}", ServerProtocol.FTP),
            fullConfig().copy(readTimeoutMs = 5_000, overallDeadlineMs = 5_000),
          )
        }
      assertTrue("Client did not connect before cancellation", acceptedLatch.await(2, TimeUnit.SECONDS))
      job.cancel(CancellationException("User stopped"))
      runCatching { job.await() }
      tHang.join(2000)
      sHang.close()
      assertTrue(closedByCancellation.get())
    }
}
