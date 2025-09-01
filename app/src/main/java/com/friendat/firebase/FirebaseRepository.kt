package com.friendat.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Anonyme Anmeldung
    fun loginAnonymously(onSuccess: (String) -> Unit) {
        auth.signInAnonymously().addOnSuccessListener {
            val uid = it.user?.uid ?: ""
            onSuccess(uid)
        }
    }

    // Aktuelles WLAN speichern
    fun updateCurrentNetwork(ssid: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf("ssid" to ssid), SetOptions.merge())
    }
}
