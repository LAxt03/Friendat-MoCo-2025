package com.friendat.services

import android.content.Context // Import für Context
import android.util.Log
import com.friendat.data.model.FriendStatus
import com.friendat.di.FcmServiceEntryPoint // <<< DEIN ENTRYPOINT IMPORTIEREN
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.EntryPointAccessors // <<< FÜR DEN ZUGRIFF AUF DEN ENTRYPOINT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// ... (Widget-Imports, falls nötig) ...

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFMService"
        // SharedPreferences Konstanten
        private const val FCM_PREFS_NAME = "FCM_TOKEN_PREFS"
        private const val KEY_PENDING_FCM_TOKEN = "pending_fcm_token"

        /**
         * Versucht, einen zwischengespeicherten FCM-Token an Firestore zu senden.
         * Diese Methode sollte nach erfolgreicher Benutzeranmeldung aufgerufen werden.
         *
         * @param context Der ApplicationContext.
         * @param userId Die ID des aktuell angemeldeten Benutzers.
         */
        fun sendPendingTokenToServerIfNecessary(context: Context, userId: String) {
            val prefs = context.getSharedPreferences(FCM_PREFS_NAME, Context.MODE_PRIVATE)
            val pendingToken = prefs.getString(KEY_PENDING_FCM_TOKEN, null)

            if (pendingToken != null) {
                Log.i(TAG, "Found pending FCM token for user $userId in SharedPreferences. Attempting to send...")
                updateFirestoreToken(userId, pendingToken) { success ->
                    if (success) {
                        prefs.edit().remove(KEY_PENDING_FCM_TOKEN).apply()
                        Log.i(TAG, "Pending FCM token for $userId sent successfully and removed from prefs.")
                    } else {
                        Log.e(TAG, "Failed to send pending FCM token for $userId. It will remain in prefs for a later attempt.")
                    }
                }
            } else {
                Log.d(TAG, "No pending FCM token found in SharedPreferences for user $userId.")
                // Optional: Hier könnte man auch proaktiv den aktuellen Token holen und senden,
                // falls er sich seit dem letzten erfolgreichen Senden geändert hat.
                // FirebaseMessaging.getInstance().token.addOnCompleteListener { task -> ... }
            }
        }

        /**
         * Hilfsmethode zum Aktualisieren des Tokens in Firestore.
         * Kann von überall aufgerufen werden, wo Context und User-Infos verfügbar sind.
         */
        fun updateFirestoreToken(userId: String, token: String, callback: (Boolean) -> Unit) {
            if (token.isBlank()) {
                Log.w(TAG, "Cannot update Firestore with a blank token for user $userId.")
                callback(false)
                return
            }
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)
            val tokenData = hashMapOf("fcmToken" to token)
            userDocRef.set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.i(TAG, "FCM token successfully updated/set in Firestore for user: $userId")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating/setting FCM token in Firestore for user $userId", e)
                    callback(false)
                }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")

        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        if (currentFirebaseUser != null) {
            // Benutzer ist bereits angemeldet, Token direkt senden
            Log.d(TAG, "User ${currentFirebaseUser.uid} is already logged in. Sending token directly.")
            updateFirestoreToken(currentFirebaseUser.uid, token) { success ->
                if (!success) {
                    // Fallback: Wenn das Senden fehlschlägt, trotzdem in Prefs speichern
                    Log.w(TAG, "Direct token send failed for logged-in user. Saving to SharedPreferences as fallback.")
                    saveTokenToPrefs(token)
                } else {
                    // Wenn erfolgreich, sicherstellen, dass kein alter Pending-Token mehr da ist
                    // (unwahrscheinlich, aber zur Sicherheit)
                    removeTokenFromPrefsIfMatches(token)
                }
            }
        } else {
            // Benutzer nicht angemeldet, Token in SharedPreferences speichern für später
            Log.w(TAG, "User not logged in when onNewToken called. Saving token to SharedPreferences.")
            saveTokenToPrefs(token)
        }
    }

    private fun saveTokenToPrefs(token: String?) {
        if (token == null || token.isBlank()) { // Token darf nicht leer sein
            Log.w(TAG, "Cannot save null or blank token to prefs.")
            return
        }
        val prefs = applicationContext.getSharedPreferences(FCM_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_FCM_TOKEN, token).apply()
        Log.i(TAG, "FCM Token saved to SharedPreferences: $token")
    }

    private fun removeTokenFromPrefsIfMatches(sentToken: String) {
        val prefs = applicationContext.getSharedPreferences(FCM_PREFS_NAME, Context.MODE_PRIVATE)
        val pendingToken = prefs.getString(KEY_PENDING_FCM_TOKEN, null)
        if (pendingToken == sentToken) {
            prefs.edit().remove(KEY_PENDING_FCM_TOKEN).apply()
            Log.i(TAG, "Successfully sent token was also the pending token. Removed from prefs.")
        }
    }

    // Die ursprüngliche sendRegistrationToServer-Methode wird nicht mehr direkt von onNewToken aufgerufen.
    // Ihre Logik ist jetzt in updateFirestoreToken und der Entscheidungslogik in onNewToken.
    // Ich lasse sie hier auskommentiert, falls du sie für andere Zwecke wiederverwenden möchtest,
    // aber in der aktuellen Form wird sie nicht mehr benötigt.
    /*
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w(TAG, "FCM token is null, cannot send to Firestore.")
            return
        }
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUserId)
            val tokenData = hashMapOf("fcmToken" to token)
            userDocRef.set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.i(TAG, "FCM token successfully updated/set in Firestore for user: $currentUserId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating/setting FCM token in Firestore for user $currentUserId", e)
                }
        } else {
            Log.w(TAG, "Cannot save FCM token to Firestore: User not currently logged in.")
        }
    }
    */

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message Received!")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.i(TAG, "Message Data Payload: ${remoteMessage.data}")
            val messageType = remoteMessage.data["type"]

            if (messageType == "FRIEND_STATUS_UPDATE") {
                val updatedUserId = remoteMessage.data["updatedUserId"]
                val locationName = remoteMessage.data["locationName"]
                val bssid = remoteMessage.data["bssid"]
                val isOnlineStr = remoteMessage.data["isOnline"]
                val iconId = remoteMessage.data["iconId"]
                val colorHex = remoteMessage.data["colorHex"]
                val timestamp = remoteMessage.data["timestamp"]

                Log.i(
                    TAG,
                    "FRIEND_STATUS_UPDATE received for User: $updatedUserId, " +
                            "Location: $locationName, Online: $isOnlineStr, BSSID: $bssid, " +
                            "Icon: $iconId, Color: $colorHex, Timestamp: $timestamp"
                )

                if (updatedUserId == null) {
                    Log.e(TAG, "FRIEND_STATUS_UPDATE missing updatedUserId. Skipping.")
                    return
                }

                // DAO über Hilt EntryPoint holen
                val hiltEntryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    FcmServiceEntryPoint::class.java
                )
                val friendStatusDao = hiltEntryPoint.friendStatusDao()

                serviceScope.launch {
                    try {
                        val statusToSave = FriendStatus(
                            friendId = updatedUserId,
                            locationName = locationName,
                            bssid = bssid,
                            isOnline = isOnlineStr?.toBooleanStrictOrNull() ?: false,
                            iconId = iconId,
                            colorHex = colorHex,
                            lastUpdateTimestamp = System.currentTimeMillis()
                        )
                        friendStatusDao.insertOrUpdate(statusToSave)
                        Log.i(
                            TAG,
                            "Successfully updated local Room DB (FriendStatus) for $updatedUserId: $statusToSave"
                        )

                        // TODO: Widget-Aktualisierung
                        // triggerWidgetUpdate(updatedUserId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing FRIEND_STATUS_UPDATE and saving to Room", e)
                    }
                }
            } else {
                Log.d(TAG, "Received FCM data message of unknown type: $messageType")
            }
        }

        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            Log.d(TAG, "Message Notification Title: ${notification.title}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    // ... (triggerWidgetUpdate, falls benötigt) ...
}
