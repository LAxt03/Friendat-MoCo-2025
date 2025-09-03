package com.friendat.model.repository

import com.friendat.model.database.daos.WifiLocationDao
import com.friendat.model.database.entity.WifiLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiRepository @Inject constructor(
    private val wifiDao: WifiLocationDao, // lokal
    private val auth: FirebaseAuth,      // online
    private val db: FirebaseFirestore
) {

    fun getAllLocations(): Flow<List<WifiLocation>> = wifiDao.getAllLocations()

    suspend fun addLocation(location: WifiLocation) {
        wifiDao.insertLocation(location)
        saveLocationOnline(location)
    }

    suspend fun updateLocation(location: WifiLocation) {
        wifiDao.updateLocation(location)
        saveLocationOnline(location)
    }

    suspend fun deleteLocation(location: WifiLocation) {
        wifiDao.deleteLocation(location)
        deleteLocationOnline(location)
    }

    suspend fun getLocationById(id: Int): WifiLocation? = wifiDao.getLocationById(id)

    private fun getUserId(): String? = auth.currentUser?.uid

    private suspend fun saveLocationOnline(location: WifiLocation) {
        val uid = getUserId() ?: return
        try {
            val data = mapOf(
                "ssid" to location.ssid,
                "locationName" to location.locationName,
                "iconName" to location.iconName,
                "color" to location.color
            )
            db.collection("users")
                .document(uid)
                .collection("wifi")
                .document(location.ssid)
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun deleteLocationOnline(location: WifiLocation) {
        val uid = getUserId() ?: return
        try {
            db.collection("users")
                .document(uid)
                .collection("wifi")
                .document(location.ssid)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getLocationsOnline(): List<WifiLocation> {
        val uid = getUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("wifi")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val ssid = doc.getString("ssid") ?: return@mapNotNull null
                val locationName = doc.getString("locationName") ?: "Unbenannt"
                val iconName = doc.getString("iconName") ?: "default"
                val color = doc.getLong("color") ?: 0xFF000000
                WifiLocation(ssid, locationName, iconName, color, id = 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
