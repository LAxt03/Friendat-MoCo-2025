package com.friendat.data.model


//Wird verwendet um abzugleichen ob die Location die der Nutzer sendet unterschiedlich zu der letzten ist. Es werden nur bssid und locationName verglichen
//Zukünftig sollten auch Icon und Farbe abgeglichen werden, um bei einer Änderung des Icon oder Farbe auch ein update zu senden.
//Wird vom Worker verwendet
data class LastSentLocationStatus(
    val bssid: String?,
    val locationName: String?,
    val timestamp: Long // Wichtig: Zeitstempel der letzten Bestätigung/Sendung
)