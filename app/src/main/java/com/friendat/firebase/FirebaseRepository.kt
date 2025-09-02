package com.friendat.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val auth: FirebaseAuth
) {

    suspend fun signInAnonymously(): FirebaseUser? {
        return try {

            // Prüfen ob bereits ein Nutzer existiert
            auth.currentUser ?: run {
                val result = auth.signInAnonymously().await()
                result.user
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }
}
