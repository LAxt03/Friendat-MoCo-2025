package com.friendat.data.repository

import com.friendat.data.model.WifiLocation
import com.friendat.data.sources.local.dao.WifiLocationDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

//Collection in Firebase
private const val WIFI_LOCATIONS_COLLECTION = "userWifiLocations"

@Singleton
class WifiLocationRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val wifiLocationDao: WifiLocationDao // <-- DAO hier injizieren
) : WifiLocationRepository {

    override suspend fun addWifiLocation(wifiLocation: WifiLocation): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User nicht angemeldet."))
            }


            val userId = currentUser.uid
            val locationForFirestore = wifiLocation.copy(
                userId = userId,
                id = "" //Wird von firestore generiert und wird dann auch für room verwendet

            )

            // In Firestore speichern und die generierte ID holen
            val documentReference = firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .add(locationForFirestore) // .add() generiert eine neue ID
                .await()
            Log.d("WifiLocationRepo", "Location added to Firestore with ID: ${documentReference.id}")


            val finalLocationToSave = wifiLocation.copy(
                userId = userId,
                id = documentReference.id // Firestore ID verwenden
            )
            wifiLocationDao.insert(finalLocationToSave)
            Log.d("WifiLocationRepo", "Location added to Room with ID: ${finalLocationToSave.id}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WifiLocationRepo", "Error in addWifiLocation", e)
            Result.failure(e)
        }
    }

        //Worker verwendet Room Locations dies holt aber die WifiLocations aus der Firebase
    override fun getWifiLocationsForCurrentUser(): Flow<Result<List<WifiLocation>>> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("User nicht angemeldet.")))
        }

        val query = firestore.collection(WIFI_LOCATIONS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("name", Query.Direction.ASCENDING)

        return query.snapshots().mapNotNull { snapshot ->
            try {
                val locations = snapshot.documents.mapNotNull { document ->
                    document.toObject<WifiLocation>()?.copy(id = document.id)
                }
                Result.success(locations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getWifiLocationsForCurrentUserFromRoom(): Flow<List<WifiLocation>> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.w("WifiLocationRepo", "getWifiLocationsForCurrentUserFromRoom: User not logged in, returning empty flow.")
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val userId = currentUser.uid
        Log.d("WifiLocationRepo", "getWifiLocationsForCurrentUserFromRoom: Fetching from DAO for user $userId")
        return wifiLocationDao.getAllLocationsForUserFlow(userId)
    }

    override suspend fun updateWifiLocation(wifiLocation: WifiLocation): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null || currentUser.uid != wifiLocation.userId) {
                return Result.failure(Exception("User nicht eingeloggt oder falscher User"))
            }
            if (wifiLocation.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Location ID ist leer für Update"))
            }

            // In Firestore aktualisieren
            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .document(wifiLocation.id) // Verwende die übergebene ID
                .set(wifiLocation) // .set() überschreibt das Dokument
                .await()
            Log.d("WifiLocationRepo", "Location updated in Firestore: ${wifiLocation.id}")

            // In Room aktualisieren
            wifiLocationDao.update(wifiLocation) // Annahme: Dao hat eine update-Methode
            Log.d("WifiLocationRepo", "Location updated in Room: ${wifiLocation.id}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WifiLocationRepo", "Error in updateWifiLocation", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteWifiLocation(locationId: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User nicht eingeloggt"))
            }
            if (locationId.isBlank()) {
                return Result.failure(IllegalArgumentException("Location ID ist leer für Delete"))
            }

            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .document(locationId)
                .delete()
                .await()
            Log.d("WifiLocationRepo", "Location deleted from Firestore: $locationId")

            val locationToDeleteFromRoom = WifiLocation(id = locationId, userId = currentUser.uid, name = "", bssid = "", iconId = "", colorHex = "") // Dummy-Objekt nur mit ID für @Delete
            wifiLocationDao.delete(locationToDeleteFromRoom) // Oder besser: wifiLocationDao.deleteById(locationId, currentUser.uid)
            Log.d("WifiLocationRepo", "Location deleted from Room: $locationId")


            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WifiLocationRepo", "Error in deleteWifiLocation", e)
            Result.failure(e)
        }
    }
}

