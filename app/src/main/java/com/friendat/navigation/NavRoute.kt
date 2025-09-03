package com.friendat.navigation

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login_screen")
    object Home2 : NavRoute("home2")
    object AddWifi : NavRoute("add_wifi_screen")
    object WifiLocationsList : NavRoute("wifi_locations_list_screen")
    object AddFriend: NavRoute("add_friend_screen")
    object FriendManagement: NavRoute("friend_management_screen")

}