package com.friendat.data.sources.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.friendat.data.model.WifiLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiLocationDao {


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(wifiLocation: WifiLocation)


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(wifiLocations: List<WifiLocation>)

    @Update
    suspend fun update(wifiLocation: WifiLocation)


    @Delete
    suspend fun delete(wifiLocation: WifiLocation)

    @Query("SELECT * FROM wifi_locations WHERE id = :id AND userId = :userId")
    suspend fun getById(id: String, userId: String): WifiLocation?


    @Query("SELECT * FROM wifi_locations WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllLocationsForUserSuspend(userId: String): List<WifiLocation>


    @Query("SELECT * FROM wifi_locations WHERE userId = :userId ORDER BY name ASC")
    fun getAllLocationsForUserFlow(userId: String): Flow<List<WifiLocation>>


    @Query("DELETE FROM wifi_locations WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)


    @Query("DELETE FROM wifi_locations")
    suspend fun deleteAll() // Sei vorsichtig mit dieser Funktion
}