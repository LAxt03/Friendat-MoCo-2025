package com.friendat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.data.model.FriendStatus
import com.friendat.data.model.UserDisplayInfo
import com.friendat.data.repository.AuthRepository
import com.friendat.data.repository.FriendRepository
import com.friendat.data.sources.local.dao.FriendStatusDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class FriendWithLiveStatus(
    val displayInfo: UserDisplayInfo, // Name, Profilbild etc. des Freundes
    val liveStatus: FriendStatus?     // Der aktuelle Live-Status (kann null sein, wenn offline oder kein Status)
)

data class FriendLiveStatusUiState(
    val friendsWithStatus: List<FriendWithLiveStatus> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FriendLiveStatusViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val friendStatusDao: FriendStatusDao, // Injiziere deinen DAO
    private val authRepository: AuthRepository
) : ViewModel() {

    private var currentUserId: String? = null

    // StateFlow für den kombinierten UI-Zustand
    val uiState: StateFlow<FriendLiveStatusUiState> = flow {

        // Zuerst die aktuelle UserID holen
        currentUserId = authRepository.getCurrentUser()?.uid
        if (currentUserId == null) {
            emit(FriendLiveStatusUiState(isLoading = false, errorMessage = "User not logged in"))
            return@flow
        }

        // Flow für akzeptierte Freunde (Liste von UserDisplayInfo)
        val acceptedFriendsFlow: Flow<List<UserDisplayInfo>> =
            friendRepository.getAcceptedFriends(currentUserId!!) // Ruft Flow<Result<List<Friendship>>>
                .map { result ->
                    result.getOrNull()?.mapNotNull { friendship ->
                        // Extrahiere die ID des Freundes
                        val friendUid = friendship.participants.firstOrNull { it != currentUserId }
                        friendUid
                    } ?: emptyList()
                }
                .transformLatest { friendIds -> // Hole DisplayInfos für diese IDs
                    if (friendIds.isNotEmpty()) {
                        val displayInfoResult = friendRepository.getUsersDisplayInfo(friendIds)
                        emit(displayInfoResult.getOrNull() ?: emptyList())
                    } else {
                        emit(emptyList())
                    }
                }

        // Flow für alle Live-Status-Updates
        val allLiveStatusesFlow: Flow<Map<String, FriendStatus>> =
            friendStatusDao.getAllFriendStatuses()
                .map { statusList -> statusList.associateBy { it.friendId } }


        // Kombiniere die beiden Flows
        combine(acceptedFriendsFlow, allLiveStatusesFlow) { friends, statuses ->
            val friendsWithLiveStatus = friends.map { friendDisplayInfo ->
                FriendWithLiveStatus(
                    displayInfo = friendDisplayInfo,
                    liveStatus = statuses[friendDisplayInfo.uid]
                )
            }
            FriendLiveStatusUiState(friendsWithStatus = friendsWithLiveStatus, isLoading = false)
        }
            .catch { e -> // Fehlerbehandlung für den kombinierten Flow
                emit(FriendLiveStatusUiState(isLoading = false, errorMessage = e.message ?: "Error loading live statuses"))
            }
            .collect { emit(it) } // Gib den kombinierten UI-Zustand weiter
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = FriendLiveStatusUiState(isLoading = true)
        )
}

