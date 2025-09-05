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
import com.friendat.data.model.WifiLocation // Importiere dein WifiLocation-Modell
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
    private val lastStatusRepository: LastStatusRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "LocationCheckWorker"
        const val USER_STATUS_COLLECTION = "user_status"
        const val DEFAULT_ICON_ID = "default_icon" // Standard-Icon, falls kein bekanntes WLAN
        const val DEFAULT_COLOR_HEX = "#CCCCCC"   // Standard-Farbe
        const val OFFLINE_ICON_ID = "ic_wifi_off" // Beispiel-Icon für Offline-Status
        const val OFFLINE_COLOR_HEX = "#808080"   // Beispiel-Farbe für Offline
    }

    override suspend fun doWork(): Result {
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Worker execution started.")
        Log.d(TAG, "[FOCUS_WLAN_AN] Worker execution started.")

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] No authenticated user.")
            Log.w(TAG, "[FOCUS_WLAN_AN] No authenticated user.")
            return Result.failure()
        }
        val userId = currentUser.uid
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User ID: $userId")
        Log.d(TAG, "[FOCUS_WLAN_AN] User ID: $userId")

        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] ACCESS_FINE_LOCATION permission not granted.")
            Log.e(TAG, "[FOCUS_WLAN_AN] ACCESS_FINE_LOCATION permission not granted.")
            // Sende "offline" Status mit Standard-Offline-Icon/Farbe
            return updateFirestoreStatus(userId, null, null, false, OFFLINE_ICON_ID, OFFLINE_COLOR_HEX)
        }

        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Attempting to get BSSID.")
        val currentBssid = WifiUtils.getCurrentBssid(appContext)
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Current BSSID: $currentBssid")
        Log.d(TAG, "[FOCUS_WLAN_AN] Current BSSID: $currentBssid")

        var currentEffectiveLocationName: String? = null
        var currentEffectiveIconId: String? = null      // <<< NEU
        var currentEffectiveColorHex: String? = null    // <<< NEU
        var isAtKnownLocation = false

        val currentRecognizedLocation: WifiLocation? = if (currentBssid != null) {
            try {
                val knownLocations = wifiLocationDao.getAllLocationsForUserSuspend(userId)
                Log.d(TAG, "[FOCUS_WLAN_AN] Found ${knownLocations.size} known locations for user $userId.")
                knownLocations.find { it.bssid.equals(currentBssid, ignoreCase = true) }
            } catch (e: Exception) {
                Log.e(TAG, "[FOCUS_WLAN_AN] Error fetching known locations for user $userId from DB.", e)
                return Result.retry()
            }
        } else {
            null
        }

        if (currentRecognizedLocation != null) {
            currentEffectiveLocationName = currentRecognizedLocation.name
            currentEffectiveIconId = currentRecognizedLocation.iconId    // <<< GESETZT
            currentEffectiveColorHex = currentRecognizedLocation.colorHex  // <<< GESETZT
            isAtKnownLocation = true
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User at known location: '${currentEffectiveLocationName}', Icon: $currentEffectiveIconId, Color: $currentEffectiveColorHex")
            Log.i(TAG, "[FOCUS_WLAN_AN] User at known location: '${currentEffectiveLocationName}' (BSSID: $currentBssid), Icon: $currentEffectiveIconId, Color: $currentEffectiveColorHex")
        } else {
            // Nicht an einem bekannten Ort ODER kein WLAN
            isAtKnownLocation = false // Explizit setzen
            if (currentBssid != null) {
                // Mit unbekanntem WLAN verbunden
                currentEffectiveLocationName = null // oder "Unknown Wi-Fi"
                currentEffectiveIconId = DEFAULT_ICON_ID
                currentEffectiveColorHex = DEFAULT_COLOR_HEX
                FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User on unknown WiFi. Using defaults.")
                Log.i(TAG, "[FOCUS_WLAN_AN] User on unknown WiFi (BSSID: $currentBssid). Location name null, Icon: $currentEffectiveIconId, Color: $currentEffectiveColorHex")
            } else {
                // Nicht mit irgendeinem WLAN verbunden
                currentEffectiveLocationName = null // Oder "Offline"
                currentEffectiveIconId = OFFLINE_ICON_ID
                currentEffectiveColorHex = OFFLINE_COLOR_HEX
                FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] User not connected to any WiFi. Using offline defaults.")
                Log.i(TAG, "[FOCUS_WLAN_AN] User not connected to any WiFi. Location name null, Icon: $currentEffectiveIconId, Color: $currentEffectiveColorHex")
            }
        }

        val lastSavedStatus = lastStatusRepository.getLastSentStatus(appContext)
        Log.d(TAG, "[FOCUS_WLAN_AN] Last locally saved status: $lastSavedStatus")
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Last locally saved status: $lastSavedStatus")

        // Erweiterter Vergleich, falls dein LastSentLocationStatus auch Icon/Farbe enthält
        // Fürs Erste vergleichen wir nur die Basisdaten. Wenn sich die Basisdaten ändern,
        // werden Icon/Farbe sowieso mit dem neuen Stand gesendet.
        // Wenn du willst, dass *nur* eine Icon/Farbänderung auch ein Update auslöst,
        // müsstest du LastSentLocationStatus erweitern und hier abfragen.
        val statusBaseContentHasChanged = lastSavedStatus == null ||
                lastSavedStatus.bssid != currentBssid ||
                lastSavedStatus.locationName != currentEffectiveLocationName
        // Optional: || lastSavedStatus.iconId != currentEffectiveIconId
        // Optional: || lastSavedStatus.colorHex != currentEffectiveColorHex

        // Wir senden immer, wenn sich der Basiszustand geändert hat,
        // ODER wenn der letzte gesendete Status null war (erster Lauf),
        // ODER wenn der aktuelle Zustand "bekannter Ort" ist und der letzte nicht (oder umgekehrt),
        // um sicherzustellen, dass Icon/Farbe korrekt sind, falls sie sich geändert haben,
        // ohne dass sich BSSID/Name geändert hat (z.B. Nutzer editiert Ort).
        // Eine noch robustere Prüfung wäre, auch Icon/Farbe im `lastSavedStatus` zu speichern und zu vergleichen.
        // Fürs Erste ist `statusBaseContentHasChanged` ein guter Indikator.
        // Wenn `isAtKnownLocation` sich ändert, impliziert das meist auch eine Icon/Farb-Änderung.

        val shouldUpdateFirestore = statusBaseContentHasChanged ||
                (isAtKnownLocation && lastSavedStatus?.locationName == null && currentEffectiveLocationName != null) || // Von unbekannt/offline zu bekannt
                (!isAtKnownLocation && lastSavedStatus?.locationName != null) // Von bekannt zu unbekannt/offline

        if (shouldUpdateFirestore) {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] CHANGE DETECTED or significant state difference. Updating Firestore.")
            Log.i(TAG, "[FOCUS_WLAN_AN] CHANGE DETECTED (or first run/significant difference). Updating Firestore.")
            Log.d(TAG, "[FOCUS_WLAN_AN] Old Local: BSSID=${lastSavedStatus?.bssid}, Name=${lastSavedStatus?.locationName}")
            Log.d(TAG, "[FOCUS_WLAN_AN] New State: BSSID=$currentBssid, Name=$currentEffectiveLocationName, Icon=$currentEffectiveIconId, Color=$currentEffectiveColorHex")

            return updateFirestoreStatus(
                userId,
                currentEffectiveLocationName,
                currentBssid,
                isAtKnownLocation,
                currentEffectiveIconId,    // <<< ÜBERGEBEN
                currentEffectiveColorHex   // <<< ÜBERGEBEN
            )
        } else {
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] NO significant CHANGE detected.")
            Log.i(TAG, "[FOCUS_WLAN_AN] NO significant CHANGE detected. Firestore update not strictly needed. Local timestamp will be updated.")
            val confirmedStatus = LastSentLocationStatus(
                bssid = currentBssid,
                locationName = currentEffectiveLocationName,
                // Wenn du Icon/Farbe im lokalen Status speichern und vergleichen willst, füge sie hier hinzu:
                // iconId = currentEffectiveIconId,
                // colorHex = currentEffectiveColorHex,
                timestamp = System.currentTimeMillis()
            )
            lastStatusRepository.saveLastSentStatus(appContext, confirmedStatus)
            Log.d(TAG, "[FOCUS_WLAN_AN] Updated local status timestamp: $confirmedStatus")
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Updated local status timestamp (no Firestore send).")
            return Result.success()
        }
    }

    private suspend fun updateFirestoreStatus(
        userId: String,
        locationName: String?,
        bssid: String?,
        isAtKnownLoc: Boolean,
        iconId: String?,      // <<< PARAMETER HINZUGEFÜGT
        colorHex: String?     // <<< PARAMETER HINZUGEFÜGT
    ): Result {
        FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] updateFirestoreStatus CALLED with Name='$locationName', BSSID='$bssid', Icon='$iconId', Color='$colorHex'")
        val statusUpdateData = hashMapOf<String, Any?>( // Any? um Null-Werte explizit zu erlauben
            // "userId" to userId, // Redundant, da es die Document ID ist, kann aber bleiben.
            "currentLocationName" to locationName,
            "currentBssid" to bssid,
            "isOnlineAtLocation" to isAtKnownLoc,
            "currentIconId" to iconId,          // <<< IN MAP HINZUGEFÜGT
            "currentColorHex" to colorHex,      // <<< IN MAP HINZUGEFÜGT
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection(USER_STATUS_COLLECTION).document(userId)
                .set(statusUpdateData) // .set überschreibt das Dokument komplett mit diesen Daten
                .await()
            FileLogger.logWorker(appContext, TAG, "[FOCUS_WLAN_AN] Successfully updated Firestore. Saving local status.")
            Log.i(TAG, "[FOCUS_WLAN_AN] Successfully updated status in Firestore for user $userId: Data=$statusUpdateData")

            val newLocalStatusToSave = LastSentLocationStatus(
                bssid = bssid,
                locationName = locationName,
                // iconId = iconId, // Optional hier speichern, wenn für Vergleich benötigt
                // colorHex = colorHex, // Optional hier speichern, wenn für Vergleich benötigt
                timestamp = System.currentTimeMillis()
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
