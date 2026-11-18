package com.jubayerhossain.bdixtester.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.Inet4Address
import java.net.NetworkInterface

enum class TransportType(
  val displayLabel: String,
) {
  WIFI("Wi-Fi"),
  CELLULAR("Mobile Data"),
  ETHERNET("Ethernet"),
  VPN("VPN"),
  OTHER("Active Network"),
  NONE("Offline"),
}

data class NetworkSnapshot(
  val isConnected: Boolean = false,
  val transportType: TransportType = TransportType.NONE,
  val hasWifiTransport: Boolean = false,
  val hasVpnTransport: Boolean = false,
  val localIpAddress: String? = null,
) {
  /**
   * Strict Wi-Fi verification for catalog synchronization:
   * requires active internet capability AND confirmed `TRANSPORT_WIFI`.
   * Accepts `TRANSPORT_WIFI + TRANSPORT_VPN` when underlying Wi-Fi is confirmed,
   * and rejects VPN-only ambiguous transports.
   */
  val isStrictWifiVerified: Boolean
    get() = isConnected && hasWifiTransport
}

data class WifiValidationResult(
  val isAllowed: Boolean,
  val reason: String,
  val snapshot: NetworkSnapshot,
)

object StrictWifiValidator {
  fun evaluate(
    snapshot: NetworkSnapshot,
    wifiOnlyRequired: Boolean,
  ): WifiValidationResult {
    if (!snapshot.isConnected) {
      return WifiValidationResult(
        isAllowed = false,
        reason = "No active internet connection detected.",
        snapshot = snapshot,
      )
    }
    if (!wifiOnlyRequired) {
      return WifiValidationResult(
        isAllowed = true,
        reason = "Active network available (Wi-Fi-only restriction disabled).",
        snapshot = snapshot,
      )
    }
    return if (snapshot.isStrictWifiVerified) {
      val detail =
        if (snapshot.hasVpnTransport) {
          "Verified Wi-Fi transport with VPN active."
        } else {
          "Verified Wi-Fi transport."
        }
      WifiValidationResult(isAllowed = true, reason = detail, snapshot = snapshot)
    } else if (snapshot.hasVpnTransport) {
      WifiValidationResult(
        isAllowed = false,
        reason = "VPN-only transport without confirmed underlying Wi-Fi; strict Wi-Fi sync blocked.",
        snapshot = snapshot,
      )
    } else {
      WifiValidationResult(
        isAllowed = false,
        reason = "Active transport is ${snapshot.transportType.displayLabel}; strict Wi-Fi sync requires Wi-Fi.",
        snapshot = snapshot,
      )
    }
  }
}

interface ConnectivityObserver {
  fun observeConnectivity(): Flow<NetworkSnapshot>

  fun currentSnapshot(): NetworkSnapshot
}

/**
 * Privacy-conscious connectivity observer using [ConnectivityManager] without
 * reading Wi-Fi SSID, BSSID, MAC addresses, or requesting location permissions.
 */
class AndroidConnectivityObserver(
  context: Context,
) : ConnectivityObserver {
  private val connectivityManager =
    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  override fun observeConnectivity(): Flow<NetworkSnapshot> =
    callbackFlow {
      trySend(currentSnapshot())
      val cm = connectivityManager
      if (cm == null) {
        awaitClose { }
        return@callbackFlow
      }

      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            trySend(currentSnapshot())
          }

          override fun onLost(network: Network) {
            trySend(currentSnapshot())
          }

          override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
          ) {
            trySend(snapshotFromCapabilities(networkCapabilities, resolveLocalIpv4(cm, network)))
          }
        }

      try {
        cm.registerDefaultNetworkCallback(callback)
      } catch (_: SecurityException) {
        trySend(NetworkSnapshot())
      }

      awaitClose {
        try {
          cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
          // Ignore if already unregistered
        }
      }
    }.distinctUntilChanged()

  override fun currentSnapshot(): NetworkSnapshot {
    val cm = connectivityManager ?: return NetworkSnapshot()
    return try {
      val activeNetwork = cm.activeNetwork ?: return NetworkSnapshot()
      val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkSnapshot()
      snapshotFromCapabilities(caps, resolveLocalIpv4(cm, activeNetwork))
    } catch (_: SecurityException) {
      NetworkSnapshot()
    }
  }

  internal fun snapshotFromCapabilities(
    caps: NetworkCapabilities,
    localIpAddress: String? = null,
  ): NetworkSnapshot {
    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    if (!hasInternet) {
      return NetworkSnapshot()
    }
    val hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    val transport =
      when {
        hasVpn && hasWifi -> TransportType.WIFI
        hasVpn -> TransportType.VPN
        hasWifi -> TransportType.WIFI
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELLULAR
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TransportType.ETHERNET
        else -> TransportType.OTHER
      }
    return NetworkSnapshot(
      isConnected = true,
      transportType = transport,
      hasWifiTransport = hasWifi,
      hasVpnTransport = hasVpn,
      localIpAddress = localIpAddress,
    )
  }

  private fun resolveLocalIpv4(
    cm: ConnectivityManager,
    network: Network?,
  ): String? {
    try {
      if (network != null) {
        val linkProps = cm.getLinkProperties(network)
        val linkIp =
          linkProps
            ?.linkAddresses
            ?.mapNotNull { it.address as? Inet4Address }
            ?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            ?.hostAddress
        if (!linkIp.isNullOrBlank()) return linkIp
      }
    } catch (_: Exception) {
      // Fall through to NetworkInterface enumeration
    }
    return try {
      val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
      while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (!iface.isUp || iface.isLoopback) continue
        val addresses = iface.inetAddresses
        while (addresses.hasMoreElements()) {
          val addr = addresses.nextElement()
          if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
            val hostAddr = addr.hostAddress
            if (!hostAddr.isNullOrBlank()) return hostAddr
          }
        }
      }
      null
    } catch (_: Exception) {
      null
    }
  }
}
