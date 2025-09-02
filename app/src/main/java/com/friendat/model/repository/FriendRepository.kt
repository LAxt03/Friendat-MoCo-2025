package com.friendat.model.repository

import com.friendat.model.database.daos.FriendDao
import com.friendat.model.database.entity.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FriendRepository @Inject constructor(
    private val friendDao: FriendDao
) {

     //Flow sorgt dafür, dass Änderungen automatisch vom UI beobachtet werden
    fun getAllFriends(): Flow<List<Friend>> = friendDao.getAllFriends()

    //Suspend sorgt dafür, dass Hauptthread nicht blockiert wird
    suspend fun addFriend(friend: Friend) {
        withContext(Dispatchers.IO) {
            friendDao.insertFriend(friend)
        }
    }


    suspend fun updateFriend(friend: Friend) {
        withContext(Dispatchers.IO) {
            friendDao.updateFriend(friend)
        }
    }


    suspend fun deleteFriend(friend: Friend) {
        withContext(Dispatchers.IO) {
            friendDao.deleteFriend(friend)
        }
    }


    suspend fun getFriendById(id: String): Friend? {
        return withContext(Dispatchers.IO) {
            friendDao.getFriendById(id)
        }
    }
}
