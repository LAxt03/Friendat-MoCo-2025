package com.friendat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.data.model.Friendship
import com.friendat.data.model.UserDisplayInfo
import com.friendat.data.repository.AuthRepository
import com.friendat.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sealed Interface für UI-Ereignisse/Aktionen, die vom ViewModel verarbeitet werden
sealed interface FriendScreenUiEvent {
    data class SearchUserByEmail(val email: String) : FriendScreenUiEvent
    data class SendFriendRequest(val targetUserId: String) : FriendScreenUiEvent
    data class AcceptFriendRequest(val friendshipId: String) : FriendScreenUiEvent
    data class DeclineFriendRequest(val friendshipId: String) : FriendScreenUiEvent
    data class RemoveFriend(val friendshipId: String) : FriendScreenUiEvent
    object ClearSearch : FriendScreenUiEvent
}

// Datenklasse für den Zustand der Suchergebnisse
data class UserSearchResultUiState(
    val searchInput: String = "",
    val searchResultUser: UserDisplayInfo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val requestSentMessage: String? = null // Für Feedback nach dem Senden einer Anfrage
)

// Datenklasse für den Zustand der Freundeslisten und Anfragen
data class FriendsListUiState(
    val pendingRequests: List<FriendshipWithDisplayInfo> = emptyList(),
    val acceptedFriends: List<FriendshipWithDisplayInfo> = emptyList(),
    val isLoadingPending: Boolean = true,
    val isLoadingFriends: Boolean = true,
    val errorMessage: String? = null
)

// Kombinierte Daten für die UI (Freundschaft + Nutzerinfos des Freundes)
data class FriendshipWithDisplayInfo(
    val friendship: Friendship,
    val friendDisplayInfo: UserDisplayInfo? // Null, wenn Infos noch nicht geladen
)


