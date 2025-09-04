package com.friendat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.friendat.data.sources.local.converters.DateConverter // Sicherstellen, dass dieser Pfad korrekt ist
import com.google.firebase.firestore.DocumentId
// import com.google.firebase.firestore.Exclude // Nicht direkt verwendet, kann entfernt werden, wenn nicht benötigt
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "wifi_locations")
@TypeConverters(DateConverter::class)
data class WifiLocation(
    @PrimaryKey
    @DocumentId
    var id: String = "", // `var` ist gut für Firestore Deserialisierung und Room Flexibilität

    var userId: String = "",
    var name: String = "",
    var bssid: String = "", // Wird für den Abgleich im Worker verwendet
    var iconId: String = "default_icon", // Wird im Worker und für die Anzeige verwendet
    var colorHex: String = "#CCCCCC",

    @ServerTimestamp // Für Firestore
    var createdAt: Date? = null, // Wird von Room dank DateConverter als Long gespeichert

    @ServerTimestamp // Für Firestore
    var lastModifiedAt: Date? = null // Wird von Room dank DateConverter als Long gespeichert
) {
    // Leerer Konstruktor ist wichtig für Firestore Deserialisierung.
    // Room benötigt ihn nicht, wenn alle Primärkonstruktor-Parameter Standardwerte haben
    // oder 'var' sind, aber er stört Room auch nicht.
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

    // Auskommentierte @Ignore / @Exclude Beispiele sind gut als Referenz,
    // aber wenn du sie nicht aktiv brauchst, kannst du sie entfernen, um die Klasse sauberer zu halten.
}
