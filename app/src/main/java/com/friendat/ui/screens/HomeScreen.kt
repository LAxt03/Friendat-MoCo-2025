package com.friendat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.model.database.entity.Friend
import com.friendat.navigation.NavRoute
import com.friendat.ui.composables.FriendCard
import com.friendat.ui.composables.WifiCard
import com.friendat.ui.friends.AcceptedFriendsSection
import com.friendat.ui.friends.FriendshipCard
import com.friendat.ui.friends.PendingRequestsSection
import com.friendat.ui.friends.UserSearchSection
import com.friendat.ui.theme.*
import com.friendat.ui.viewmodel.AuthViewModel
import com.friendat.ui.viewmodel.FriendScreenUiEvent
import com.friendat.ui.viewmodel.FriendViewModel
import com.friendat.ui.viewmodel.FriendsListUiState
import com.friendat.ui.viewmodel.WifiLocationListUiState
import com.friendat.ui.viewmodel.WifiLocationsViewModel


@Composable
fun HomeScreen(navController: NavController,
               viewModelWifiLocation: WifiLocationsViewModel = hiltViewModel(),
               viewModelFriend: FriendViewModel = hiltViewModel(),
               authViewModel: AuthViewModel = hiltViewModel()) {
    val locationsState by viewModelWifiLocation.wifiLocationsState.collectAsState()
    val actionState by viewModelWifiLocation.wifiLocationActionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(1) }
    val labels = listOf("Friends", "Locations")
    var showMenu by remember { mutableStateOf<Boolean>(false) }

    val searchState by viewModelFriend.userSearchResultUiState.collectAsState()
    val friendsListState by viewModelFriend.friendsListUiState.collectAsState()

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

    Column {
        Box(Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Primary))
        Box(Modifier
            .fillMaxWidth()
            .background(Primary)) {
            IconButton({ showMenu = (!showMenu) }, Modifier) {
                Icon(Icons.Default.Menu, "Menue", Modifier.fillMaxHeight())
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("logout") },
                    onClick = {
                        authViewModel.signOut()
                        navController.navigate(NavRoute.Login.route)
                    }
                )
            }
        }
        // TabBar oben
        TabRow(
            selectedTabIndex = selectedTab,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab]),
                        shape = RoundedCornerShape(
                            topStart = 3.dp,
                            topEnd = 3.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp
                        ),
                        color = Sekundary
                    )
                }
            },
            containerColor = Primary
        ) {
            labels.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                )
            }
        }

        // 🔹 Content je nach Tab
        when (selectedTab) {
            1 -> LocationsScreen(locationsState,viewModelWifiLocation)
            0 -> FriendsScreen()
        }
    }


    Box(Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = {when(selectedTab){
                0-> navController.navigate(NavRoute.AddFriend.route)
                1-> navController.navigate(NavRoute.AddWifi.route)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}


@Composable
fun FriendsScreen() {
    Box(Modifier.fillMaxSize()) {
        Text("Under Construction", modifier = Modifier.padding(20.dp))
    }
}


@Composable
fun LocationsScreen(locationsState: WifiLocationListUiState,viewModel: WifiLocationsViewModel = hiltViewModel()) {
    Box(Modifier.fillMaxSize()) {
        when (val state = locationsState) {
            is WifiLocationListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(alignment = Alignment.Center))
            }

            is WifiLocationListUiState.Success -> {
                if (state.locations.isEmpty()) {
                    Text(
                        "No Wi-Fi locations added yet. Tap the '+' button to add one.",
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
                            .padding(20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.locations, key = { location -> location.id }) { location ->
                                    WifiCard(location, {}, { viewModel.deleteWifiLocation(location.id) })

                        }
                    }
                }
            }

            is WifiLocationListUiState.Error -> {
                Text(
                    "Error loading locations: ${state.message ?: "Unknown error"}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}


