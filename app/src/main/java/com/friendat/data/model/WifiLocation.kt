package com.friendat.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WifiLocation(
    @DocumentId // Firestore automatische ID mapping
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val ssid: String = "",
    // val bssid: String? = null, // bssid wäre genauer
    val iconId: String = "default_icon",
    val colorHex: String = "#CCCCCC", // Grau

    @ServerTimestamp
    val createdAt: Date? = null, //wird automatisch erstellt
    @ServerTimestamp
    val lastModifiedAt: Date? = null //wird automatisch aktualisiert


) {
    // Nötig für die Deserialisierung von Firestore
    constructor() : this("", "", "", "", "default_icon", "#CCCCCC", null, null)
}

