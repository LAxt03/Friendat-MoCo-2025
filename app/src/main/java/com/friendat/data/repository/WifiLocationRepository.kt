package com.friendat.data.repository

import com.friendat.data.model.WifiLocation
import kotlinx.coroutines.flow.Flow


interface WifiLocationRepository {

    suspend fun addWifiLocation(wifiLocation: WifiLocation): Result<Unit>

    fun getWifiLocationsForCurrentUser(): Flow<Result<List<WifiLocation>>>

    suspend fun updateWifiLocation(wifiLocation: WifiLocation): Result<Unit>

    suspend fun deleteWifiLocation(locationId: String): Result<Unit>

    fun getWifiLocationsForCurrentUserFromRoom(): Flow<List<WifiLocation>>

}



