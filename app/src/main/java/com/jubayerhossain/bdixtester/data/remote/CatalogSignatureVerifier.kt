package com.jubayerhossain.bdixtester.data.remote

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

enum class TrustedKeyStatus {
  ACTIVE,
  ROTATING,
  REVOKED,
}

data class TrustedPublicKeyEntry(
  val keyId: String,
  val x509SpkiBase64: String,
  val status: TrustedKeyStatus,
  val description: String = "",
)

class TrustedKeyRegistry(
  entries: List<TrustedPublicKeyEntry> = DEFAULT_KEYS,
) {
  private val keyMap: Map<String, TrustedPublicKeyEntry> = entries.associateBy { it.keyId }

  fun findKey(keyId: String): TrustedPublicKeyEntry? = keyMap[keyId]

  fun decodePublicKey(entry: TrustedPublicKeyEntry): PublicKey {
    val spkiBytes =
      decodeBase64Flexible(entry.x509SpkiBase64)
        ?: throw IllegalArgumentException("Invalid Base64 SPKI for keyId=${entry.keyId}")
    val spec = X509EncodedKeySpec(spkiBytes)
    return KeyFactory.getInstance("EC").generatePublic(spec)
  }

  companion object {
    /**
     * Trusted P-256 ECDSA public keys for signed remote catalog manifests.
     * Key rotation works by shipping an upcoming `ROTATING` key ahead of cutover,
     * switching to `ACTIVE`, and moving retired or compromised keys to `REVOKED`.
     *
     * Note: No private signing key is ever shipped in the Android application.
     */
    val DEFAULT_KEYS: List<TrustedPublicKeyEntry> =
      listOf(
        TrustedPublicKeyEntry(
          keyId = "bdix-catalog-2026-v1",
          // P-256 X.509 SubjectPublicKeyInfo (public verification key only)
          x509SpkiBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEf9qK8pV3aF1a6C+hJ8jS9L0fJ3kO5v2x+V6Q7e1F2W4pN1xJ9z8k6Q5L3R7P4u2m1N8w3E5R7T9Y2U4I6O8P1A==",
          status = TrustedKeyStatus.ACTIVE,
          description = "Primary ECDSA P-256 catalog manifest verification key (2026)",
        ),
      )
  }
}

sealed interface SignatureVerificationResult {
  data class Verified(
    val keyId: String,
    val rawManifestUtf8: String,
  ) : SignatureVerificationResult

  data class Rejected(
    val reason: String,
  ) : SignatureVerificationResult
}

/**
 * Verifies detached ECDSA P-256 (SHA256withECDSA) manifest signatures directly
 * against the exact raw UTF-8 manifest bytes before any JSON reserialization.
 */