@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository // Um die currentUserId zu erhalten
) : ViewModel() {

    private val _userSearchResultUiState = MutableStateFlow(UserSearchResultUiState())
    val userSearchResultUiState: StateFlow<UserSearchResultUiState> = _userSearchResultUiState.asStateFlow()

    private val _friendsListUiState = MutableStateFlow(FriendsListUiState())
    val friendsListUiState: StateFlow<FriendsListUiState> = _friendsListUiState.asStateFlow()


    // Aktuelle UserID vom AuthRepository holen (einmalig oder als Flow, wenn sie sich ändern könnte)
    // Für dieses ViewModel nehmen wir an, dass sie sich während der Lebenszeit des ViewModels nicht ändert
    // oder wir holen sie bei Bedarf. Für Flows wäre es etwas komplexer.
    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUser()?.uid // Einfache Abfrage bei Init
            currentUserId?.let { userId ->
                observePendingRequests(userId)
                observeAcceptedFriends(userId)
            }
        }
    }

    fun onEvent(event: FriendScreenUiEvent) {
        when (event) {
            is FriendScreenUiEvent.SearchUserByEmail -> searchUser(event.email)
            is FriendScreenUiEvent.SendFriendRequest -> sendFriendRequestToTarget(event.targetUserId)
            is FriendScreenUiEvent.AcceptFriendRequest -> acceptRequest(event.friendshipId)
            is FriendScreenUiEvent.DeclineFriendRequest -> declineRequest(event.friendshipId)
            is FriendScreenUiEvent.RemoveFriend -> removeExistingFriend(event.friendshipId)
            FriendScreenUiEvent.ClearSearch -> clearSearchResults()
        }
    }

    private fun searchUser(email: String) {
        if (email.isBlank()) {
            _userSearchResultUiState.value = UserSearchResultUiState(errorMessage = "Email cannot be blank.")
            return
        }
        _userSearchResultUiState.value = UserSearchResultUiState(searchInput = email, isLoading = true)
        viewModelScope.launch {
            val result = friendRepository.findUserByEmail(email)
            result.fold(
                onSuccess = { foundUser ->
                    _userSearchResultUiState.value = _userSearchResultUiState.value.copy(
                        searchResultUser = foundUser,
                        isLoading = false,
                        errorMessage = if (foundUser == null) "User not found." else null,
                        requestSentMessage = null
                    )
                },
                onFailure = { error ->
                    _userSearchResultUiState.value = _userSearchResultUiState.value.copy(
                        searchResultUser = null,
                        isLoading = false,
                        errorMessage = error.message ?: "Error searching user.",
                        requestSentMessage = null
                    )
                }
            )
        }
    }

    private fun clearSearchResults() {
        _userSearchResultUiState.value = UserSearchResultUiState()
    }

    private fun sendFriendRequestToTarget(targetUserId: String) {
        val localCurrentUserId = currentUserId
        if (localCurrentUserId == null) {
            _userSearchResultUiState.value = _userSearchResultUiState.value.copy(errorMessage = "User not logged in.")
            return
        }
        if (targetUserId == localCurrentUserId) {
            _userSearchResultUiState.value = _userSearchResultUiState.value.copy(errorMessage = "Cannot send request to yourself.")
            return
        }

        _userSearchResultUiState.value = _userSearchResultUiState.value.copy(isLoading = true) // Zeige Ladeindikator während des Sendens
        viewModelScope.launch {
            val result = friendRepository.sendFriendRequest(localCurrentUserId, targetUserId)
            result.fold(
                onSuccess = {
                    _userSearchResultUiState.value = _userSearchResultUiState.value.copy(
                        requestSentMessage = "Friend request sent!",
                        errorMessage = null,
                        isLoading = false
                        // Optional: Suchergebnis leeren oder Zustand anpassen
                    )
                },
                onFailure = { error ->
                    _userSearchResultUiState.value = _userSearchResultUiState.value.copy(
                        requestSentMessage = null,
                        errorMessage = error.message ?: "Failed to send request.",
                        isLoading = false
                    )
                }
            )
        }
    }

    private fun observePendingRequests(userId: String) {
        viewModelScope.launch {
            friendRepository.getPendingFriendRequests(userId)
                .combine(getDisplayInfoFlowForFriendships()) { result, displayInfoMap ->
                    result.map { friendships ->
                        friendships.map { friendship ->
                            val friendUid = friendship.participants.firstOrNull { it != userId } ?: friendship.requesterId
                            FriendshipWithDisplayInfo(friendship, displayInfoMap[friendUid])
                        }
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { pendingList ->
                            _friendsListUiState.value = _friendsListUiState.value.copy(
                                pendingRequests = pendingList,
                                isLoadingPending = false,
                                errorMessage = null
                            )
                        },
                        onFailure = { error ->
                            _friendsListUiState.value = _friendsListUiState.value.copy(
                                isLoadingPending = false,
                                errorMessage = error.message ?: "Error fetching pending requests."
                            )
                        }
                    )
                }
        }
    }

    private fun observeAcceptedFriends(userId: String) {
        viewModelScope.launch {
            friendRepository.getAcceptedFriends(userId)
                .combine(getDisplayInfoFlowForFriendships()) { result, displayInfoMap ->
                    result.map { friendships ->
                        friendships.map { friendship ->
                            val friendUid = friendship.participants.firstOrNull { it != userId }
                                ?: if (friendship.participants.isNotEmpty()) friendship.participants.first() else "" // Fallback, sollte nicht passieren
                            FriendshipWithDisplayInfo(friendship, displayInfoMap[friendUid])
                        }
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { acceptedList ->
                            _friendsListUiState.value = _friendsListUiState.value.copy(
                                acceptedFriends = acceptedList,
                                isLoadingFriends = false,
                                errorMessage = null
                            )
                        },
                        onFailure = { error ->
                            _friendsListUiState.value = _friendsListUiState.value.copy(
                                isLoadingFriends = false,
                                errorMessage = error.message ?: "Error fetching accepted friends."
                            )
                        }
                    )
                }
        }
    }

    // Hilfs-Flow, um DisplayInfos für alle relevanten Nutzer in den Freundschaftslisten zu sammeln
    // und nur einmal abzurufen. Dies ist eine Optimierung.
    private fun getDisplayInfoFlowForFriendships(): StateFlow<Map<String, UserDisplayInfo>> {
        // Dieser Flow kombiniert die User-IDs aus PENDING und ACCEPTED Listen,
        // holt deren DisplayInfos und stellt sie als Map bereit.
        // stateIn wird verwendet, um die Abfrage zu teilen und mehrfach Aufrufe zu vermeiden.
        return combine(
            friendRepository.getPendingFriendRequests(currentUserId ?: ""),
            friendRepository.getAcceptedFriends(currentUserId ?: "")
        ) { pendingResult, acceptedResult ->
            val userIds = mutableSetOf<String>()
            pendingResult.getOrNull()?.forEach { friendship ->
                userIds.addAll(friendship.participants)
            }
            acceptedResult.getOrNull()?.forEach { friendship ->
                userIds.addAll(friendship.participants)
            }
            // Entferne die aktuelle UserID, da wir Infos über die *anderen* Nutzer wollen
            currentUserId?.let { userIds.remove(it) }
            userIds.toList()
        }
            .transformLatest { ids -> // transformLatest bricht alte Abfragen ab, wenn neue IDs kommen
                if (ids.isNotEmpty()) {
                    emit(friendRepository.getUsersDisplayInfo(ids).getOrNull()?.associateBy { it.uid } ?: emptyMap())
                } else {
                    emit(emptyMap<String, UserDisplayInfo>())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Startet, wenn abonniert, stoppt nach 5s Inaktivität
                initialValue = emptyMap()
            )
    }


    private fun acceptRequest(friendshipId: String) {
        val localCurrentUserId = currentUserId ?: return // Frühzeitiger Ausstieg, wenn keine UserID
        viewModelScope.launch {
            // Optional: Zeige einen Ladeindikator für diese spezifische Anfrage in der UI
            val result = friendRepository.acceptFriendRequest(friendshipId, localCurrentUserId)
            result.onFailure { error ->
                // Fehlerbehandlung: Zeige Snackbar/Toast
                _friendsListUiState.value = _friendsListUiState.value.copy(errorMessage = error.message ?: "Failed to accept request.")
            }
            // Bei Erfolg aktualisiert der Flow für pendingRequests/acceptedFriends die Liste automatisch.
        }
    }

    private fun declineRequest(friendshipId: String) {
        val localCurrentUserId = currentUserId ?: return
        viewModelScope.launch {
            val result = friendRepository.declineFriendRequest(friendshipId, localCurrentUserId)
            result.onFailure { error ->
                _friendsListUiState.value = _friendsListUiState.value.copy(errorMessage = error.message ?: "Failed to decline request.")
            }
        }
    }

    private fun removeExistingFriend(friendshipId: String) {
        val localCurrentUserId = currentUserId ?: return
        viewModelScope.launch {
            val result = friendRepository.removeFriend(friendshipId, localCurrentUserId)
            result.onFailure { error ->
                _friendsListUiState.value = _friendsListUiState.value.copy(errorMessage = error.message ?: "Failed to remove friend.")
            }
        }
    }
}

