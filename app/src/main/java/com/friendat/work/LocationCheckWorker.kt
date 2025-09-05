package com.friendat.work // Stelle sicher, dass dies dein korrekter Paketname ist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.friendat.data.model.LastSentLocationStatus // Dein Modell
import com.friendat.data.repository.LastStatusRepository // Dein Repository
import com.friendat.data.sources.local.dao.WifiLocationDao
import com.friendat.utils.FileLogger
import com.friendat.utils.WifiUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class LocationCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wifiLocationDao: WifiLocationDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val lastStatusRepository: LastStatusRepository // Wird über dein RepositoryModule bereitgestellt
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "LocationCheckWorker"
        const val USER_STATUS_COLLECTION = "user_status" // Deine Firestore Collection für den User-Status
    }

    override suspend fun doWork(): Result {
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Worker execution started.")
        Log.d(TAG, "[FOCUS_WLAN_AN] Worker execution started.")

        // 1. Authentifizierten Benutzer holen
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] No authenticated user. Worker cannot proceed.")
            Log.w(TAG, "[FOCUS_WLAN_AN] No authenticated user. Worker cannot proceed.")
            return Result.failure()
        }
        val userId = currentUser.uid
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User ID: $userId")
        Log.d(TAG, "[FOCUS_WLAN_AN] User ID: $userId")

        // 2. Berechtigung prüfen
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] ACCESS_FINE_LOCATION permission not granted.")
            Log.e(TAG, "[FOCUS_WLAN_AN] ACCESS_FINE_LOCATION permission not granted.")
            // Ohne Berechtigung können wir die BSSID nicht zuverlässig bekommen.
            // Sende "offline" oder einen undefinierten Status.
            updateFirestoreStatus(userId, null, null, false)
            return Result.success() // Dennoch Success, da der Worker seine Aufgabe (Status setzen) erledigt hat
        }

        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Attempting to get BSSID from WifiUtils.")
        // 3. Aktuelle BSSID und bekannten Ort ermitteln
        val currentBssid = WifiUtils.getCurrentBssid(appContext)
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Current BSSID from WifiUtils: $currentBssid")
        Log.d(TAG, "[FOCUS_WLAN_AN] Current BSSID from WifiUtils: $currentBssid")

        var currentEffectiveLocationName: String? = null
        // var currentEffectiveLocationIcon: String? = null // Dein WifiLocation hat iconId
        var isAtKnownLocation = false

        if (currentBssid != null) {
            val knownLocations = try {
                // Diese Methode gibt direkt List<WifiLocation> zurück, das ist gut.

                wifiLocationDao.getAllLocationsForUserSuspend(userId)
            } catch (e: Exception) {
                Log.e(TAG, "[FOCUS_WLAN_AN] Error fetching known locations for user $userId from DB.", e)
                return Result.retry() // DB-Problem, später erneut versuchen
            }
            Log.d(TAG, "[FOCUS_WLAN_AN] Found ${knownLocations.size} known locations for user $userId.")

            val currentRecognizedLocation = knownLocations.find {
                it.bssid.equals(currentBssid, ignoreCase = true)
            }

            if (currentRecognizedLocation != null) {
                currentEffectiveLocationName = currentRecognizedLocation.name
                // currentEffectiveLocationIcon = currentRecognizedLocation.iconId
                isAtKnownLocation = true
                FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User is at known location: '$currentEffectiveLocationName'")
                Log.i(TAG, "[FOCUS_WLAN_AN] User is at known location: '$currentEffectiveLocationName' (BSSID: $currentBssid)")
            } else {
                FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User is on an unknown WiFi.")
                Log.i(TAG, "[FOCUS_WLAN_AN] User is on an unknown WiFi (BSSID: $currentBssid). Location name will be null.")
                // currentEffectiveLocationName bleibt null
            }
        } else {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User is not connected to any WiFi network.")
            Log.i(TAG, "[FOCUS_WLAN_AN] User is not connected to any WiFi network. Location name will be null.")
            // currentEffectiveLocationName bleibt null
        }

        // 4. Letzten *an Firestore gesendeten* Status laden (optional, aber gut zum Vergleichen)
        val lastSavedStatus = lastStatusRepository.getLastSentStatus(appContext)
        Log.d(TAG, "[FOCUS_WLAN_AN] Last locally saved status: $lastSavedStatus")
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Last locally saved status: $lastSavedStatus")

        // 5. IMMER SENDEN oder nur bei Änderung? Für das "WLAN AN"-Problem ist es sicherer,
        //    bei jedem Durchlauf den aktuellen Zustand zu senden, es sei denn, er ist EXAKT gleich.
        //    Dies vermeidet, dass ein alter "null"-Status in Firebase hängen bleibt.

        // Wir prüfen, ob der NEU ermittelte Status sich vom ZULETZT LOKAL GESPEICHERTEN unterscheidet.
        // Der Timestamp im lastSavedStatus wird hier ignoriert.
        val statusContentHasChanged = lastSavedStatus == null ||
                lastSavedStatus.bssid != currentBssid ||
                lastSavedStatus.locationName != currentEffectiveLocationName
        // || lastSavedStatus.locationIcon != currentEffectiveLocationIcon

        if (statusContentHasChanged) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] CHANGE DETECTED. Updating Firestore.")
            Log.i(TAG, "[FOCUS_WLAN_AN] CHANGE DETECTED (or first run). Updating Firestore.")
            Log.d(TAG, "[FOCUS_WLAN_AN] Old Local: BSSID=${lastSavedStatus?.bssid}, Name=${lastSavedStatus?.locationName}")
            Log.d(TAG, "[FOCUS_WLAN_AN] New State: BSSID=$currentBssid, Name=$currentEffectiveLocationName")

            return updateFirestoreStatus(userId, currentEffectiveLocationName, currentBssid, isAtKnownLocation)
        } else {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] NO CHANGE detected.")
            Log.i(TAG, "[FOCUS_WLAN_AN] NO CHANGE detected compared to last locally saved status. Firestore update not strictly needed, but local timestamp will be updated.")
            // Aktualisiere trotzdem den Timestamp des lokalen Status, um zu zeigen, dass der Zustand bestätigt wurde.
            // Dies ist für die *vereinfachte* Version nicht zwingend, aber gute Praxis.
            val confirmedStatus = LastSentLocationStatus(
                bssid = currentBssid,
                locationName = currentEffectiveLocationName,
                // locationIcon = currentEffectiveLocationIcon,
                timestamp = System.currentTimeMillis() // Update timestamp
            )
            lastStatusRepository.saveLastSentStatus(appContext, confirmedStatus)
            Log.d(TAG, "[FOCUS_WLAN_AN] Updated local status timestamp: $confirmedStatus")
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Updated local status timestamp (no Firestore send).")
            return Result.success()
        }
    }

    /**
     * Hilfsfunktion zum Aktualisieren von Firestore und dem lokalen Status.
     */
    private suspend fun updateFirestoreStatus(
        userId: String,
        locationName: String?,
        bssid: String?,
        isAtKnownLoc: Boolean
        // locationIcon: String? // Wenn du es in Firestore speichern willst
    ): Result {
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] updateFirestoreStatus CALLED with Name='$locationName', BSSID='$bssid'")
        val statusUpdateData = hashMapOf(
            "userId" to userId,
            "currentLocationName" to locationName,
            // "currentLocationIcon" to locationIcon,
            "currentBssid" to bssid,
            "isOnlineAtLocation" to isAtKnownLoc,
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection(USER_STATUS_COLLECTION).document(userId)
                .set(statusUpdateData)
                .await()
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Successfully updated Firestore. Saving local status.")
            Log.i(TAG, "[FOCUS_WLAN_AN] Successfully updated status in Firestore for user $userId: Name='$locationName', BSSID='$bssid'")

            // Nach erfolgreichem Senden den neuen Status lokal speichern
            val newLocalStatusToSave = LastSentLocationStatus(
                bssid = bssid,
                locationName = locationName,
                // locationIcon = locationIcon,
                timestamp = System.currentTimeMillis() // Wichtig für den Vergleich im nächsten Lauf
            )
            lastStatusRepository.saveLastSentStatus(appContext, newLocalStatusToSave)
            Log.d(TAG, "[FOCUS_WLAN_AN] Saved new status locally (after Firestore update): $newLocalStatusToSave")
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Saved new status locally: $newLocalStatusToSave")
            Result.success()

        } catch (e: Exception) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Error updating Firestore: ${e.message}. Retrying.")
            Log.e(TAG, "[FOCUS_WLAN_AN] Error updating status in Firestore for user $userId. Retrying.", e)
            Result.retry()
        }
    }
}
