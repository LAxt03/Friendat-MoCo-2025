package com.friendat.model.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.friendat.model.database.entity.WifiLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiLocationDao {
    @Delete
    fun deleteLocation(entry : WifiLocation)

    @Insert
    fun insertLocation(entry : WifiLocation)

    @Update
    fun updateLocation(entry : WifiLocation)

    @Query("SELECT * FROM WifiLocation ORDER BY location_name ASC")
    fun getAllLocations(): Flow<List<WifiLocation>>

    @Query("SELECT * FROM WifiLocation WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: Int): WifiLocation?
}