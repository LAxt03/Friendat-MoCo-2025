package com.friendat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey





//FriendStatus dient dazu, um den Status eines Freundes zu speichern. Ist der Freund in Netzwerk A so wird das gespeichert. Ändert er zu Netzwerk B oder zu keinem Netzwerk so wird es überschrieben.
//Dies ist auch notwendig für die Statusanzeige (FriendLiveStatusViewModel)
@Entity(tableName = "friend_received_status")
data class FriendStatus(
    @PrimaryKey val friendId: String,
    val locationName: String?,
    val bssid: String?,       // Wird warscheinlich nicht gebraucht, weil dies nicht angezeigt werden sollte (Interessant ist nur Name, Icon und Farbe)
    val isOnline: Boolean,
    val iconId: String?,     //Icon des Netzwerkes, die der Sender eingestellt hat
    val colorHex: String?,    //Farbe des Netzwerkes, die der Sender eingestellt hat
    val lastUpdateTimestamp: Long
)

