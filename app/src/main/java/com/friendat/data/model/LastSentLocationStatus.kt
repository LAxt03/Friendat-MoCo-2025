package com.friendat.data.model

data class LastSentLocationStatus(
    val bssid: String?,
    val locationName: String?,
    // val locationIcon: String?, // Falls du das Icon auch brauchst
    val timestamp: Long // Wichtig: Zeitstempel der letzten Bestätigung/Sendung
)