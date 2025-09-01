package com.friendat.model.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.friendat.model.database.entity.Friend
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Delete
    fun deleteLocation(entry : Friend)

    @Insert
    fun insertLocation(entry : Friend)

    @Update
    fun updateLocation(entry : Friend)

    @Query("SELECT * FROM Friend ORDER BY userName ASC")
    fun getAllLocations(): Flow<List<Friend>>

    @Query("SELECT * FROM Friend WHERE userName = :id LIMIT 1")
    suspend fun getLocationById(id: String): Friend?
}