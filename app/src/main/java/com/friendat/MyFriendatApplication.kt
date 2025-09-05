package com.friendat

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.friendat.utils.NetworkChangeCallback
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyFriendatApplication : Application(), Configuration.Provider {

    //Worker Init
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    //Callback für WLAN Änderungen Init
    private lateinit var networkChangeCallback: NetworkChangeCallback

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
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
