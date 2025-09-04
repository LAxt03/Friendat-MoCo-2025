package com.friendat.utils

import android.Manifest
import android.annotation.SuppressLint // Nötig für @SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

object WifiUtils {

    private const val TAG = "WifiUtils"

    // Hilfsfunktion, um eine Liste von Strings für Transporttypen zu bekommen
    private fun getTransportTypes(capabilities: NetworkCapabilities): List<String> {
        val types = mutableListOf<String>()
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) types.add("WIFI")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) types.add("CELLULAR")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) types.add("BLUETOOTH")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) types.add("ETHERNET")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) types.add("VPN")
        // Weitere bei Bedarf hinzufügen
        return types
    }

    // Hilfsfunktion, um eine Liste von Strings für Capabilities zu bekommen (vereinfacht)
    private fun getCaps(capabilities: NetworkCapabilities): List<String> {
        val caps = mutableListOf<String>()
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) caps.add("INTERNET")
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) caps.add("VALIDATED")
        // Weitere wichtige bei Bedarf hinzufügen
        return caps
    }

    @SuppressLint("WrongConstant") // Unterdrückt Lint-Warnung für numerische Capability-Werte
    fun getCurrentBssid(context: Context): String? {
        Log.d(TAG, "[${System.currentTimeMillis()}] getCurrentBssid CALLED")

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "ACCESS_FINE_LOCATION permission NOT GRANTED.")
            return null
        }
        Log.d(TAG, "ACCESS_FINE_LOCATION permission GRANTED.")

        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is NULL.")
            return null
        }

        try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                Log.w(TAG, "ConnectivityManager.activeNetwork is NULL.")
                return null
            }
            Log.d(TAG, "Active Network: $activeNetwork")

            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities == null) {
                Log.w(TAG, "NetworkCapabilities for active network are NULL.")
                return null
            }

            val transportTypes = getTransportTypes(networkCapabilities)
            val caps = getCaps(networkCapabilities)
            Log.d(TAG, "Active Network Caps: $caps, Transports: $transportTypes")

            if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.w(TAG, "Active network is NOT WiFi. Actual transports: $transportTypes")
                return null
            }
            Log.d(TAG, "Active network IS WiFi.")

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager == null) {
                Log.e(TAG, "WifiManager service is NULL.")
                return null
            }

            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WifiManager reports WiFi is NOT enabled.")
                return null
            }
            Log.d(TAG, "WifiManager reports WiFi is ENABLED.")

            Log.d(TAG, "[${System.currentTimeMillis()}] Attempting to get WifiManager.connectionInfo...")
            val connectionInfo: WifiInfo? = wifiManager.connectionInfo

            if (connectionInfo == null) {
                Log.w(TAG, "[${System.currentTimeMillis()}] WifiManager.connectionInfo is NULL.")
                return null
            }

            val rawBssid = connectionInfo.bssid
            val ssid = connectionInfo.ssid
            val supplicantState = connectionInfo.supplicantState
            val rssi = connectionInfo.rssi

            Log.d(TAG, "[${System.currentTimeMillis()}] WifiInfo NOT NULL: SSID='${ssid}', BSSID='${rawBssid}', SupplicantState='${supplicantState}', RSSI='${rssi}'")

            // Die numerischen Werte, falls compileSdk zu niedrig ist.
            val bssidPlaceholder1 = "02:00:00:00:00:00"
            val bssidPlaceholder2 = "00:00:00:00:00:00"
            val unknownSsidPlaceholder = "<unknown ssid>" // eher für SSID, aber als Check
            val unknownBssidPlaceholder = "<unknown bssid>"


            if (rawBssid != null &&
                rawBssid.isNotEmpty() &&
                !rawBssid.equals(bssidPlaceholder1, ignoreCase = true) &&
                !rawBssid.equals(bssidPlaceholder2, ignoreCase = true) &&
                !rawBssid.equals(unknownSsidPlaceholder, ignoreCase = true) &&
                !rawBssid.equals(unknownBssidPlaceholder, ignoreCase = true)
            ) {
                Log.i(TAG, "VALID BSSID found: '$rawBssid'")
                return rawBssid
            } else {
                Log.w(TAG, "INVALID or PLACEHOLDER BSSID detected from WifiInfo. Raw BSSID='${rawBssid}', SSID='${ssid}'.")
                return null
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in getCurrentBssid: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Generic Exception in getCurrentBssid: ${e.message}", e)
            return null
        } finally {
            Log.d(TAG, "[${System.currentTimeMillis()}] getCurrentBssid FINISHED")
        }
    }
}
