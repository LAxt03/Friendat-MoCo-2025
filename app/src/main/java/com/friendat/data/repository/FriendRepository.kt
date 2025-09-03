package com.friendat.data.repository

import com.friendat.data.model.Friendship
import com.friendat.data.model.UserDisplayInfo
import kotlinx.coroutines.flow.Flow

interface FriendRepository {

    suspend fun findUserByEmail(email: String): Result<UserDisplayInfo?>


    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String): Result<Unit>


    fun getPendingFriendRequests(userId: String): Flow<Result<List<Friendship>>>


    fun getAcceptedFriends(userId: String): Flow<Result<List<Friendship>>>


    suspend fun acceptFriendRequest(friendshipId: String, currentUserId: String): Result<Unit>


    suspend fun declineFriendRequest(friendshipId: String, currentUserId: String): Result<Unit>


    suspend fun removeFriend(friendshipId: String, currentUserId: String): Result<Unit>


    suspend fun getUsersDisplayInfo(userIds: List<String>): Result<List<UserDisplayInfo>>
}
