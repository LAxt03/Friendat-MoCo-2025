package com.friendat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.firebase.FirestoreRepository
import com.friendat.model.database.entity.Friend
import com.friendat.model.repository.FriendRepository
import com.friendat.ui.theme.Primary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val localRepo: FriendRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            val onlineFriends = firestoreRepo.getFriendsOnline()
            val localFriends = localRepo.getAllFriends()

            localFriends.collect { localList ->
                val merged = onlineFriends.map { id ->
                    localList.find { it.id == id } ?: Friend(id, "Unbenannt", "default", 0xFF000000)
                }
                _friends.value = merged
            }
        }
    }

    fun addFriend(id: String, name: String) {
        viewModelScope.launch {
            firestoreRepo.addFriendOnline(id)
            localRepo.addFriend(
                Friend(id = id, name = name, iconName = "default", color = Primary.value.toLong())
            )
        }
    }
}