class CatalogSignatureVerifier(
  private val keyRegistry: TrustedKeyRegistry = TrustedKeyRegistry(),
) {
  fun verifyDetachedManifest(
    rawManifestBytes: ByteArray,
    rawSignatureBytes: ByteArray,
  ): SignatureVerificationResult {
    if (rawManifestBytes.isEmpty()) {
      return SignatureVerificationResult.Rejected("Manifest payload is empty.")
    }
    if (rawManifestBytes.size > MAX_MANIFEST_BYTES) {
      return SignatureVerificationResult.Rejected(
        "Manifest exceeds ${MAX_MANIFEST_BYTES}B limit (${rawManifestBytes.size}B).",
      )
    }
    if (rawSignatureBytes.isEmpty()) {
      return SignatureVerificationResult.Rejected("Detached signature payload is empty.")
    }
    if (rawSignatureBytes.size > MAX_SIGNATURE_BYTES) {
      return SignatureVerificationResult.Rejected(
        "Detached signature exceeds ${MAX_SIGNATURE_BYTES}B limit (${rawSignatureBytes.size}B).",
      )
    }

    val manifestText =
      decodeStrictUtf8(rawManifestBytes)
        .getOrElse { error ->
          return SignatureVerificationResult.Rejected(error.message ?: "Invalid UTF-8 manifest.")
        }

    val keyId =
      extractUntrustedKeyId(manifestText)
        ?: return SignatureVerificationResult.Rejected("Manifest missing required string keyId field.")

    val keyEntry =
      keyRegistry.findKey(keyId)
        ?: return SignatureVerificationResult.Rejected("Untrusted catalog signing keyId: $keyId.")

    if (keyEntry.status == TrustedKeyStatus.REVOKED) {
      return SignatureVerificationResult.Rejected("Catalog signing keyId '$keyId' is revoked.")
    }

    val derSignature =
      parseDetachedSignatureBytes(rawSignatureBytes)
        ?: return SignatureVerificationResult.Rejected("Malformed detached ECDSA signature bytes.")

    return try {
      val publicKey = keyRegistry.decodePublicKey(keyEntry)
      val signature = Signature.getInstance("SHA256withECDSA")
      signature.initVerify(publicKey)
      signature.update(rawManifestBytes)
      if (signature.verify(derSignature)) {
        SignatureVerificationResult.Verified(
          keyId = keyId,
          rawManifestUtf8 = manifestText,
        )
      } else {
        SignatureVerificationResult.Rejected(
          "ECDSA signature verification failed against exact manifest bytes.",
        )
      }
    } catch (e: Exception) {
      SignatureVerificationResult.Rejected(
        "Cryptographic signature verification error: ${e.message ?: e::class.java.simpleName}",
      )
    }
  }

  companion object {
    const val MAX_MANIFEST_BYTES = 64 * 1024 // 64 KiB
    const val MAX_SIGNATURE_BYTES = 512 // 512 bytes
    const val MAX_PAYLOAD_BYTES = 2 * 1024 * 1024 // 2 MiB

    private val KEY_ID_REGEX = Regex("""\"keyId\"\s*:\s*\"([^\"\\]+)\"""")

    fun decodeStrictUtf8(bytes: ByteArray): Result<String> {
      if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() &&
        bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
      ) {
        return Result.failure(
          IllegalArgumentException("UTF-8 BOM is prohibited in signed manifests and payloads."),
        )
      }
      return try {
        val decoder =
          Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
        if (decoded.startsWith('\uFEFF')) {
          Result.failure(
            IllegalArgumentException("UTF-8 BOM is prohibited in signed manifests and payloads."),
          )
        } else {
          Result.success(decoded)
        }
      } catch (e: Exception) {
        Result.failure(IllegalArgumentException("Malformed UTF-8 byte sequence: ${e.message}"))
      }
    }

    fun extractUntrustedKeyId(manifestText: String): String? =
      KEY_ID_REGEX.find(manifestText)?.groupValues?.getOrNull(1)?.trim()?.takeIf {
        it.isNotEmpty()
      }

    fun parseDetachedSignatureBytes(rawSignatureBytes: ByteArray): ByteArray? {
      // Support either Base64-encoded ASCII text signature file or raw ASN.1 DER bytes.
      val asAscii = rawSignatureBytes.toString(Charsets.UTF_8).trim()
      if (asAscii.isNotEmpty() &&
        asAscii.all { it.isLetterOrDigit() || it in charArrayOf('+', '/', '=', '-', '_') }
      ) {
        decodeBase64Flexible(asAscii)?.let { decoded ->
          if (decoded.isNotEmpty()) return decoded
        }
      }
      return if (rawSignatureBytes.size >= 8 && rawSignatureBytes[0] == 0x30.toByte()) {
        rawSignatureBytes
      } else {
        null
      }
    }

    fun sha256Hex(bytes: ByteArray): String {
      val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
      return digest.joinToString("") { "%02x".format(it) }
    }
  }
}

private fun decodeBase64Flexible(input: String): ByteArray? {
  val trimmed = input.trim()
  if (trimmed.isEmpty()) return null
  return try {
    Base64.decode(trimmed, Base64.DEFAULT)
  } catch (_: IllegalArgumentException) {
    try {
      Base64.decode(trimmed, Base64.URL_SAFE or Base64.NO_WRAP)
    } catch (_: IllegalArgumentException) {
      null
    }
  }
}
