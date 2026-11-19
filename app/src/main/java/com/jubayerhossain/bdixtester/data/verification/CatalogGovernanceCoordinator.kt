package com.jubayerhossain.bdixtester.data.verification

import com.jubayerhossain.bdixtester.core.model.BdIspProbeRecord
import com.jubayerhossain.bdixtester.core.model.DnsEvidence
import com.jubayerhossain.bdixtester.core.model.FtpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.GovernanceDecision
import com.jubayerhossain.bdixtester.core.model.HttpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.PriorGovernanceObservation
import com.jubayerhossain.bdixtester.core.model.RegistrationEvidence
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.core.model.VerificationRunProgress
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.data.network.EndpointProbeConfig
import com.jubayerhossain.bdixtester.data.network.EndpointProbeExecutor
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CatalogGovernanceCoordinator(
  private val catalogRepository: CatalogRepository,
  private val probeExecutor: EndpointProbeExecutor,
  private val dnsResolver: DohDnsResolver,
  private val rdapClient: RdapLookupClient,
  private val clock: () -> Long = { System.currentTimeMillis() },
) {
  private val runMutex = Mutex()
  private val importedIspProbes = ConcurrentHashMap<String, List<BdIspProbeRecord>>()

  fun importIspProbeJson(
    rawJson: String,
    freshnessTtlHours: Int = 168,
  ): Result<IspProbeLoadResult> {
    val parsed = IspProbeJsonParser.parseJson(rawJson, freshnessTtlHours, clock())
    parsed.onSuccess { loadResult ->
      loadResult.probesByCanonicalUrl.forEach { (key, list) ->
        importedIspProbes[key] = list
      }
    }
    return parsed
  }

  suspend fun verifySingleEndpoint(
    serverId: String,
    triggerSource: String = "manual_single",
  ): GovernanceDecision? =
    withContext(Dispatchers.IO) {
      val endpoint = catalogRepository.getEndpointById(serverId) ?: return@withContext null
      val existingRecord = catalogRepository.governanceRecords.value[serverId]
      val prior =
        existingRecord?.lastVerifiedAtEpochMs?.let { ts ->
          PriorGovernanceObservation(
            status = existingRecord.lifecycleStatus,
            checkedAtEpochMs = ts,
            dnsSummary = existingRecord.dnsSummary,
            registrationStatus = existingRecord.registrationStatus,
          )
        }
      val decision =
        evaluateSingle(
          endpoint = endpoint,
          dnsCache = mutableMapOf(),
          rdapCache = mutableMapOf(),
          prior = prior,
          currentLifecycle = existingRecord?.lifecycleStatus,
        )
      catalogRepository.applyGovernanceDecision(decision, triggerSource)
      decision
    }

  suspend fun verifyBatch(
    maxEndpoints: Int = DEFAULT_BATCH_LIMIT,
    triggerSource: String = "manual_batch",
    onStepProgress: suspend (processed: Int, total: Int, currentName: String?) -> Unit = { _, _, _ -> },
  ): Result<List<GovernanceDecision>> =
    runMutex.withLock {
      withContext(Dispatchers.IO) {
        try {
          catalogRepository.ensureInitialized()
          val all = catalogRepository.allCatalogEndpoints.value
          val govMap = catalogRepository.governanceRecords.value

          // Prioritize non-archived endpoints that are oldest / pending verification
          val targets =
            all
              .asSequence()
              .filter { ep ->
                govMap[ep.id]?.lifecycleStatus != VerificationLifecycleStatus.ARCHIVED
              }.sortedBy { ep -> govMap[ep.id]?.lastVerifiedAtEpochMs ?: 0L }
              .take(maxEndpoints.coerceAtLeast(1))
              .toList()

          catalogRepository.updateVerificationProgress(
            VerificationRunProgress(
              isRunning = true,
              processedCount = 0,
              totalCount = targets.size,
              currentEndpointName = targets.firstOrNull()?.name,
              candidatesDetected = 0,
            ),
          )

          val dnsCache = mutableMapOf<String, DnsEvidence>()
          val rdapCache = mutableMapOf<String, RegistrationEvidence>()
          val results = mutableListOf<GovernanceDecision>()
          var candidateCount = 0

          for ((index, ep) in targets.withIndex()) {
            val existing = govMap[ep.id]
            val prior =
              existing?.lastVerifiedAtEpochMs?.let { ts ->
                PriorGovernanceObservation(
                  status = existing.lifecycleStatus,
                  checkedAtEpochMs = ts,
                  dnsSummary = existing.dnsSummary,
                  registrationStatus = existing.registrationStatus,
                )
              }
            val decision =
              evaluateSingle(
                endpoint = ep,
                dnsCache = dnsCache,
                rdapCache = rdapCache,
                prior = prior,
                currentLifecycle = existing?.lifecycleStatus,
              )
            results += decision
            if (decision.status.isRemovalCandidateOrConfirmed) {
              candidateCount++
            }
            val processed = index + 1
            catalogRepository.updateVerificationProgress(
              VerificationRunProgress(
                isRunning = true,
                processedCount = processed,
                totalCount = targets.size,
                currentEndpointName = ep.name,
                candidatesDetected = candidateCount,
              ),
            )
            onStepProgress(processed, targets.size, ep.name)
          }

          if (results.isNotEmpty()) {
            catalogRepository.applyGovernanceDecisions(results, triggerSource)
          }

          catalogRepository.updateVerificationProgress(
            VerificationRunProgress(
              isRunning = false,
              processedCount = targets.size,
              totalCount = targets.size,
              currentEndpointName = null,
              candidatesDetected = candidateCount,
              lastCompletedAtEpochMs = clock(),
            ),
          )
          Result.success(results)
        } catch (ce: CancellationException) {
          catalogRepository.updateVerificationProgress(
            catalogRepository.verificationRunProgress.value.copy(isRunning = false),
          )
          throw ce
        } catch (e: Exception) {
          catalogRepository.updateVerificationProgress(
            catalogRepository.verificationRunProgress.value.copy(
              isRunning = false,
              lastErrorMessage = e.message ?: "Verification batch failed.",
            ),
          )
          Result.failure(e)
        }
      }
    }

  private suspend fun evaluateSingle(
    endpoint: ServerEndpoint,
    dnsCache: MutableMap<String, DnsEvidence>,
    rdapCache: MutableMap<String, RegistrationEvidence>,
    prior: PriorGovernanceObservation?,
    currentLifecycle: VerificationLifecycleStatus?,
  ): GovernanceDecision {
    val domain = DomainExtractor.extractNormalizedDomain(endpoint.host)
    val canonical = UrlParser.canonicalKey(endpoint.originalUrl)
    val ispProbes = importedIspProbes[canonical].orEmpty()

    if (endpoint.isPrivateIp ||
      endpoint.protocol == ServerProtocol.UNSUPPORTED ||
      endpoint.protocol == ServerProtocol.FTPS
    ) {
      return ConservativeDecisionEngine.evaluate(
        endpoint = endpoint,
        ispProbes = ispProbes,
        prior = prior,
        currentLifecycleOverride = currentLifecycle,
        nowEpochMs = clock(),
      )
    }

    val dns: DnsEvidence? =
      if (domain != null) {
        dnsCache[domain] ?: dnsResolver.lookupDomain(domain).also { dnsCache[domain] = it }
      } else {
        null
      }

    val registration: RegistrationEvidence? =
      if (domain != null) {
        rdapCache[domain] ?: rdapClient.lookupDomain(domain).also { rdapCache[domain] = it }
      } else {
        null
      }

    val probeConfig =
      EndpointProbeConfig(
        scanMode = ScanMode.QUICK_SCAN,
        connectTimeoutMs = 2_500,
        readTimeoutMs = 2_500,
        overallDeadlineMs = 3_500,
      )
    val probeResult = probeExecutor.probe(endpoint, probeConfig)

    var httpEvidence: HttpProbeEvidence? = null
    var ftpEvidence: FtpProbeEvidence? = null
    when (endpoint.protocol) {
      ServerProtocol.HTTP, ServerProtocol.HTTPS -> {
        httpEvidence = ConservativeDecisionEngine.toHttpEvidence(probeResult, clock())
      }
      ServerProtocol.FTP, ServerProtocol.FTPS -> {
        ftpEvidence = ConservativeDecisionEngine.toFtpEvidence(probeResult, clock())
      }
      ServerProtocol.UNSUPPORTED -> Unit
    }

    return ConservativeDecisionEngine.evaluate(
      endpoint = endpoint,
      dns = dns,
      registration = registration,
      http = httpEvidence,
      ftp = ftpEvidence,
      ispProbes = ispProbes,
      prior = prior,
      currentLifecycleOverride = currentLifecycle,
      nowEpochMs = clock(),
    )
  }

  companion object {
    const val DEFAULT_BATCH_LIMIT = 60
  }
}
