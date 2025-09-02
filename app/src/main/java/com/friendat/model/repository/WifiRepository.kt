package com.friendat.model.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WifiRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun saveWifiNetwork(ssid: String, name: String, color: Long) {
        val uid = getUserId() ?: return
        try {
            val wifiMap = mapOf(
                "ssid" to ssid,
                "name" to name,
                "color" to color
            )
            db.collection("users")
                .document(uid)
                .collection("wifi")
                .document(ssid)
                .set(wifiMap)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getWifiNetworks(): List<Map<String, Any>> {
        val uid = getUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("wifi")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteWifiNetwork(ssid: String) {
        val uid = getUserId() ?: return
        try {
            db.collection("users")
                .document(uid)
                .collection("wifi")
                .document(ssid)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
