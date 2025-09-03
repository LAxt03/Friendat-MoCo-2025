package com.friendat.navigation // Oder dein gewähltes Paket

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.friendat.ui.friends.FriendManagementScreen
import com.friendat.ui.screens.AddWifiScreen
import com.friendat.ui.screens.HomeScreen2
import com.friendat.ui.screens.LoginScreen
import com.friendat.ui.screens.WifiLocationsListScreen

@Composable
fun AppNavigation(
) {
    val navController: NavHostController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Login.route
    ) {
        composable(route = NavRoute.Login.route) {
            LoginScreen(
                navController = navController,
                onLoginSuccess = { // Callback vom LoginScreen bei Erfolg
                    navController.navigate(NavRoute.Home2.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = NavRoute.FriendManagement.route) {
            FriendManagementScreen(navController = navController)
        }

        composable(route = NavRoute.WifiLocationsList.route) {
            WifiLocationsListScreen(navController = navController)
        }

        composable(route = NavRoute.AddWifi.route) { // Dein neuer AddWifiScreen
            AddWifiScreen(navController = navController)
        }

        composable(route = NavRoute.Home2.route) {
            HomeScreen2(navController = navController)
        }

    }
}