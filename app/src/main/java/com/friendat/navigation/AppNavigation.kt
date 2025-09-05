package com.friendat.navigation // Oder dein gewähltes Paket

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.friendat.ui.friends.FriendManagementScreen
import com.friendat.ui.screens.AddFriend2
import com.friendat.ui.screens.AddWifiScreen
import com.friendat.ui.screens.HomeScreen
import com.friendat.ui.screens.HomeScreen2
import com.friendat.ui.screens.LoginScreen
import com.friendat.ui.screens.WifiLocationsListScreen
import java.net.URLDecoder

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
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = NavRoute.Home2.route) {
            HomeScreen2(navController = navController)
        }

        composable(route = NavRoute.FriendManagement.route) {
            FriendManagementScreen(navController = navController)
        }

        composable(route = NavRoute.WifiLocationsList.route) {

            WifiLocationsListScreen(navController = navController)
        }


        composable(
            route = NavRoute.AddWifi.route,
            arguments = listOf(navArgument("bssidArg") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val encodedBssidArg = backStackEntry.arguments?.getString("bssidArg")
            val actualBssid = encodedBssidArg?.let {
                if (it == "NO_BSSID_FOUND") "" else URLDecoder.decode(it, "UTF-8")
            } ?: ""
            AddWifiScreen(
                navController = navController,
                bssid = actualBssid

            )
        }

        composable(route = NavRoute.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(route = NavRoute.AddFriend.route){
            AddFriend2(navController)
        }

    }
}