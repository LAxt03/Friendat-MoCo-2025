package com.friendat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friend_received_status") // Kann so bleiben oder z.B. friend_display_status
data class FriendStatus(
    @PrimaryKey val friendId: String,
    val locationName: String?,
    val bssid: String?,       // Optional, falls du es für die Anzeige brauchst
    val isOnline: Boolean,
    val iconId: String?,      // <<< NEUES FELD HINZUGEFÜGT
    val colorHex: String?,    // <<< NEUES FELD HINZUGEFÜGT
    val lastUpdateTimestamp: Long // Zeitstempel der letzten Aktualisierung dieses Eintrags
)

