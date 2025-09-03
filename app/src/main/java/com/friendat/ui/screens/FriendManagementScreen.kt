package com.friendat.ui.friends // Oder dein entsprechendes UI-Paket

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.data.model.FriendshipStatus
import com.friendat.data.model.UserDisplayInfo
import com.friendat.ui.viewmodel.FriendScreenUiEvent
import com.friendat.ui.viewmodel.FriendViewModel
import com.friendat.ui.viewmodel.FriendshipWithDisplayInfo
import com.friendat.ui.viewmodel.FriendsListUiState
import com.friendat.ui.viewmodel.UserSearchResultUiState

@Composable
fun FriendManagementScreen(
    navController: NavController,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val searchState by viewModel.userSearchResultUiState.collectAsState()
    val friendsListState by viewModel.friendsListUiState.collectAsState()

    // Für die Snackbar-Anzeige von Fehlern oder Erfolgsmeldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // Globale Fehlermeldung aus friendsListState anzeigen
    LaunchedEffect(friendsListState.errorMessage) {
        friendsListState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            // Optional: Fehler im ViewModel zurücksetzen, nachdem er angezeigt wurde
        }
    }
    // Erfolgsmeldung für gesendete Anfrage
    LaunchedEffect(searchState.requestSentMessage) {
        searchState.requestSentMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            // Optional: Nachricht im ViewModel zurücksetzen
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { UserSearchSection(searchState, viewModel::onEvent) }
            item { Divider() }
            item { PendingRequestsSection(friendsListState, viewModel::onEvent) }
            item { Divider() }
            item { AcceptedFriendsSection(friendsListState, viewModel::onEvent) }
        }
    }
}

@Composable
fun UserSearchSection(
    searchState: UserSearchResultUiState,
    onEvent: (FriendScreenUiEvent) -> Unit
) {
    var emailInput by remember { mutableStateOf(searchState.searchInput) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Search User by Email", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = searchState.errorMessage?.contains("Email") == true || searchState.errorMessage?.contains("not found") == true
        )
        if (searchState.isLoading && searchState.searchResultUser == null) { // Ladeindikator nur für die Suche
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        searchState.errorMessage?.let {
            if (!it.startsWith("Friend request sent") && !it.startsWith("Failed to send")) { // Fehler, die nicht direkt das Senden betreffen
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { onEvent(FriendScreenUiEvent.SearchUserByEmail(emailInput)) }) {
                Text("Search")
            }
            if (searchState.searchResultUser != null || searchState.searchInput.isNotEmpty()) {
                Button(onClick = {
                    emailInput = "" // Textfeld auch leeren
                    onEvent(FriendScreenUiEvent.ClearSearch)
                }) {
                    Text("Clear")
                }
            }
        }


        searchState.searchResultUser?.let { user ->
            if (user.uid.isNotBlank()) { // Nur anzeigen, wenn eine gültige UID vorhanden ist
                Spacer(modifier = Modifier.height(8.dp))
                Text("Search Result:", style = MaterialTheme.typography.titleSmall)
                UserDisplayCard(user = user) {
                    // Button zum Senden der Anfrage
                    Button(
                        onClick = { onEvent(FriendScreenUiEvent.SendFriendRequest(user.uid)) },
                        enabled = !searchState.isLoading // Deaktivieren während des Sendens
                    ) {
                        if (searchState.isLoading && searchState.requestSentMessage == null) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Request")
                        }
                    }
                }
                searchState.errorMessage?.let {
                    if(it.startsWith("Failed to send") || it.startsWith("Cannot send")) { // Fehler, die das Senden betreffen
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PendingRequestsSection(
    friendsListState: FriendsListUiState,
    onEvent: (FriendScreenUiEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pending Friend Requests", style = MaterialTheme.typography.titleMedium)
        if (friendsListState.isLoadingPending) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (friendsListState.pendingRequests.isEmpty() && !friendsListState.isLoadingPending) {
            Text("No pending requests.")
        }
        friendsListState.pendingRequests.forEach { friendshipWithInfo ->
            FriendshipCard(
                friendshipWithInfo = friendshipWithInfo,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onEvent(FriendScreenUiEvent.AcceptFriendRequest(friendshipWithInfo.friendship.id)) }) {
                            Text("Accept")
                        }
                        Button(onClick = { onEvent(FriendScreenUiEvent.DeclineFriendRequest(friendshipWithInfo.friendship.id)) }) {
                            Text("Decline")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AcceptedFriendsSection(
    friendsListState: FriendsListUiState,
    onEvent: (FriendScreenUiEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your Friends", style = MaterialTheme.typography.titleMedium)
        if (friendsListState.isLoadingFriends) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (friendsListState.acceptedFriends.isEmpty() && !friendsListState.isLoadingFriends) {
            Text("You have no friends yet.")
        }
        friendsListState.acceptedFriends.forEach { friendshipWithInfo ->
            FriendshipCard(
                friendshipWithInfo = friendshipWithInfo,
                actions = {
                    Button(onClick = { onEvent(FriendScreenUiEvent.RemoveFriend(friendshipWithInfo.friendship.id)) }) {
                        Text("Remove")
                    }
                }
            )
        }
    }
}

@Composable
fun UserDisplayCard(user: UserDisplayInfo, actionContent: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(user.displayName ?: "No Name", fontWeight = FontWeight.Bold)
                Text(user.email ?: "No Email", fontSize = 14.sp)
                Text("UID: ${user.uid}", fontSize = 12.sp, style = MaterialTheme.typography.bodySmall)
            }
            actionContent()
        }
    }
}

@Composable
fun FriendshipCard(
    friendshipWithInfo: FriendshipWithDisplayInfo,
    actions: @Composable () -> Unit
) {
    val friendInfo = friendshipWithInfo.friendDisplayInfo
    val friendship = friendshipWithInfo.friendship

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (friendInfo != null) {
                Text(friendInfo.displayName ?: "Loading Name...", fontWeight = FontWeight.Bold)
                Text(friendInfo.email ?: "Loading Email...", fontSize = 14.sp)
                Text("UID: ${friendInfo.uid}", fontSize = 12.sp, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Friend Info loading...", style = MaterialTheme.typography.bodyMedium)
                Text("Friend UID (from friendship): ${
                    friendship.participants.firstOrNull { it != /* TODO: Get current user ID here or pass it */ "" } ?: friendship.requesterId
                }", fontSize = 12.sp, style = MaterialTheme.typography.bodySmall)

            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Status: ${friendship.status}",
                fontSize = 12.sp,
                color = if (friendship.status == FriendshipStatus.PENDING.name) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            if (friendship.status == FriendshipStatus.PENDING.name) {
                Text("Requested by: ${friendship.requesterId}", fontSize = 10.sp) // TODO: Hier könnte man den Namen des Requesters anzeigen, falls bekannt
            }
            Text("Friendship ID: ${friendship.id}", fontSize = 10.sp, style = MaterialTheme.typography.bodySmall)


            Spacer(modifier = Modifier.height(8.dp))
            actions()
        }
    }
}

