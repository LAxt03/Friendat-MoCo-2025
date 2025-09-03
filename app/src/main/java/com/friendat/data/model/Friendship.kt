package com.friendat.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Friendship(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val requesterId: String = "",
    val status: String = FriendshipStatus.PENDING.name,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val respondedAt: Date? = null
) {
    constructor(
        user1Id: String,
        user2Id: String,
        initiatorId: String,
        initialStatus: FriendshipStatus = FriendshipStatus.PENDING
    ) : this(
        participants = listOf(user1Id, user2Id).sorted(),
        requesterId = initiatorId,
        status = initialStatus.name
    )

    fun getOtherParticipantId(currentUserId: String): String? {
        return participants.firstOrNull { it != currentUserId }
    }
}
