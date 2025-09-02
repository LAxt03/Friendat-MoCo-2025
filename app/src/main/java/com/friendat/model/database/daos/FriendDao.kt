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
    suspend fun deleteFriend(entry : Friend)

    @Insert
    suspend fun insertFriend(entry : Friend)

    @Update
    suspend fun updateFriend(entry : Friend)

    @Query("SELECT * FROM Friend ORDER BY id ASC")
    fun getAllFriends(): Flow<List<Friend>>

    @Query("SELECT * FROM Friend WHERE id = :id LIMIT 1")
    suspend fun getFriendById(id: String): Friend?
}