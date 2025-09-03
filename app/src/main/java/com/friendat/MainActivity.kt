package com.friendat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.friendat.navigation.AppNavigation
import com.friendat.ui.theme.FriendatTheme
import dagger.hilt.android.AndroidEntryPoint // Import hinzufügen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FriendatTheme {
                AppNavigation()
            }
        }
    }
}