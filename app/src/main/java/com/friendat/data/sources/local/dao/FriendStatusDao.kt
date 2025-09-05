package com.friendat.data.sources.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.friendat.data.model.FriendStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(friendStatus: FriendStatus) // Nimmt das FriendStatus-Objekt entgegen

    @Query("SELECT * FROM friend_received_status WHERE friendId = :friendId")
    fun getStatusForFriend(friendId: String): Flow<FriendStatus?>

    @Query("SELECT * FROM friend_received_status WHERE friendId IN (:friendIds)")
    fun getStatusesForFriends(friendIds: List<String>): Flow<List<FriendStatus>>

    @Query("SELECT * FROM friend_received_status")
    fun getAllFriendStatuses(): Flow<List<FriendStatus>>

    @Query("DELETE FROM friend_received_status WHERE friendId = :friendId")
    suspend fun delete(friendId: String)

    @Query("DELETE FROM friend_received_status")
    suspend fun clearAll()
}