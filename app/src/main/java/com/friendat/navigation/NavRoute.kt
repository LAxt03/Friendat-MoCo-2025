package com.friendat.navigation

import java.net.URLEncoder

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login_screen")
    object Home2 : NavRoute("home2")
    object AddWifi : NavRoute("add_wifi_screen/{bssidArg}") {
        fun createRoute(bssid: String?): String {
            val encodedBssid = URLEncoder.encode(bssid ?: "NO_SSID_FOUND", "UTF-8")
            return "add_wifi_screen/$encodedBssid"
        }
    }
    object WifiLocationsList : NavRoute("wifi_locations_list_screen")
    object AddFriend: NavRoute("add_friend_screen")
    object FriendManagement: NavRoute("friend_management_screen")
    object FriendLiveStatus: NavRoute("friendLiveStatus")


    object Home : NavRoute("home")

}