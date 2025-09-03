package com.friendat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await // Wichtig für Firebase-Aufrufe in Coroutines
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): UserAuthResult {
        return try {
            // Wartet auf das Ergebnis (Asynchron)
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()

            authResult.user?.let { user ->
                UserAuthResult.Success(user)
            } ?: UserAuthResult.Failure(Exception("Authentication erfolgreich, aber trotzdem Fehler")) // Seltener Fall
        } catch (e: Exception) {
            UserAuthResult.Failure(e)
        }
    }


    override suspend fun signUpWithEmailPassword(email: String, password: String): UserAuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

            authResult.user?.let { user ->
                UserAuthResult.Success(user)
            } ?: UserAuthResult.Failure(Exception("Registrierung erfolgreich, aber trotzdem Fehler")) // Seltener Fall
        } catch (e: Exception) {
            UserAuthResult.Failure(e)
        }
    }


    override suspend fun signOut(): AuthActionStatus {
        return try {
            firebaseAuth.signOut()
            AuthActionStatus.Success
        } catch (e: Exception) {
            AuthActionStatus.Failure(e)
        }
    }
}
