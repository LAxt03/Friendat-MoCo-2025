package com.friendat

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration // Import für Configuration
// Kein separater Import für Configuration.Provider mehr nötig, wenn man direkt die Eigenschaft überschreibt
import com.friendat.utils.NetworkChangeCallback // Stelle sicher, dass der Pfad korrekt ist
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyFriendatApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private lateinit var networkChangeCallback: NetworkChangeCallback

    // Korrigierte Implementierung für neuere WorkManager Versionen
    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO) // Oder Log.DEBUG für mehr Details
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // WorkManager wird initialisiert, wenn es zum ersten Mal benötigt wird,
        // und verwendet dabei die workManagerConfiguration oben.
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        networkChangeCallback = NetworkChangeCallback(applicationContext)
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkChangeCallback)
        } else {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkChangeCallback)
        }
        Log.i("MyFriendatApplication", "NetworkChangeCallback registered.")
    }
}
