package com.friendat.data.repository

import android.util.Log // Import für Log hinzugefügt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore // Bereits vorhanden, gut!
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl" // Für Logging
        private const val USERS_COLLECTION = "users" // Name deiner Firestore-Sammlung für Nutzer
    }

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): UserAuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.let { user ->
                // Nach erfolgreichem Login, Nutzerdokument in Firestore erstellen/aktualisieren
                createOrUpdateUserDocumentInFirestore(user)
                UserAuthResult.Success(user)
            } ?: UserAuthResult.Failure(Exception("Authentication successful, but user is null"))
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmailPassword failed", e)
            UserAuthResult.Failure(e)
        }
    }

    override suspend fun signUpWithEmailPassword(email: String, password: String): UserAuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            authResult.user?.let { user ->
                // Nach erfolgreicher Registrierung, Nutzerdokument in Firestore erstellen/aktualisieren
                createOrUpdateUserDocumentInFirestore(user)
                UserAuthResult.Success(user)
            } ?: UserAuthResult.Failure(Exception("Registration successful, but user is null"))
        } catch (e: Exception) {
            Log.e(TAG, "signUpWithEmailPassword failed", e)
            UserAuthResult.Failure(e)
        }
    }

    override suspend fun signOut(): AuthActionStatus {
        return try {
            firebaseAuth.signOut()
            AuthActionStatus.Success
        } catch (e: Exception) {
            Log.e(TAG, "signOut failed", e)
            AuthActionStatus.Failure(e)
        }
    }


    private suspend fun createOrUpdateUserDocumentInFirestore(user: FirebaseUser) {
        // Die Dokument-ID in der 'users'-Sammlung ist die UID des Firebase-Nutzers
        val userDocRef = firestore.collection(USERS_COLLECTION).document(user.uid)

        val userData = hashMapOf(
            "uid" to user.uid,
            "email" to user.email,
            "displayName" to user.displayName,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {

            userDocRef.set(userData, SetOptions.merge()).await()
            Log.d(TAG, "User document created/updated in Firestore for UID: ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating user document in Firestore for UID: ${user.uid}", e)

        }
    }


}
