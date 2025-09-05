package com.friendat.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date






//Datenmodell für Freundschaften zwischen zwei Nutzern


data class Friendship(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val requesterId: String = "",
    val status: String = FriendshipStatus.PENDING.name,

    //Date Variablen
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val respondedAt: Date? = null
) {
    constructor(
        user1Id: String,
        user2Id: String,
        initiatorId: String,

        //Wird eine Freundschaftsanfrage gesendet, so steht sie zuerst auf PENDING
        initialStatus: FriendshipStatus = FriendshipStatus.PENDING
    ) : this(
        //Enthält beide Nutzer
        participants = listOf(user1Id, user2Id).sorted(),

        //Der Sender der Anfrage
        requesterId = initiatorId,
        status = initialStatus.name
    )
}
