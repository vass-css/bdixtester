package com.jubayerhossain.bdixtester.data.remote

import androidx.room.withTransaction
import com.jubayerhossain.bdixtester.BuildConfig
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.network.ConnectivityObserver
import com.jubayerhossain.bdixtester.core.network.StrictWifiValidator
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.local.BdixDatabase
import com.jubayerhossain.bdixtester.data.local.entity.CatalogMetadataEntity
import com.jubayerhossain.bdixtester.data.local.entity.ServerEntity
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.repository.RoomCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RemoteCatalogSyncManager(
  private val database: BdixDatabase,
  private val connectivityObserver: ConnectivityObserver,
  private val signatureVerifier: CatalogSignatureVerifier = CatalogSignatureVerifier(),
  private val baseClient: OkHttpClient = OkHttpClient(),
  private val appVersionCode: Int = BuildConfig.VERSION_CODE,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clock: () -> Long = { System.currentTimeMillis() },
) {
  private val syncMutex = Mutex()
  private val metadataDao = database.catalogMetadataDao()
  private val serverDao = database.serverDao()

  private val client: OkHttpClient =
    baseClient
      .newBuilder()
      .followRedirects(false)
      .followSslRedirects(false)
      .connectTimeout(10_000L, TimeUnit.MILLISECONDS)
      .readTimeout(15_000L, TimeUnit.MILLISECONDS)
      .callTimeout(30_000L, TimeUnit.MILLISECONDS)
      .build()

  suspend fun syncCatalog(
    settings: AppSettings,
    overrideManifestUrl: String? = null,
  ): RemoteSyncResult =
    withContext(ioDispatcher) {
      syncMutex.withLock {
        val now = clock()
        val manifestUrlString = (overrideManifestUrl ?: settings.remoteCatalogUrl).trim()
        val existingMeta = metadataDao.getMetadata(RoomCatalogRepository.REMOTE_FEED_ID)

        val manifestUrl = manifestUrlString.toHttpUrlOrNull()
        if (manifestUrl == null || !manifestUrl.scheme.equals("https", ignoreCase = true)) {
          return@withLock recordOutcome(
            existingMeta = existingMeta,
            sourceUrl = manifestUrlString,
            attemptEpochMs = now,
            status = SyncStatus.REJECTED_ORIGIN,
            message = "Remote catalog feed URL must be a valid https:// URL.",
          )
        }

        // 1. Pre-flight strict Wi-Fi check
        val preWifi =
          StrictWifiValidator.evaluate(
            snapshot = connectivityObserver.currentSnapshot(),
            wifiOnlyRequired = settings.wifiOnlyCatalogUpdates,
          )
        if (!preWifi.isAllowed) {
          return@withLock recordOutcome(
            existingMeta = existingMeta,
            sourceUrl = manifestUrlString,
            attemptEpochMs = now,
            status = SyncStatus.WIFI_REQUIRED,
            message = preWifi.reason,
          )
        }

        // Monitor active network mid-flight to abort immediately if Wi-Fi drops
        val activeCalls = Collections.synchronizedSet(mutableSetOf<Call>())
        val wifiLostMidFlight = AtomicBoolean(false)
        val monitorScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        val wifiMonitorJob: Job? =
          if (settings.wifiOnlyCatalogUpdates) {
            monitorScope.launch {
              connectivityObserver.observeConnectivity().collect { snapshot ->
                val eval = StrictWifiValidator.evaluate(snapshot, wifiOnlyRequired = true)
                if (!eval.isAllowed) {
                  wifiLostMidFlight.set(true)
                  synchronized(activeCalls) {
                    activeCalls.forEach { it.cancel() }
                  }
                }
              }
            }
          } else {
            null
          }

        try {
          metadataDao.upsertMetadata(
            (existingMeta ?: emptyMetadata(manifestUrlString)).copy(
              sourceUrl = manifestUrlString,
              lastSyncAttemptAtEpochMs = now,
              lastSyncStatus = SyncStatus.SYNCING.name,
              lastSyncMessage = "Checking remote catalog manifest…",
            ),
          )

          // 2. Fetch manifest (conditional headers + HTTPS-only + max 64 KiB)
          val manifestFetch =
            fetchBoundedBytes(
              initialUrl = manifestUrl,
              maxBytes = CatalogSignatureVerifier.MAX_MANIFEST_BYTES,
              etag = existingMeta?.etag,
              lastModified = existingMeta?.lastModified,
              allowCrossOrigin = true,
              originUrl = manifestUrl,
              activeCalls = activeCalls,
            )

          if (wifiLostMidFlight.get()) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.WIFI_REQUIRED,
              message = "Sync aborted mid-flight because verified Wi-Fi transport was lost.",
            )
          }

          when (manifestFetch) {
            is BoundedFetchResult.NotModified -> {
              val reusedCount = serverDao.countRemoteServers()
              val updatedMeta =
                (existingMeta ?: emptyMetadata(manifestUrlString)).copy(
                  sourceUrl = manifestUrlString,
                  lastSyncAttemptAtEpochMs = clock(),
                  lastSyncStatus = SyncStatus.NOT_MODIFIED.name,
                  lastSyncMessage =
                    "304 Not Modified — reused previously verified catalog sequence " +
                      "${existingMeta?.catalogSequence ?: 0L}.",
                )
              metadataDao.upsertMetadata(updatedMeta)
              return@withLock RemoteSyncResult(
                status = SyncStatus.NOT_MODIFIED,
                catalogSequence = updatedMeta.catalogSequence,
                entryCount = reusedCount,
                message = updatedMeta.lastSyncMessage.orEmpty(),
              )
            }
            is BoundedFetchResult.SizeExceeded -> {
              return@withLock recordOutcome(
                existingMeta = existingMeta,
                sourceUrl = manifestUrlString,
                attemptEpochMs = clock(),
                status = SyncStatus.REJECTED_SIZE,
                message = manifestFetch.reason,
              )
            }
            is BoundedFetchResult.OriginBlocked -> {
              return@withLock recordOutcome(
                existingMeta = existingMeta,
                sourceUrl = manifestUrlString,
                attemptEpochMs = clock(),
                status = SyncStatus.REJECTED_ORIGIN,
                message = manifestFetch.reason,
              )
            }
            is BoundedFetchResult.Error -> {
              return@withLock recordOutcome(
                existingMeta = existingMeta,
                sourceUrl = manifestUrlString,
                attemptEpochMs = clock(),
                status = SyncStatus.NETWORK_ERROR,
                message = manifestFetch.reason,
              )
            }
            is BoundedFetchResult.Success -> {
              // Continue below
            }
          }

          // 3. Fetch detached signature (${manifestUrl}.sig) over HTTPS
          val signatureUrl = buildSignatureUrl(manifestUrl)
          val sigFetch =
            fetchBoundedBytes(
              initialUrl = signatureUrl,
              maxBytes = CatalogSignatureVerifier.MAX_SIGNATURE_BYTES,
              etag = null,
              lastModified = null,
              allowCrossOrigin = settings.allowCrossOriginRemotePayload,
              originUrl = manifestUrl,
              activeCalls = activeCalls,
            )

          if (wifiLostMidFlight.get()) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.WIFI_REQUIRED,
              message = "Sync aborted mid-flight because verified Wi-Fi transport was lost.",
            )
          }

          val sigBytes =
            when (sigFetch) {
              is BoundedFetchResult.Success -> sigFetch.bytes
              is BoundedFetchResult.SizeExceeded -> {
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SIZE,
                  message = sigFetch.reason,
                )
              }
              is BoundedFetchResult.OriginBlocked -> {
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_ORIGIN,
                  message = sigFetch.reason,
                )
              }
              is BoundedFetchResult.Error, BoundedFetchResult.NotModified -> {
                val reason =
                  (sigFetch as? BoundedFetchResult.Error)?.reason
                    ?: "Detached manifest signature unavailable."
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SIGNATURE,
                  message = reason,
                )
              }
            }

          // 4. Verify detached ECDSA signature directly over exact raw UTF-8 manifest bytes
          val verified =
            when (val v = signatureVerifier.verifyDetachedManifest(manifestFetch.bytes, sigBytes)) {
              is SignatureVerificationResult.Verified -> v
              is SignatureVerificationResult.Rejected -> {
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SIGNATURE,
                  message = v.reason,
                )
              }
            }

          // 5. Parse manifest JSON cleanly AFTER verification
          val manifest =
            parseManifestJson(verified.rawManifestUtf8)
              .getOrElse { err ->
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SCHEMA,
                  message = err.message ?: "Invalid catalog manifest JSON.",
                )
              }

          // 6. Validate schemaVersion, minAppVersionCode, payloadUrl origin, payloadSize
          if (manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_SCHEMA,
              message = "Unsupported remote catalog schemaVersion=${manifest.schemaVersion}.",
            )
          }
          if (manifest.minAppVersionCode > appVersionCode) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_SCHEMA,
              message = "Catalog requires app versionCode >= ${manifest.minAppVersionCode} (current=$appVersionCode).",
            )
          }
          if (manifest.payloadSize <= 0L || manifest.payloadSize > CatalogSignatureVerifier.MAX_PAYLOAD_BYTES) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_SIZE,
              message =
                "Declared payloadSize=${manifest.payloadSize} exceeds ceiling " +
                  "(${CatalogSignatureVerifier.MAX_PAYLOAD_BYTES}B).",
            )
          }

          val payloadHttpUrl = manifest.payloadUrl.toHttpUrlOrNull()
          if (payloadHttpUrl == null || !payloadHttpUrl.scheme.equals("https", ignoreCase = true)) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_ORIGIN,
              message = "Payload URL must use https://.",
            )
          }
          if (!settings.allowCrossOriginRemotePayload && !isSameOrigin(manifestUrl, payloadHttpUrl)) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_ORIGIN,
              message = "Cross-origin payload (${payloadHttpUrl.host}) blocked unless explicitly enabled.",
            )
          }

          // 7. Anti-rollback matrix
          val currentSeq = existingMeta?.catalogSequence ?: 0L
          val currentDigest = existingMeta?.payloadSha256?.lowercase()
          val manifestDigest = manifest.sha256.lowercase()
          when {
            manifest.catalogSequence < currentSeq -> {
              return@withLock recordOutcome(
                existingMeta = existingMeta,
                sourceUrl = manifestUrlString,
                attemptEpochMs = clock(),
                status = SyncStatus.REJECTED_ROLLBACK,
                message = "Rejected older catalogSequence=${manifest.catalogSequence} (current=$currentSeq).",
              )
            }
            manifest.catalogSequence == currentSeq && currentSeq > 0L -> {
              if (currentDigest == manifestDigest) {
                val reusedCount = serverDao.countRemoteServers()
                val refreshed =
                  existingMeta.copy(
                    sourceUrl = manifestUrlString,
                    etag = manifestFetch.etag ?: existingMeta.etag,
                    lastModified = manifestFetch.lastModified ?: existingMeta.lastModified,
                    lastSyncAttemptAtEpochMs = clock(),
                    lastSyncStatus = SyncStatus.NOT_MODIFIED.name,
                    lastSyncMessage = "Catalog sequence $currentSeq digest unchanged.",
                  )
                metadataDao.upsertMetadata(refreshed)
                return@withLock RemoteSyncResult(
                  status = SyncStatus.NOT_MODIFIED,
                  catalogSequence = currentSeq,
                  entryCount = reusedCount,
                  message = refreshed.lastSyncMessage.orEmpty(),
                )
              } else {
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_CONFLICT,
                  message = "Digest conflict at equal catalogSequence=${manifest.catalogSequence}.",
                )
              }
            }
          }

          // 8. Download remote payload with bounded byte limit & origin check
          val payloadFetch =
            fetchBoundedBytes(
              initialUrl = payloadHttpUrl,
              maxBytes = CatalogSignatureVerifier.MAX_PAYLOAD_BYTES,
              etag = null,
              lastModified = null,
              allowCrossOrigin = settings.allowCrossOriginRemotePayload,
              originUrl = manifestUrl,
              activeCalls = activeCalls,
            )

          if (wifiLostMidFlight.get()) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.WIFI_REQUIRED,
              message = "Sync aborted mid-flight because verified Wi-Fi transport was lost.",
            )
          }

          val payloadBytes =
            when (payloadFetch) {
              is BoundedFetchResult.Success -> payloadFetch.bytes
              is BoundedFetchResult.SizeExceeded -> {
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SIZE,
                  message = payloadFetch.reason,
                )
              }
              is BoundedFetchResult.OriginBlocked -> {
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_ORIGIN,
                  message = payloadFetch.reason,
                )
              }
              is BoundedFetchResult.Error, BoundedFetchResult.NotModified -> {
                val reason =
                  (payloadFetch as? BoundedFetchResult.Error)?.reason
                    ?: "Payload download failed."
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.NETWORK_ERROR,
                  message = reason,
                )
              }
            }

          // Verify exact raw byte size and SHA-256 against manifest
          if (payloadBytes.size.toLong() != manifest.payloadSize) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_SIZE,
              message =
                "Downloaded payload size ${payloadBytes.size}B != manifest payloadSize " +
                  "${manifest.payloadSize}B.",
            )
          }

          val actualSha256 = CatalogSignatureVerifier.sha256Hex(payloadBytes)
          if (!actualSha256.equals(manifestDigest, ignoreCase = true)) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_SIGNATURE,
              message = "Payload SHA-256 mismatch against signed manifest.",
            )
          }

          val payloadText =
            CatalogSignatureVerifier
              .decodeStrictUtf8(payloadBytes)
              .getOrElse { err ->
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SCHEMA,
                  message = err.message ?: "Invalid UTF-8 payload.",
                )
              }

          val parsedPayload =
            parsePayloadJson(payloadText)
              .getOrElse { err ->
                return@withLock recordOutcome(
                  existingMeta = existingMeta,
                  sourceUrl = manifestUrlString,
                  attemptEpochMs = clock(),
                  status = SyncStatus.REJECTED_SCHEMA,
                  message = err.message ?: "Invalid catalog payload JSON.",
                )
              }

          if (parsedPayload.schemaVersion != manifest.schemaVersion ||
            parsedPayload.catalogSequence != manifest.catalogSequence
          ) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = clock(),
              status = SyncStatus.REJECTED_SCHEMA,
              message = "Payload header metadata does not match signed manifest.",
            )
          }

          val commitTime = clock()
          val remoteEntities =
            mapRemoteEntriesToEntities(
              entries = parsedPayload.entries,
              nowEpochMs = commitTime,
            ).getOrElse { err ->
              return@withLock recordOutcome(
                existingMeta = existingMeta,
                sourceUrl = manifestUrlString,
                attemptEpochMs = commitTime,
                status = SyncStatus.REJECTED_SCHEMA,
                message = err.message ?: "Invalid endpoint entry in remote payload.",
              )
            }

          // 9. Re-verify strict Wi-Fi before transactional DB commit
          val preCommitWifi =
            StrictWifiValidator.evaluate(
              snapshot = connectivityObserver.currentSnapshot(),
              wifiOnlyRequired = settings.wifiOnlyCatalogUpdates,
            )
          if (!preCommitWifi.isAllowed) {
            return@withLock recordOutcome(
              existingMeta = existingMeta,
              sourceUrl = manifestUrlString,
              attemptEpochMs = commitTime,
              status = SyncStatus.WIFI_REQUIRED,
              message = "Aborted commit: ${preCommitWifi.reason}",
            )
          }

          // 10. Atomic commit: leaves BUILT_IN, USER_CUSTOM, favorites, test_history untouched!
          val committedMeta =
            CatalogMetadataEntity(
              feedId = RoomCatalogRepository.REMOTE_FEED_ID,
              sourceUrl = manifestUrlString,
              keyId = manifest.keyId,
              catalogSequence = manifest.catalogSequence,
              schemaVersion = manifest.schemaVersion,
              minAppVersionCode = manifest.minAppVersionCode,
              payloadSha256 = actualSha256,
              payloadSizeBytes = payloadBytes.size.toLong(),
              etag = manifestFetch.etag,
              lastModified = manifestFetch.lastModified,
              generatedAtIso = manifest.generatedAt,
              publishedAtIso = manifest.publishedAt,
              lastSyncAttemptAtEpochMs = commitTime,
              lastSuccessfulSyncAtEpochMs = commitTime,
              lastSyncStatus = SyncStatus.UPDATED.name,
              lastSyncMessage =
                "Updated to sequence ${manifest.catalogSequence} " +
                  "(${remoteEntities.size} remote entries).",
              entryCount = remoteEntities.size,
            )

          database.withTransaction {
            serverDao.replaceRemoteCatalogAtomically(remoteEntities)
            metadataDao.upsertMetadata(committedMeta)
          }

          RemoteSyncResult(
            status = SyncStatus.UPDATED,
            catalogSequence = manifest.catalogSequence,
            entryCount = remoteEntities.size,
            message = committedMeta.lastSyncMessage.orEmpty(),
          )
        } catch (ce: CancellationException) {
          throw ce
        } finally {
          wifiMonitorJob?.cancel()
        }
      }
    }

  private suspend fun recordOutcome(
    existingMeta: CatalogMetadataEntity?,
    sourceUrl: String,
    attemptEpochMs: Long,
    status: SyncStatus,
    message: String,
  ): RemoteSyncResult {
    val base = existingMeta ?: emptyMetadata(sourceUrl)
    val updated =
      base.copy(
        sourceUrl = sourceUrl,
        lastSyncAttemptAtEpochMs = attemptEpochMs,
        lastSyncStatus = status.name,
        lastSyncMessage = message,
      )
    metadataDao.upsertMetadata(updated)
    return RemoteSyncResult(
      status = status,
      catalogSequence = updated.catalogSequence,
      entryCount = updated.entryCount,
      message = message,
    )
  }

  private fun emptyMetadata(sourceUrl: String): CatalogMetadataEntity =
    CatalogMetadataEntity(
      feedId = RoomCatalogRepository.REMOTE_FEED_ID,
      sourceUrl = sourceUrl,
      keyId = null,
      catalogSequence = 0L,
      schemaVersion = 0,
      minAppVersionCode = 0,
      payloadSha256 = null,
      payloadSizeBytes = null,
      etag = null,
      lastModified = null,
      generatedAtIso = null,
      publishedAtIso = null,
      lastSyncAttemptAtEpochMs = null,
      lastSuccessfulSyncAtEpochMs = null,
      lastSyncStatus = SyncStatus.IDLE.name,
      lastSyncMessage = null,
      entryCount = 0,
    )

  private sealed interface BoundedFetchResult {
    data class Success(
      val bytes: ByteArray,
      val etag: String?,
      val lastModified: String?,
    ) : BoundedFetchResult

    data object NotModified : BoundedFetchResult

    data class SizeExceeded(
      val reason: String,
    ) : BoundedFetchResult

    data class OriginBlocked(
      val reason: String,
    ) : BoundedFetchResult

    data class Error(
      val reason: String,
    ) : BoundedFetchResult
  }

  private fun fetchBoundedBytes(
    initialUrl: HttpUrl,
    maxBytes: Int,
    etag: String?,
    lastModified: String?,
    allowCrossOrigin: Boolean,
    originUrl: HttpUrl,
    activeCalls: MutableSet<Call>,
  ): BoundedFetchResult {
    var currentUrl = initialUrl
    var hops = 0

    while (true) {
      if (!currentUrl.scheme.equals("https", ignoreCase = true)) {
        return BoundedFetchResult.OriginBlocked("Refused non-HTTPS URL: ${currentUrl.scheme}")
      }
      if (!allowCrossOrigin && !isSameOrigin(originUrl, currentUrl)) {
        return BoundedFetchResult.OriginBlocked("Cross-origin URL blocked: ${currentUrl.host}")
      }

      val reqBuilder =
        Request
          .Builder()
          .url(currentUrl)
          .get()
          .header("Accept-Encoding", "identity")
          .header("User-Agent", "BDIX-Tester-CatalogSync/1.0")
      if (!etag.isNullOrBlank()) {
        reqBuilder.header("If-None-Match", etag)
      }
      if (!lastModified.isNullOrBlank()) {
        reqBuilder.header("If-Modified-Since", lastModified)
      }

      val call = client.newCall(reqBuilder.build())
      activeCalls.add(call)
      try {
        call.execute().use { response ->
          if (response.code == 304) {
            return BoundedFetchResult.NotModified
          }
          if (response.code in 300..399) {
            val location =
              response.header("Location")
                ?: return BoundedFetchResult.Error(
                  "Redirect HTTP ${response.code} missing Location header.",
                )
            val nextUrl =
              currentUrl.resolve(location)
                ?: return BoundedFetchResult.Error("Invalid redirect target.")
            if (!nextUrl.scheme.equals("https", ignoreCase = true)) {
              return BoundedFetchResult.OriginBlocked(
                "Blocked HTTPS-to-${nextUrl.scheme.uppercase()} redirect.",
              )
            }
            if (!allowCrossOrigin && !isSameOrigin(originUrl, nextUrl)) {
              return BoundedFetchResult.OriginBlocked(
                "Blocked cross-origin redirect to ${nextUrl.host}.",
              )
            }
            hops++
            if (hops > MAX_REDIRECTS) {
              return BoundedFetchResult.Error("Too many HTTP redirects.")
            }
            currentUrl = nextUrl
            return@use
          }
          if (!response.isSuccessful) {
            return BoundedFetchResult.Error("HTTP ${response.code} fetching ${currentUrl.encodedPath}.")
          }

          val body = response.body

          val declaredLength = body.contentLength()
          if (declaredLength > maxBytes.toLong()) {
            return BoundedFetchResult.SizeExceeded(
              "Declared Content-Length ${declaredLength}B exceeds ${maxBytes}B limit.",
            )
          }

          val out = ByteArrayOutputStream()
          val buffer = ByteArray(8192)
          var total = 0
          body.byteStream().use { stream ->
            while (true) {
              val read = stream.read(buffer)
              if (read == -1) break
              total += read
              if (total > maxBytes) {
                return BoundedFetchResult.SizeExceeded(
                  "Response stream exceeded ${maxBytes}B client limit.",
                )
              }
              out.write(buffer, 0, read)
            }
          }

          return BoundedFetchResult.Success(
            bytes = out.toByteArray(),
            etag = response.header("ETag"),
            lastModified = response.header("Last-Modified"),
          )
        }
      } catch (e: IOException) {
        return BoundedFetchResult.Error(e.message ?: "Network failure during catalog sync.")
      } finally {
        activeCalls.remove(call)
      }
    }
  }

  private fun mapRemoteEntriesToEntities(
    entries: List<RemoteCatalogEntryDto>,
    nowEpochMs: Long,
  ): Result<List<ServerEntity>> {
    val seenIds = mutableSetOf<String>()
    val result = ArrayList<ServerEntity>(entries.size)
    for (idx in entries.indices) {
      val dto = entries[idx]
      if (dto.id.isBlank() || !seenIds.add(dto.id)) {
        return Result.failure(IllegalArgumentException("Duplicate or blank remote entry id at index $idx."))
      }
      val parsed = UrlParser.parse(dto.url)
      if (!parsed.isValid || parsed.host == null || parsed.protocol == ServerProtocol.UNSUPPORTED) {
        return Result.failure(IllegalArgumentException("Invalid remote URL at entry ${dto.id}."))
      }
      val category =
        parseEnumOrNull<ServerCategory>(dto.category)
          ?.takeIf { it != ServerCategory.ALL }
          ?: ServerCategory.UNCATEGORIZED
      val verification =
        parseEnumOrNull<VerificationState>(dto.verificationState)
          ?: VerificationState.COMMUNITY_SUBMITTED
      val speedUrl = dto.speedTestUrl?.trim()?.takeIf { it.isNotEmpty() }
      if (speedUrl != null) {
        val parsedSpeed = UrlParser.parse(speedUrl)
        if (!parsedSpeed.isValid ||
          (parsedSpeed.protocol != ServerProtocol.HTTP && parsedSpeed.protocol != ServerProtocol.HTTPS)
        ) {
          return Result.failure(IllegalArgumentException("Invalid speedTestUrl at entry ${dto.id}."))
        }
      }

      result.add(
        ServerEntity(
          id = "remote:${RoomCatalogRepository.REMOTE_FEED_ID}:${dto.id}",
          sourceType = CatalogSourceType.REMOTE_CATALOG.name,
          sourceIndex = idx,
          feedId = RoomCatalogRepository.REMOTE_FEED_ID,
          remoteEntryId = dto.id,
          name =
            dto.name?.trim()?.takeIf { it.isNotEmpty() }
              ?: BuiltInCatalog.buildDisplayName(parsed.host, parsed.explicitPort),
          originalUrl = dto.url,
          host = parsed.host,
          explicitPort = parsed.explicitPort,
          protocol = parsed.protocol.name,
          category = category.name,
          isFeatured = dto.isFeatured,
          description = dto.description?.trim()?.takeIf { it.isNotEmpty() },
          sourceAttribution = "Signed Remote Catalog",
          verificationState = verification.name,
          lastReviewedAtEpochMs = nowEpochMs,
          speedTestUrl = speedUrl,
          speedTestMaxBytes = dto.speedTestMaxBytes,
          isPrivateIp = parsed.isPrivateIp,
          createdAtEpochMs = nowEpochMs,
          updatedAtEpochMs = nowEpochMs,
        ),
      )
    }
    return Result.success(result)
  }

  companion object {
    const val SUPPORTED_SCHEMA_VERSION = 1
    private const val MAX_REDIRECTS = 3
    private val SHA256_REGEX = Regex("^[0-9a-fA-F]{64}$")

    fun buildSignatureUrl(manifestUrl: HttpUrl): HttpUrl {
      val builder = manifestUrl.newBuilder()
      val segments = manifestUrl.pathSegments
      val lastIdx = segments.lastIndex
      if (lastIdx >= 0 && segments[lastIdx].isNotEmpty()) {
        builder.setPathSegment(lastIdx, "${segments[lastIdx]}.sig")
      } else {
        builder.addPathSegment("manifest.json.sig")
      }
      return builder.build()
    }

    fun isSameOrigin(
      a: HttpUrl,
      b: HttpUrl,
    ): Boolean =
      a.scheme.equals(b.scheme, ignoreCase = true) &&
        a.host.equals(b.host, ignoreCase = true) &&
        a.port == b.port

    fun parseManifestJson(rawJson: String): Result<SignedCatalogManifest> =
      runCatching {
        val obj = JSONObject(rawJson)
        val keyId = obj.getString("keyId").trim()
        val catalogSequence = obj.getLong("catalogSequence")
        val schemaVersion = obj.getInt("schemaVersion")
        val minAppVersionCode = obj.getInt("minAppVersionCode")
        val payloadUrl = obj.getString("payloadUrl").trim()
        val payloadSize = obj.getLong("payloadSize")
        val sha256 = obj.getString("sha256").trim()
        val generatedAt = obj.getString("generatedAt").trim()
        val publishedAt = obj.getString("publishedAt").trim()

        require(keyId.isNotEmpty()) { "keyId cannot be blank" }
        require(catalogSequence > 0L) { "catalogSequence must be > 0" }
        require(SHA256_REGEX.matches(sha256)) { "sha256 must be 64 hex chars" }
        require(generatedAt.isNotEmpty() && publishedAt.isNotEmpty()) { "Timestamps required" }

        SignedCatalogManifest(
          keyId = keyId,
          catalogSequence = catalogSequence,
          schemaVersion = schemaVersion,
          minAppVersionCode = minAppVersionCode,
          payloadUrl = payloadUrl,
          payloadSize = payloadSize,
          sha256 = sha256.lowercase(),
          generatedAt = generatedAt,
          publishedAt = publishedAt,
        )
      }

    fun parsePayloadJson(rawJson: String): Result<RemoteCatalogPayload> =
      runCatching {
        val obj = JSONObject(rawJson)
        val schemaVersion = obj.getInt("schemaVersion")
        val catalogSequence = obj.getLong("catalogSequence")
        val arr = obj.getJSONArray("entries")
        val entries = ArrayList<RemoteCatalogEntryDto>(arr.length())
        for (i in 0 until arr.length()) {
          val item = arr.getJSONObject(i)
          entries.add(
            RemoteCatalogEntryDto(
              id = item.getString("id").trim(),
              name = item.optString("name", "").takeIf { it.isNotBlank() },
              url = item.getString("url"),
              category = item.optString("category", "").takeIf { it.isNotBlank() },
              isFeatured = item.optBoolean("isFeatured", false),
              verificationState = item.optString("verificationState", "").takeIf { it.isNotBlank() },
              description = item.optString("description", "").takeIf { it.isNotBlank() },
              speedTestUrl = item.optString("speedTestUrl", "").takeIf { it.isNotBlank() },
              speedTestMaxBytes =
                if (item.has("speedTestMaxBytes") && !item.isNull("speedTestMaxBytes")) {
                  item.getLong("speedTestMaxBytes")
                } else {
                  null
                },
            ),
          )
        }
        RemoteCatalogPayload(
          schemaVersion = schemaVersion,
          catalogSequence = catalogSequence,
          entries = entries,
        )
      }

    private inline fun <reified T : Enum<T>> parseEnumOrNull(name: String?): T? {
      if (name.isNullOrBlank()) return null
      return enumValues<T>().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
    }
  }
}
