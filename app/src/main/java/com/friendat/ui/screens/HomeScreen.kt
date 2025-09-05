package com.friendat.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.data.model.FriendshipStatus
import com.friendat.model.database.entity.Friend
import com.friendat.navigation.NavRoute
import com.friendat.ui.composables.FriendCard
import com.friendat.ui.composables.WifiCard
import com.friendat.ui.friends.AcceptedFriendsSection
import com.friendat.ui.friends.FriendshipCard
import com.friendat.ui.friends.PendingRequestsSection
import com.friendat.ui.friends.UserSearchSection
import com.friendat.ui.livestatus.FriendLiveStatusCard
import com.friendat.ui.livestatus.FriendLiveStatusViewModel
import com.friendat.ui.theme.*
import com.friendat.ui.viewmodel.AuthViewModel
import com.friendat.ui.viewmodel.FriendScreenUiEvent
import com.friendat.ui.viewmodel.FriendViewModel
import com.friendat.ui.viewmodel.FriendsListUiState
import com.friendat.ui.viewmodel.FriendshipWithDisplayInfo
import com.friendat.ui.viewmodel.WifiLocationListUiState
import com.friendat.ui.viewmodel.WifiLocationsViewModel
import com.friendat.utils.WifiUtils


@Composable
fun HomeScreen(navController: NavController,
               viewModelWifiLocation: WifiLocationsViewModel = hiltViewModel(),
               viewModelFriend: FriendViewModel = hiltViewModel(),
               authViewModel: AuthViewModel = hiltViewModel()) {

    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    val locationsState by viewModelWifiLocation.wifiLocationsState.collectAsState()
    val actionState by viewModelWifiLocation.wifiLocationActionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(1) }
    val labels = listOf("Friends", "Locations")
    var showMenu by remember { mutableStateOf<Boolean>(false) }

    val searchState by viewModelFriend.userSearchResultUiState.collectAsState()
    val friendsListState by viewModelFriend.friendsListUiState.collectAsState()

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsRedirectDialog by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun navigateToIdentifierAddScreen(navController: NavController, detectedIdentifier: String?) {
        val targetRoute = NavRoute.AddWifi.createRoute(detectedIdentifier) // Deine NavRoute.AddWifi.createRoute
        Log.d("WifiLocationsListScreen", "Navigating to target route: $targetRoute")
        navController.navigate(targetRoute)
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val fineLocationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            if (fineLocationGranted) {
                Log.d("WifiLocationsListScreen", "ACCESS_FINE_LOCATION GRANTED after request")
                val identifier = WifiUtils.getCurrentBssid(context) // Hole BSSID
                // feedbackMessage = "BSSID: $identifier. Navigating..." // Optional
                navigateToIdentifierAddScreen(navController, identifier)
            } else {
                Log.d("WifiLocationsListScreen", "ACCESS_FINE_LOCATION DENIED after request")
                // feedbackMessage = "Precise location permission is required..." // Optional

                if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d("WifiLocationsListScreen", "Permission likely denied permanently.")
                    showSettingsRedirectDialog = true
                } else {
                    showRationaleDialog = true
                }
            }
        }
    )


    fun performPermissionRequestAndNavigate() {
        // feedbackMessage = null // Optional zurücksetzen
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted) {
            Log.d("WifiLocationsListScreen", "Fine location already granted. Getting BSSID.")
            val identifier = WifiUtils.getCurrentBssid(context)
            // feedbackMessage = "BSSID: $identifier. Navigating..." // Optional
            navigateToIdentifierAddScreen(navController, identifier)
        } else {
            Log.d("WifiLocationsListScreen", "Fine location not granted. Checking rationale.")
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("WifiLocationsListScreen", "Showing rationale dialog.")
                showRationaleDialog = true
            } else {
                Log.d("WifiLocationsListScreen", "No rationale needed or first time. Requesting permissions.")
                permissionLauncher.launch(permissionsToRequest)
            }
        }
    }

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
                DropdownMenuItem(
                    text = { Text("DevMode") },
                    onClick = {
                        authViewModel.signOut()
                        navController.navigate(NavRoute.Home2.route)
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
                //1-> performPermissionRequestAndNavigate()
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
fun FriendsScreen(viewModel: FriendLiveStatusViewModel = hiltViewModel(),friendVM: FriendViewModel=hiltViewModel()) {
    Column(Modifier.fillMaxWidth()) {
            val uiState by viewModel.uiState.collectAsState()
            val paddingValues by remember {mutableStateOf(3.dp) }
            when {

                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: ${uiState.errorMessage}")
                    }
                }
                uiState.friendsWithStatus.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No friends to display status for.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.friendsWithStatus, key = { it.displayInfo.uid }) { friendWithStatus ->
                            FriendCard(Friend(friendWithStatus.displayInfo.displayName?:"error",
                                friendWithStatus.displayInfo.email?:"error",
                                friendWithStatus.liveStatus?.iconId?:"error",
                                friendWithStatus.liveStatus?.colorHex?.toLong()?:Color(100,100,100).value.toLong(),
                                friendWithStatus.liveStatus?.locationName?:"error"),{},
                                {friendVM.onEvent(FriendScreenUiEvent.RemoveFriend(friendWithStatus.displayInfo.uid)) }
                            )
                            Divider()
                        }
                    }
                }
            }
            val friendsListState by friendVM.friendsListUiState.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pending Friend Requests", style = MaterialTheme.typography.titleMedium)
                if (friendsListState.isLoadingPending) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                if (friendsListState.pendingRequests.isEmpty() && !friendsListState.isLoadingPending) {
                    Text("No pending requests.")
                }
                friendsListState.pendingRequests.forEach { friendshipWithInfo ->
                    PendingFriendshipCard(
                        friendshipWithInfo = friendshipWithInfo,
                        actions = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { friendVM.onEvent(FriendScreenUiEvent.AcceptFriendRequest(friendshipWithInfo.friendship.id)) },
                                    colors = ButtonColors(Sekundary, Color.White, Color.White, Color.Gray)) {
                                    Text("Accept")
                                }
                                Button(onClick = { friendVM.onEvent(FriendScreenUiEvent.DeclineFriendRequest(friendshipWithInfo.friendship.id)) },
                                    colors = ButtonColors(Sekundary, Color.White, Color.White, Color.Gray)) {
                                    Text("Decline")
                                }
                            }
                        }
                    )
                }
            }
    }
}
@Composable
fun PendingFriendshipCard(
    friendshipWithInfo: FriendshipWithDisplayInfo,
    actions: @Composable () -> Unit
) {
    val friendInfo = friendshipWithInfo.friendDisplayInfo
    val friendship = friendshipWithInfo.friendship

    Card(modifier = Modifier.fillMaxWidth(), colors = CardColors(BackGround,Color.Black,BackGround,Color.Black)) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (friendInfo != null) {
                Text(friendInfo.displayName ?: "Loading Name...", fontWeight = FontWeight.Bold)
                Text(friendInfo.email ?: "Loading Email...", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            actions()
        }
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


