package com.friendat.navigation

import java.net.URLEncoder


//Objekte für den NavController
sealed class NavRoute(val route: String) {
    //Login
    object Login : NavRoute("login_screen")

    //Startseite mit Testbuttons und Funktionen
    object Home2 : NavRoute("home2")

    //WifiObject weil bssid automatisch eingetragen wird im nächsten Screen muss bssid mitgegeben werden.
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

    //Startseite
    object Home : NavRoute("home")

}