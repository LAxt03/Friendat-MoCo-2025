package com.friendat.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo // Importieren
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat

object WifiUtils {

    private const val TAG = "WifiUtils"

    fun getCurrentBssid(context: Context): String? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "ACCESS_FINE_LOCATION permission not granted. Cannot get BSSID.")
            return null
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null.")
            return null
        }

        var finalBssid: String? = null // Umbenannt für Klarheit

        try {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                Log.d(TAG, "No active network.")
                return null
            }

            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities == null || !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.d(TAG, "Active network is not WiFi or capabilities are null.")
                return null
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager == null) {
                Log.e(TAG, "WifiManager is null.")
                return null
            }
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi is not enabled.")
                return null
            }

            val connectionInfo: WifiInfo? = wifiManager.connectionInfo
            if (connectionInfo == null) {
                Log.w(TAG, "WifiInfo is null (connectionInfo).")
                return null
            }

            val rawBssid = connectionInfo.bssid
            Log.d(TAG, "Raw BSSID from WifiInfo: '$rawBssid'")

            if (rawBssid != null &&
                rawBssid.isNotEmpty() &&
                rawBssid != "02:00:00:00:00:00" &&
                rawBssid != "00:00:00:00:00:00" &&
                !rawBssid.equals("<unknown ssid>", ignoreCase = true) && // Vorsichtshalber, obwohl für BSSID unüblich
                !rawBssid.equals("<unknown bssid>", ignoreCase = true) // Sicherstellen, dass wir auch diesen Fall abfangen
            ) {
                finalBssid = rawBssid
            } else {
                Log.w(TAG, "BSSID is null, empty, a placeholder, or <unknown>. Raw value: '$rawBssid'. This can happen if location services are off or permissions are insufficient despite the check.")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while getting BSSID: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception while getting BSSID: ${e.message}")
        }

        Log.d(TAG, "Returning BSSID: $finalBssid")
        return finalBssid
    }
}
