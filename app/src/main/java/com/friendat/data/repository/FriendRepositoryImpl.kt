package com.friendat.data.repository

import android.util.Log
import com.friendat.data.model.Friendship
import com.friendat.data.model.FriendshipStatus
import com.friendat.data.model.UserDisplayInfo
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore // Wird über Hilt injiziert
) : FriendRepository {

    companion object {
        private const val TAG = "FriendRepositoryImpl"
        private const val USERS_COLLECTION = "users" // Sammlung für Nutzerprofile
        private const val FRIENDSHIPS_COLLECTION = "friendships" // Sammlung für Freundschaftsbeziehungen
    }

    /**
     * Finds a user by their email.
     * Assumes a 'users' collection in Firestore where user documents have an 'email' field.
     * The document ID of a user in the 'users' collection is assumed to be their Firebase Auth UID.
     */
    override suspend fun findUserByEmail(email: String): Result<UserDisplayInfo?> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be blank."))
        }
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email.trim())
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Result.success(null) // Kein Nutzer mit dieser E-Mail gefunden
            } else {
                val document = querySnapshot.documents.first()
                val userDisplayInfo = UserDisplayInfo(
                    uid = document.id, // Annahme: Dokument-ID ist die UID
                    displayName = document.getString("displayName"), // Optionales Feld
                    email = document.getString("email")
                )
                Result.success(userDisplayInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by email: $email", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a friend request from the [currentUserId] to the [targetUserId].
     * Creates a new document in the 'friendships' collection with PENDING status.
     * Ensures that 'participants' list is sorted to maintain consistency.
     */
    override suspend fun sendFriendRequest(currentUserId: String, targetUserId: String): Result<Unit> {
        if (currentUserId == targetUserId) {
            return Result.failure(IllegalArgumentException("Cannot send friend request to oneself."))
        }

        return try {
            val sortedParticipants = listOf(currentUserId, targetUserId).sorted()

            // Überprüfen, ob bereits eine Freundschaft (egal welcher Status) zwischen diesen Nutzern existiert
            val existingFriendshipQuery = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("participants", sortedParticipants)
                .limit(1)
                .get()
                .await()

            if (!existingFriendshipQuery.isEmpty) {
                // Eine Beziehung existiert bereits. Überprüfe den Status.
                val existingFriendshipDoc = existingFriendshipQuery.documents.first()
                val existingStatus = existingFriendshipDoc.getString("status")
                if (existingStatus == FriendshipStatus.PENDING.name) {
                    return Result.failure(Exception("Friend request already pending."))
                } else if (existingStatus == FriendshipStatus.ACCEPTED.name) {
                    return Result.failure(Exception("You are already friends."))
                }
                // Für andere Status (falls später hinzugefügt) ggf. andere Logik
            }

            // Neue Freundschaftsanfrage erstellen
            val newFriendship = Friendship(
                user1Id = currentUserId,
                user2Id = targetUserId,
                initiatorId = currentUserId, // Der aktuelle Nutzer sendet die Anfrage
                initialStatus = FriendshipStatus.PENDING
            )

            // Explizit die Felder setzen, um @ServerTimestamp korrekt zu nutzen
            val friendshipData = hashMapOf(
                "participants" to newFriendship.participants,
                "requesterId" to newFriendship.requesterId,
                "status" to newFriendship.status,
                "createdAt" to FieldValue.serverTimestamp(), // Wird vom Server gesetzt
                "respondedAt" to null // Wird erst bei Antwort gesetzt
            )

            firestore.collection(FRIENDSHIPS_COLLECTION)
                .add(friendshipData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request from $currentUserId to $targetUserId", e)
            Result.failure(e)
        }
    }

    /**
     * Gets a flow of pending friend requests for the given [userId] where this user is the recipient.
     * This means the [userId] is in 'participants' and is NOT the 'requesterId',
     * and status is PENDING.
     */
    override fun getPendingFriendRequests(userId: String): Flow<Result<List<Friendship>>> = callbackFlow {
        val listenerRegistration = firestore.collection(FRIENDSHIPS_COLLECTION)
            .whereArrayContains("participants", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen error for pending requests for user $userId", e)
                    trySend(Result.failure(e)).isFailure // trySend non-blocking
                    close(e) // Schließt den Flow bei Fehler
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val pendingRequests = snapshots.documents.mapNotNull { doc ->
                        try {
                            // Wichtig: document.toObject(Friendship::class.java) funktioniert nur,
                            // wenn Friendship einen parameterlosen Konstruktor hat oder alle Felder
                            // Standardwerte haben und die Namen exakt mit Firestore übereinstimmen.
                            doc.toObject<Friendship>()?.copy(id = doc.id)
                        } catch (parseError: Exception) {
                            Log.e(TAG, "Error parsing Friendship document ${doc.id}", parseError)
                            null
                        }
                    }.filter {
                        // Clientseitige Filterung: Nur Anfragen, bei denen der aktuelle Nutzer der Empfänger ist
                        it.requesterId != userId && it.participants.contains(userId)
                    }
                    trySend(Result.success(pendingRequests)).isFailure
                }
            }
        awaitClose {
            Log.d(TAG, "Closing listener for pending requests for user $userId")
            listenerRegistration.remove()
        }
    }


    override fun getAcceptedFriends(userId: String): Flow<Result<List<Friendship>>> = callbackFlow {
        Log.d(TAG, "Subscribing to accepted friends for user: $userId")
        val listenerRegistration = firestore.collection(FRIENDSHIPS_COLLECTION)
            .whereArrayContains("participants", userId) // Der Nutzer muss Teil der Beziehung sein
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name) // Nur akzeptierte Freundschaften
            .orderBy("respondedAt", Query.Direction.DESCENDING) // Optional: Nach Akzeptanzdatum sortieren
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen error for accepted friends for user $userId", e)
                    trySend(Result.failure(e)).isFailure
                    close(e) // Schließt den Flow bei Fehler
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val acceptedFriends = snapshots.documents.mapNotNull { doc ->
                        try {
                            doc.toObject<Friendship>()?.copy(id = doc.id)
                        } catch (parseError: Exception) {
                            Log.e(TAG, "Error parsing Friendship document ${doc.id} for accepted", parseError)
                            null
                        }
                    }
                    Log.d(TAG, "Accepted friends for user $userId: ${acceptedFriends.size}")
                    trySend(Result.success(acceptedFriends)).isFailure
                } else {
                    Log.d(TAG, "Accepted friends snapshots for user $userId is null")
                }
            }
        awaitClose {
            Log.d(TAG, "Closing listener for accepted friends for user $userId")
            listenerRegistration.remove()
        }
    }

    override suspend fun acceptFriendRequest(friendshipId: String, currentUserId: String): Result<Unit> {
        if (friendshipId.isBlank()) {
            return Result.failure(IllegalArgumentException("Friendship ID cannot be blank."))
        }
        return try {
            val friendshipRef = firestore.collection(FRIENDSHIPS_COLLECTION).document(friendshipId)
            val friendshipDoc = friendshipRef.get().await()

            if (!friendshipDoc.exists()) {
                return Result.failure(Exception("Friendship request not found."))
            }

            val friendship = friendshipDoc.toObject<Friendship>()
            if (friendship == null) {
                return Result.failure(Exception("Failed to parse friendship data."))
            }

            // Sicherheitsprüfung: Ist der currentUserId der Empfänger und ist der Status PENDING?
            if (friendship.status != FriendshipStatus.PENDING.name) {
                return Result.failure(Exception("Friendship request is not pending."))
            }
            if (friendship.requesterId == currentUserId || !friendship.participants.contains(currentUserId)) {
                // Der Anfragende kann seine eigene Anfrage nicht annehmen ODER der currentUserId ist nicht mal Teilnehmer
                return Result.failure(Exception("User not authorized to accept this request."))
            }

            val updates = hashMapOf<String, Any>(
                "status" to FriendshipStatus.ACCEPTED.name,
                "respondedAt" to FieldValue.serverTimestamp()
            )
            friendshipRef.update(updates).await()
            Log.d(TAG, "Friend request $friendshipId accepted by $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request $friendshipId by $currentUserId", e)
            Result.failure(e)
        }
    }

    override suspend fun declineFriendRequest(friendshipId: String, currentUserId: String): Result<Unit> {
        if (friendshipId.isBlank()) {
            return Result.failure(IllegalArgumentException("Friendship ID cannot be blank."))
        }
        return try {
            val friendshipRef = firestore.collection(FRIENDSHIPS_COLLECTION).document(friendshipId)
            val friendshipDoc = friendshipRef.get().await()

            if (!friendshipDoc.exists()) {
                // Wenn die Anfrage nicht existiert, ist es quasi schon "abgelehnt" oder wurde zurückgezogen.
                // Man könnte hier auch einen Erfolg melden, je nach gewünschtem Verhalten.
                Log.w(TAG, "Decline request: Friendship $friendshipId not found. Assuming already handled.")
                return Result.success(Unit) // Oder Result.failure(Exception("Friendship request not found."))
            }

            val friendship = friendshipDoc.toObject<Friendship>()
            if (friendship == null) {
                return Result.failure(Exception("Failed to parse friendship data for decline."))
            }

            // Sicherheitsprüfung: Ist der Status PENDING und der currentUserId der Empfänger ODER der Ersteller?
            // Der Ersteller könnte seine eigene Anfrage zurückziehen (optional).
            // Für "decline" fokussieren wir uns auf den Empfänger.
            if (friendship.status != FriendshipStatus.PENDING.name) {
                Log.w(TAG, "Decline request: Friendship $friendshipId is not pending (status: ${friendship.status}).")
                // Wenn nicht mehr PENDING, vielleicht wurde sie schon akzeptiert/abgelehnt.
                return Result.success(Unit) // Als Erfolg werten, da keine Aktion mehr nötig.
            }
            if (friendship.requesterId != currentUserId && !friendship.participants.contains(currentUserId)) {
                // Weder Ersteller noch Empfänger
                return Result.failure(Exception("User not authorized to decline this request."))
            }
            // Spezifischer für decline: Nur der Empfänger (nicht der requesterId) sollte ablehnen.
            if (friendship.requesterId == currentUserId) {
                return Result.failure(Exception("Requester cannot decline their own request. Use cancel/withdraw instead."))
            }


            friendshipRef.delete().await()
            Log.d(TAG, "Friend request $friendshipId declined/deleted by $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining friend request $friendshipId by $currentUserId", e)
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(friendshipId: String, currentUserId: String): Result<Unit> {
        if (friendshipId.isBlank()) {
            return Result.failure(IllegalArgumentException("Friendship ID cannot be blank."))
        }
        return try {
            val friendshipRef = firestore.collection(FRIENDSHIPS_COLLECTION).document(friendshipId)
            val friendshipDoc = friendshipRef.get().await()

            if (!friendshipDoc.exists()) {
                Log.w(TAG, "Remove friend: Friendship $friendshipId not found.")
                return Result.success(Unit) // Betrachte als Erfolg, wenn es nichts zu entfernen gibt
            }

            val friendship = friendshipDoc.toObject<Friendship>()
            if (friendship == null) {
                return Result.failure(Exception("Failed to parse friendship data for remove."))
            }

            // Sicherheitsprüfung: Ist der Status ACCEPTED und ist der currentUserId einer der Teilnehmer?
            if (friendship.status != FriendshipStatus.ACCEPTED.name) {
                return Result.failure(Exception("Cannot remove friend: Friendship is not in ACCEPTED state."))
            }
            if (!friendship.participants.contains(currentUserId)) {
                return Result.failure(Exception("User not part of this friendship, cannot remove."))
            }

            friendshipRef.delete().await()
            Log.d(TAG, "Friendship $friendshipId removed by $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend $friendshipId by $currentUserId", e)
            Result.failure(e)
        }
    }

    override suspend fun getUsersDisplayInfo(userIds: List<String>): Result<List<UserDisplayInfo>> {
        if (userIds.isEmpty()) {
            return Result.success(emptyList())
        }

        // Firestore 'in' Abfragen sind auf maximal 30 IDs pro Abfrage beschränkt (vorher 10).
        // Wir müssen die Abfrage ggf. aufteilen (chunking).
        val chunkSize = 30
        val userDisplayInfoList = mutableListOf<UserDisplayInfo>()

        return try {
            userIds.chunked(chunkSize).forEach { chunk ->
                if (chunk.isNotEmpty()) { // Sicherstellen, dass der Chunk nicht leer ist
                    val querySnapshot = firestore.collection(USERS_COLLECTION)
                        .whereIn(FieldPath.documentId(), chunk) // Abfrage nach Dokument-IDs
                        .get()
                        .await()

                    querySnapshot.documents.forEach { document ->
                        val userInfo = UserDisplayInfo(
                            uid = document.id,
                            displayName = document.getString("displayName"),
                            email = document.getString("email")
                        )
                        userDisplayInfoList.add(userInfo)
                    }
                }
            }
            Result.success(userDisplayInfoList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users display info for UIDs: $userIds", e)
            Result.failure(e)
        }
    }
}
