package com.friendat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.friendat.data.sources.local.converters.DateConverter
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


//Wird für Room und Firebase verwendet. Room benötigt @Entity
@Entity(tableName = "wifi_locations")
@TypeConverters(DateConverter::class)
data class WifiLocation(
    @PrimaryKey
    @DocumentId
    var id: String = "",

    var userId: String = "",
    var name: String = "",
    var bssid: String = "", // Wird für den Abgleich im Worker verwendet (nicht firebase!)
    var iconId: String = "default_icon", // Wird im Worker und für die Anzeige verwendet
    var colorHex: String = "#CCCCCC", // Standardfarbe

    @ServerTimestamp // Für Firestore
    var createdAt: Date? = null, // Wird von Room dank DateConverter als Long gespeichert

    @ServerTimestamp // Für Firestore
    var lastModifiedAt: Date? = null // Wird von Room dank DateConverter als Long gespeichert
) {

    constructor() : this(
        id = "",
        userId = "",
        name = "",
        bssid = "",
        iconId = "default_icon",
        colorHex = "#CCCCCC",
        createdAt = null,
        lastModifiedAt = null
    )
}
