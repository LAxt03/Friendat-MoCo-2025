package com.friendat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyFriendatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

    }
}
