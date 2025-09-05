package com.friendat.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.friendat.work.LocationCheckWorker
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
        Log.i(TAG, logMessage)
        val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Ist verbunden

        Log.i(TAG, "Capabilities changed for $network: WiFi=$isWifi, Cellular=$isCellular, Validated=$isValidated, Connected=$isConnected")

        if ((isWifi || isCellular) && isConnected) {
            Log.d(TAG, "Relevant network capability change detected for $network. Enqueuing worker.")
            enqueueLocationCheckWorker("CapChanged-$network-WiFi:$isWifi-Cell:$isCellular-Valid:$isValidated")
        } else if (!isConnected) {
            Log.d(TAG, "Network $network is no longer connected. Enqueuing worker.")
            enqueueLocationCheckWorker("CapChanged-$network-Disconnected")
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        val logMessage = "onLost: $network. Enqueuing worker."
        Log.i(TAG, "Network lost: $network. Enqueuing worker.")
        enqueueLocationCheckWorker("Lost-$network")
    }

    private fun enqueueLocationCheckWorker(triggerReason: String) {
        Log.d(TAG, "Attempting to enqueue LocationCheckWorker (delay: ${WORKER_DELAY_SECONDS}s) due to: $triggerReason")

        val locationCheckWorkRequest = OneTimeWorkRequestBuilder<LocationCheckWorker>()
            .setInitialDelay(WORKER_DELAY_SECONDS, TimeUnit.SECONDS)
            .addTag(LocationCheckWorker.TAG)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            LocationCheckWorker.TAG,
            ExistingWorkPolicy.REPLACE,
            locationCheckWorkRequest
        )
        Log.i(TAG, "LocationCheckWorker enqueued with REPLACE policy. Reason: $triggerReason")
    }

}
