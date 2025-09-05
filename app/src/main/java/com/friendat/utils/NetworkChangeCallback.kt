package com.friendat.utils // oder dein Paket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.friendat.work.LocationCheckWorker // Sicherstellen, dass der Pfad korrekt ist
import java.util.concurrent.TimeUnit

class NetworkChangeCallback(private val context: Context) : ConnectivityManager.NetworkCallback() {

    companion object {
        private const val TAG = "NetworkChangeCallback"
        private val WORKER_DELAY_SECONDS = 10L
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val logMessage = "onCapabilitiesChanged for $network: WiFi=$isWifi, Connected=${networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}"
        Log.i(TAG, logMessage) // LogCat
        FileLogger.logNetworkCallback(context.applicationContext, TAG, logMessage) // Datei
        val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Hat Internet
        val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Ist verbunden

        Log.i(TAG, "Capabilities changed for $network: WiFi=$isWifi, Cellular=$isCellular, Validated=$isValidated, Connected=$isConnected")
        // Wir interessieren uns für jede signifikante Änderung, die potenziell die BSSID beeinflusst
        // oder den Online-Status ändert. Der Worker entscheidet dann über die Details.
        // Eine valide Verbindung (WLAN oder Mobil) ist ein guter Trigger.
        // Auch wenn WLAN nur verbunden, aber nicht validiert ist (BSSID könnte verfügbar sein).
        if ((isWifi || isCellular) && isConnected) { // isConnected ist hier der grundlegendste Check
            Log.d(TAG, "Relevant network capability change detected for $network. Enqueuing worker.")
            FileLogger.logNetworkCallback(context.applicationContext, TAG, "Relevant change detected. Enqueuing worker.")
            enqueueLocationCheckWorker("CapChanged-$network-WiFi:$isWifi-Cell:$isCellular-Valid:$isValidated")
        } else if (!isConnected) {
            // Wenn die Verbindung für dieses Netzwerk verloren geht, auch ein Trigger
            Log.d(TAG, "Network $network is no longer connected. Enqueuing worker.")
            FileLogger.logNetworkCallback(context.applicationContext, TAG, "Network $network no longer connected. Enqueuing worker.")
            enqueueLocationCheckWorker("CapChanged-$network-Disconnected")
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        val logMessage = "onLost: $network. Enqueuing worker."
        Log.i(TAG, "Network lost: $network. Enqueuing worker.")
        FileLogger.logNetworkCallback(context.applicationContext, TAG, logMessage) // Datei
        enqueueLocationCheckWorker("Lost-$network")
    }

    private fun enqueueLocationCheckWorker(triggerReason: String) {
        FileLogger.logNetworkCallback(context.applicationContext, TAG, "Attempting to enqueue LocationCheckWorker (delay: ${WORKER_DELAY_SECONDS}s) due to: $triggerReason")
        Log.d(TAG, "Attempting to enqueue LocationCheckWorker (delay: ${WORKER_DELAY_SECONDS}s) due to: $triggerReason")

        val locationCheckWorkRequest = OneTimeWorkRequestBuilder<LocationCheckWorker>()
            .setInitialDelay(WORKER_DELAY_SECONDS, TimeUnit.SECONDS)
            .addTag(LocationCheckWorker.TAG) // Wichtig für Identifizierung und unique work
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            LocationCheckWorker.TAG, // Eindeutiger Name
            ExistingWorkPolicy.REPLACE, // Ersetzt existierende Worker mit gleichem Namen
            locationCheckWorkRequest
        )
        FileLogger.logNetworkCallback(context.applicationContext, TAG, "LocationCheckWorker enqueued with REPLACE policy. Reason: $triggerReason")
        Log.i(TAG, "LocationCheckWorker enqueued with REPLACE policy. Reason: $triggerReason")
    }

    // `onAvailable` kann hier weggelassen werden, da `onCapabilitiesChanged` meist detaillierter ist
    // und auch bei Verfügbarkeit getriggert wird.
}
