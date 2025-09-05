package com.friendat.data.repository

import android.app.Application
import android.util.Log
import com.friendat.services.MyFirebaseMessagingService
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
    private val firestore: FirebaseFirestore,
    private val application: Application
) : AuthRepository {

    companion object {

        //Tag fürs Logging
        private const val TAG = "AuthRepositoryImpl"

        //Name der Collection in Firebase
        private const val USERS_COLLECTION = "users"
    }


    //Gibt aktuell eingeloggten User zurück
    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): UserAuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.let { user ->
                //Sicherstellung dass der User auch in Firestore existiert sonst wird er hinzugefügt oder aktualisiert. Beim Registieren kann ein Fehler auftreten.
                createOrUpdateUserDocumentInFirestore(user)
                Log.i(TAG, "Attempting to send pending FCM token for user ${user.uid} after sign in.")

                //Beim registrieren wird ein Token für Firebase Cloud Messaging (FCM) erstellt. Jedoch kann der Token nicht zugewiesen werden, wenn der Benutzer noch nicht eingeloggt ist.
                //Deswegen wird es zwischengespeichert und wenn man sich dann wirklich einloggt, wird das Token an den Server gesendet.
                MyFirebaseMessagingService.sendPendingTokenToServerIfNecessary(application, user.uid)
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

                //Erstellen des Users in Firestore
                createOrUpdateUserDocumentInFirestore(user)
                Log.i(TAG, "Attempting to send pending FCM token for user ${user.uid} after sign up.")

                //Beim registrieren wird ein Token für Firebase Cloud Messaging (FCM) erstellt. Jedoch kann der Token nicht zugewiesen werden, wenn der Benutzer noch nicht eingeloggt ist.
                MyFirebaseMessagingService.sendPendingTokenToServerIfNecessary(application, user.uid)
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

    //Hilfsfunktion um einen User zu erstellen oder zu aktualisieren in Firebase Collection
    private suspend fun createOrUpdateUserDocumentInFirestore(user: FirebaseUser) {
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
