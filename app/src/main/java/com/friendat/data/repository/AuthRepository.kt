package com.friendat.data.repository

import com.google.firebase.auth.FirebaseUser


//Für Logout / Reset Password
//Reset Password noch nicht implementiert, Email bestätigung auch nicht (wäre dafür sehr gut geeignet)
sealed class AuthActionStatus {
    object Success : AuthActionStatus()
    data class Failure(val exception: Exception) : AuthActionStatus()
}

//Für Login / SignUp
sealed class UserAuthResult {
    data class Success(val user: FirebaseUser) : UserAuthResult()
    data class Failure(val exception: Exception) : UserAuthResult()
}


interface AuthRepository {


    fun getCurrentUser(): FirebaseUser?

    suspend fun signInWithEmailPassword(email: String, password: String): UserAuthResult

    suspend fun signUpWithEmailPassword(email: String, password: String): UserAuthResult

    suspend fun signOut(): AuthActionStatus

}
