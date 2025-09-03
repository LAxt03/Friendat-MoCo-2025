package com.friendat.data.repository

import com.friendat.data.model.WifiLocation
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

// Name der Firestore-Collection für WLAN-Standorte
private const val WIFI_LOCATIONS_COLLECTION = "userWifiLocations"

@Singleton
class WifiLocationRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : WifiLocationRepository {

    override suspend fun addWifiLocation(wifiLocation: WifiLocation): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User nicht angemeldet."))
            }

            val locationToAdd = wifiLocation.copy(userId = currentUser.uid)


            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .add(locationToAdd)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWifiLocationsForCurrentUser(): Flow<Result<List<WifiLocation>>> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("User nicht angemeldet.")))
        }


        val query = firestore.collection(WIFI_LOCATIONS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)


        return query.snapshots().mapNotNull { snapshot ->
            try {

                val locations = snapshot.documents.mapNotNull { document ->
                    document.toObject<WifiLocation>()
                }
                Result.success(locations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateWifiLocation(wifiLocation: WifiLocation): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null || currentUser.uid != wifiLocation.userId) {
                return Result.failure(Exception("User nicht eingeloggt"))
            }
            if (wifiLocation.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Location ID ist leer"))
            }


            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .document(wifiLocation.id)
                .set(wifiLocation)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
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
                return Result.failure(IllegalArgumentException("Location ID ist leer"))
            }

            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .document(locationId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


