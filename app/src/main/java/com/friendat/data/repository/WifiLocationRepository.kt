package com.friendat.data.repository

import com.friendat.data.model.WifiLocation // Dein Datenmodell
import kotlinx.coroutines.flow.Flow // Für reaktive Datenströme von Firestore


interface WifiLocationRepository {

    suspend fun addWifiLocation(wifiLocation: WifiLocation): Result<Unit>

    fun getWifiLocationsForCurrentUser(): Flow<Result<List<WifiLocation>>>

    suspend fun updateWifiLocation(wifiLocation: WifiLocation): Result<Unit>

    suspend fun deleteWifiLocation(locationId: String): Result<Unit>

    fun getWifiLocationsForCurrentUserFromRoom(): Flow<List<WifiLocation>>

}



