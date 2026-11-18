package com.jubayerhossain.bdixtester

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.core.network.ConnectivityObserver
import com.jubayerhossain.bdixtester.core.network.NetworkSnapshot
import com.jubayerhossain.bdixtester.core.network.StrictWifiValidator
import com.jubayerhossain.bdixtester.core.network.TransportType
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.local.BdixDatabase
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.remote.CatalogSignatureVerifier
import com.jubayerhossain.bdixtester.data.remote.RemoteCatalogSyncManager
import com.jubayerhossain.bdixtester.data.remote.SignatureVerificationResult
import com.jubayerhossain.bdixtester.data.remote.TrustedKeyRegistry
import com.jubayerhossain.bdixtester.data.remote.TrustedKeyStatus
import com.jubayerhossain.bdixtester.data.remote.TrustedPublicKeyEntry
import com.jubayerhossain.bdixtester.data.repository.RoomCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CatalogSignatureAndSyncTest {
  private lateinit var db: BdixDatabase
  private var activeRepo: RoomCatalogRepository? = null
  private lateinit var keyPair: KeyPair
  private val keyId = "test-ec-key-2026"

  @Before
  fun setUp() {
    val context: Context = ApplicationProvider.getApplicationContext()
    db =
      Room
        .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val kpg = KeyPairGenerator.getInstance("EC")
    kpg.initialize(ECGenParameterSpec("secp256r1"))
    keyPair = kpg.generateKeyPair()
  }

  @After
  fun tearDown() {
    runBlocking { activeRepo?.closeAndJoin() }
    db.close()
  }

  private fun signBytesBase64(bytes: ByteArray): ByteArray {
    val sig = Signature.getInstance("SHA256withECDSA")
    sig.initSign(keyPair.private)
    sig.update(bytes)
    val der = sig.sign()
    return Base64.encodeToString(der, Base64.NO_WRAP).toByteArray(Charsets.UTF_8)
  }

  private fun testRegistry(status: TrustedKeyStatus = TrustedKeyStatus.ACTIVE): TrustedKeyRegistry {
    val spkiB64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    return TrustedKeyRegistry(
      listOf(TrustedPublicKeyEntry(keyId = keyId, x509SpkiBase64 = spkiB64, status = status)),
    )
  }

  @Test
  fun verifierEnforcesRawUtf8BytesRejectsBomReserializationAndRevokedKeys() {
    val verifier = CatalogSignatureVerifier(testRegistry())
    val rawManifest = """{"keyId":"$keyId","catalogSequence":1}""".toByteArray(Charsets.UTF_8)
    val sig = signBytesBase64(rawManifest)

    assertTrue(verifier.verifyDetachedManifest(rawManifest, sig) is SignatureVerificationResult.Verified)

    // Reserialized whitespace alteration must fail
    val reformatted = """{ "keyId" : "$keyId", "catalogSequence" : 1 }""".toByteArray(Charsets.UTF_8)
    assertTrue(verifier.verifyDetachedManifest(reformatted, sig) is SignatureVerificationResult.Rejected)

    // UTF-8 BOM must fail
    val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + rawManifest
    assertTrue(
      verifier.verifyDetachedManifest(withBom, signBytesBase64(withBom)) is SignatureVerificationResult.Rejected,
    )

    // Revoked key must fail
    val revokedVerifier = CatalogSignatureVerifier(testRegistry(TrustedKeyStatus.REVOKED))
    assertTrue(revokedVerifier.verifyDetachedManifest(rawManifest, sig) is SignatureVerificationResult.Rejected)
  }

  @Test
  fun strictWifiValidatorPermitsWifiAndWifiPlusVpnAndRejectsVpnOnlyOrCellular() {
    assertTrue(
      StrictWifiValidator
        .evaluate(
          NetworkSnapshot(true, TransportType.WIFI, hasWifiTransport = true),
          true,
        ).isAllowed,
    )
    assertTrue(
      StrictWifiValidator
        .evaluate(
          NetworkSnapshot(true, TransportType.WIFI, hasWifiTransport = true, hasVpnTransport = true),
          true,
        ).isAllowed,
    )
    assertFalse(
      StrictWifiValidator
        .evaluate(
          NetworkSnapshot(true, TransportType.VPN, hasWifiTransport = false, hasVpnTransport = true),
          true,
        ).isAllowed,
    )
    assertFalse(
      StrictWifiValidator
        .evaluate(
          NetworkSnapshot(true, TransportType.CELLULAR, hasWifiTransport = false),
          true,
        ).isAllowed,
    )
  }

  @Test
  fun fullSyncPipelineHandlesUpdates304AndAntiRollbackWithoutTouchingBuiltInOrCustomRows() =
    runTest {
      val repo = RoomCatalogRepository(db).also { activeRepo = it }
      repo.ensureInitialized()
      repo.saveCustomServer(
        name = "Custom Keep",
        rawUrl = "http://10.0.0.5",
        category = com.jubayerhossain.bdixtester.core.model.ServerCategory.OTHER,
      )

      val payloadSeq1 =
        """{"schemaVersion":1,"catalogSequence":1,"entries":[{"id":"e1","name":"Mirror 1","url":"https://m1.example.net","category":"MOVIES","isFeatured":true}]}"""
          .toByteArray(
            Charsets.UTF_8,
          )
      val shaSeq1 = CatalogSignatureVerifier.sha256Hex(payloadSeq1)
      val manifestSeq1 =
        """{"keyId":"$keyId","catalogSequence":1,"schemaVersion":1,"minAppVersionCode":1,"payloadUrl":"https://catalog.example.test/payload.json","payloadSize":${payloadSeq1.size},"sha256":"$shaSeq1","generatedAt":"2026-11-18T00:00:00Z","publishedAt":"2026-11-18T00:00:00Z"}"""
          .toByteArray(
            Charsets.UTF_8,
          )
      val sigSeq1 = signBytesBase64(manifestSeq1)

      var currentManifest = manifestSeq1
      var currentSig = sigSeq1
      var currentPayload = payloadSeq1
      var return304 = false

      val interceptor =
        Interceptor { chain ->
          val req = chain.request()
          val path = req.url.encodedPath
          when {
            path.endsWith("/manifest.json") && return304 ->
              Response
                .Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(304)
                .message("Not Modified")
                .body(ByteArray(0).toResponseBody(null))
                .build()
            path.endsWith("/manifest.json") ->
              Response
                .Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .addHeader("ETag", "\"etag-1\"")
                .body(currentManifest.toResponseBody("application/json".toMediaType()))
                .build()
            path.endsWith("/manifest.json.sig") ->
              Response
                .Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(currentSig.toResponseBody("text/plain".toMediaType()))
                .build()
            path.endsWith("/payload.json") ->
              Response
                .Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(currentPayload.toResponseBody("application/json".toMediaType()))
                .build()
            else ->
              Response
                .Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body(ByteArray(0).toResponseBody(null))
                .build()
          }
        }

      val snapshotFlow = MutableStateFlow(NetworkSnapshot(true, TransportType.WIFI, hasWifiTransport = true))
      val observer =
        object : ConnectivityObserver {
          override fun observeConnectivity(): Flow<NetworkSnapshot> = snapshotFlow

          override fun currentSnapshot(): NetworkSnapshot = snapshotFlow.value
        }

      val syncManager =
        RemoteCatalogSyncManager(
          database = db,
          connectivityObserver = observer,
          signatureVerifier = CatalogSignatureVerifier(testRegistry()),
          baseClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
          appVersionCode = 1,
        )
      val settings =
        AppSettings(
          remoteCatalogUrl = "https://catalog.example.test/manifest.json",
          wifiOnlyCatalogUpdates = true,
        )

      // 1. Accept valid sequence 1
      val res1 = syncManager.syncCatalog(settings)
      assertEquals(SyncStatus.UPDATED, res1.status)
      assertEquals(1L, res1.catalogSequence)
      assertEquals(1, res1.entryCount)
      assertEquals(BuiltInCatalog.DEFAULT_CURATED_COUNT, db.serverDao().countBuiltInServers())
      assertEquals(1, db.serverDao().countRemoteServers())

      // 2. 304 preserves sequence 1
      return304 = true
      val res304 = syncManager.syncCatalog(settings)
      assertEquals(SyncStatus.NOT_MODIFIED, res304.status)
      assertEquals(1L, res304.catalogSequence)
      return304 = false

      // 3. Equal sequence + different digest rejected as CONFLICT
      val alteredPayload = """{"schemaVersion":1,"catalogSequence":1,"entries":[]}""".toByteArray(Charsets.UTF_8)
      val conflictManifest =
        """{"keyId":"$keyId","catalogSequence":1,"schemaVersion":1,"minAppVersionCode":1,"payloadUrl":"https://catalog.example.test/payload.json","payloadSize":${alteredPayload.size},"sha256":"${CatalogSignatureVerifier.sha256Hex(
          alteredPayload,
        )}","generatedAt":"2026-11-18T00:00:00Z","publishedAt":"2026-11-18T00:00:00Z"}"""
          .toByteArray(Charsets.UTF_8)
      currentManifest = conflictManifest
      currentSig = signBytesBase64(conflictManifest)
      currentPayload = alteredPayload
      val resConflict = syncManager.syncCatalog(settings)
      assertEquals(SyncStatus.REJECTED_CONFLICT, resConflict.status)

      // 4. Lower sequence rejected (if stored seq were higher) -> verify seq 2 update then seq 1 rollback
      val payloadSeq2 =
        """{"schemaVersion":1,"catalogSequence":2,"entries":[{"id":"e2","name":"Mirror 2","url":"https://m2.example.net","category":"MOVIES","isFeatured":false}]}"""
          .toByteArray(
            Charsets.UTF_8,
          )
      val manifestSeq2 =
        """{"keyId":"$keyId","catalogSequence":2,"schemaVersion":1,"minAppVersionCode":1,"payloadUrl":"https://catalog.example.test/payload.json","payloadSize":${payloadSeq2.size},"sha256":"${CatalogSignatureVerifier.sha256Hex(
          payloadSeq2,
        )}","generatedAt":"2026-11-18T00:00:00Z","publishedAt":"2026-11-18T00:00:00Z"}"""
          .toByteArray(Charsets.UTF_8)
      currentManifest = manifestSeq2
      currentSig = signBytesBase64(manifestSeq2)
      currentPayload = payloadSeq2
      assertEquals(SyncStatus.UPDATED, syncManager.syncCatalog(settings).status)

      currentManifest = manifestSeq1
      currentSig = sigSeq1
      currentPayload = payloadSeq1
      val resRollback = syncManager.syncCatalog(settings)
      assertEquals(SyncStatus.REJECTED_ROLLBACK, resRollback.status)
      assertEquals(2L, resRollback.catalogSequence)
    }
}
