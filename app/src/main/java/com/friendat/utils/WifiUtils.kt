package com.friendat.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build // Nicht direkt verwendet in getCurrentBssid, aber oft in WifiUtils
import android.util.Log
import androidx.core.content.ContextCompat

object WifiUtils {

    private const val TAG = "WifiUtils"

    // --- FEHLENDE HILFSFUNKTIONEN HINZUGEFÜGT ---
    private fun getTransportTypes(capabilities: NetworkCapabilities): List<String> {
        val types = mutableListOf<String>()
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) types.add("WIFI")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) types.add("CELLULAR")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) types.add("BLUETOOTH")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) types.add("ETHERNET")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) types.add("VPN")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // TRANSPORT_WIFI_AWARE ab API 27
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) types.add("WIFI_AWARE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // TRANSPORT_LOWPAN ab API 26
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) types.add("LOWPAN")
        }
        return types
    }

    private fun getCaps(capabilities: NetworkCapabilities): List<String> {
        val caps = mutableListOf<String>()
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) caps.add("INTERNET")
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) caps.add("VALIDATED")
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) caps.add("NOT_METERED")
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) caps.add("NOT_VPN")
        // Weitere wichtige bei Bedarf hinzufügen
        return caps
    }
    // --- ENDE FEHLENDE HILFSFUNKTIONEN ---


    @SuppressLint("WrongConstant", "MissingPermission")
    fun getCurrentBssid(context: Context): String? {
        Log.d(TAG, "[${System.currentTimeMillis()}] getCurrentBssid CALLED")
        FileLogger.logWifiUtils(context.applicationContext, TAG, "[${System.currentTimeMillis()}] getCurrentBssid CALLED")
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            FileLogger.logWifiUtils(context.applicationContext, TAG, "ACCESS_FINE_LOCATION permission NOT GRANTED.")
            Log.e(TAG, "ACCESS_FINE_LOCATION permission NOT GRANTED.")
            return null
        }
        FileLogger.logWifiUtils(context.applicationContext, TAG, "ACCESS_FINE_LOCATION permission GRANTED.")
        Log.d(TAG, "ACCESS_FINE_LOCATION permission GRANTED.")

        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            FileLogger.logWifiUtils(context.applicationContext, TAG, "ConnectivityManager is NULL.")
            Log.e(TAG, "ConnectivityManager is NULL.")
            return null
        }

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            FileLogger.logWifiUtils(context.applicationContext, TAG, "WifiManager service is NULL.")

            Log.e(TAG, "WifiManager service is NULL.")
            return null
        }

        if (!wifiManager.isWifiEnabled) {
            FileLogger.logWifiUtils(context.applicationContext, TAG, "WifiManager reports Wifi is NOT enabled.")

            Log.w(TAG, "WifiManager reports WiFi is NOT enabled.")
            return null
        }
        FileLogger.logWifiUtils(context.applicationContext, TAG, "WifiManager reports WiFi is ENABLED")

        Log.d(TAG, "WifiManager reports WiFi is ENABLED.")

        // VERSUCH 1: Über ConnectivityManager.activeNetwork (bevorzugt, wenn es funktioniert)
        try {
            val activeNetwork: Network? = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                FileLogger.logWifiUtils(context.applicationContext, TAG, "Active Network (from CM): $activeNetwork")
                Log.d(TAG, "Active Network (from CM): $activeNetwork")
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    FileLogger.logWifiUtils(context.applicationContext, TAG, "Active network IS WiFi (confirmed by CM). Attempting WifiInfo from WifiManager. Caps: ${getCaps(networkCapabilities)}")
                    Log.d(TAG, "Active network IS WiFi (confirmed by CM). Attempting WifiInfo from WifiManager. Caps: ${getCaps(networkCapabilities)}")
                    val connectionInfo: WifiInfo? = wifiManager.connectionInfo
                    if (connectionInfo != null) {
                        val rawBssid = connectionInfo.bssid
                        val ssid = connectionInfo.ssid
                        // Log.d(TAG, "[CM Path] WifiInfo: SSID='${ssid}', BSSID='${rawBssid}', SupplicantState='${connectionInfo.supplicantState}'")
                        if (isValidBssid(rawBssid, ssid)) {
                            Log.i(TAG, "[CM Path] VALID BSSID found: '$rawBssid'")
                            return rawBssid
                        } else {
                            Log.w(TAG, "[CM Path] INVALID or PLACEHOLDER BSSID detected. Raw BSSID='${rawBssid}', SSID='${ssid}'.")
                        }
                    } else {
                        Log.w(TAG, "[CM Path] wifiManager.connectionInfo is NULL despite active WiFi network from CM.")
                    }
                } else {

                    Log.w(TAG, "Active network (from CM) is NOT WiFi or no capabilities. Transports: ${networkCapabilities?.let { getTransportTypes(it) } ?: "null caps"}")
                }
            } else {
                FileLogger.logWifiUtils(context.applicationContext, TAG, "ConnectivityManager.activeNetwork is NULL. Will try direct WifiManager info.")
                Log.w(TAG, "ConnectivityManager.activeNetwork is NULL. Will try direct WifiManager info.")
            }
        } catch (e: SecurityException) {
            FileLogger.logWifiUtils(context.applicationContext, TAG, "[CM Path] SecurityException: ${e.message}")
            Log.e(TAG, "[CM Path] SecurityException: ${e.message}", e)
        } catch (e: Exception) {
            FileLogger.logWifiUtils(context.applicationContext, TAG, "[CM Path] Generic Exception: ${e.message}")
            Log.e(TAG, "[CM Path] Generic Exception: ${e.message}", e)
        }

        // VERSUCH 2: Direkter Weg über WifiManager.connectionInfo (Fallback)
        Log.d(TAG, "Attempting direct WifiManager.connectionInfo path...")
        try {
            val connectionInfo: WifiInfo? = wifiManager.connectionInfo
            if (connectionInfo != null) {
                val rawBssid = connectionInfo.bssid
                val ssid = connectionInfo.ssid
                val supplicantState = connectionInfo.supplicantState

                Log.d(TAG, "[Direct WifiManager Path] WifiInfo: SSID='${ssid}', BSSID='${rawBssid}', SupplicantState='${supplicantState}'")

                if (supplicantState == SupplicantState.COMPLETED && isValidBssid(rawBssid, ssid)) {
                    Log.i(TAG, "[Direct WifiManager Path] VALID BSSID found (SupplicantState COMPLETED): '$rawBssid'")
                    return rawBssid
                } else if (isValidBssid(rawBssid, ssid)) {
                    Log.w(TAG, "[Direct WifiManager Path] BSSID '$rawBssid' seems valid, but SupplicantState is '$supplicantState' (not COMPLETED). Treating as not reliably connected for BSSID.")
                    return null
                } else {
                    Log.w(TAG, "[Direct WifiManager Path] INVALID or PLACEHOLDER BSSID, or SupplicantState not ideal. Raw BSSID='${rawBssid}', SSID='${ssid}', State='${supplicantState}'.")
                    return null
                }
            } else {
                Log.w(TAG, "[Direct WifiManager Path] WifiManager.connectionInfo is NULL.")
                return null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "[Direct WifiManager Path] SecurityException: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "[Direct WifiManager Path] Generic Exception: ${e.message}", e)
            return null
        } finally {
            Log.d(TAG, "[${System.currentTimeMillis()}] getCurrentBssid FINISHED")
        }
    }

    private fun isValidBssid(rawBssid: String?, ssid: String?): Boolean {
        val bssidPlaceholder1 = "02:00:00:00:00:00"
        val bssidPlaceholder2 = "00:00:00:00:00:00"
        val unknownBssidPlaceholder = "<unknown bssid>"

        return rawBssid != null &&
                rawBssid.isNotEmpty() &&
                !rawBssid.equals(bssidPlaceholder1, ignoreCase = true) &&
                !rawBssid.equals(bssidPlaceholder2, ignoreCase = true) &&
                !rawBssid.equals(unknownBssidPlaceholder, ignoreCase = true) &&
                // Ein leerer oder "<unknown ssid>" SSID ist nicht unbedingt ein Problem für die BSSID-Validität.
                // Aber ein UNINITIALIZED SupplicantState kann auf Probleme hindeuten,
                // obwohl wir das hauptsächlich im zweiten Pfad prüfen.
                // Für BSSID-Validität ist der SSID-Inhalt sekundär.
                ssid != SupplicantState.UNINITIALIZED.toString() // Dieser Check ist etwas unkonventionell hier, aber kann nicht schaden.
    }
}
