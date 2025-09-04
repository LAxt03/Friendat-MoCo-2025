package com.friendat.ui.screens // Passe den Paketnamen ggf. an

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.data.model.WifiLocation // Dein neues Datenmodell
import com.friendat.navigation.NavRoute // Deine Navigationsrouten-Definitionen
import com.friendat.ui.viewmodel.WifiLocationActionUiState
import com.friendat.ui.viewmodel.WifiLocationListUiState
import com.friendat.ui.viewmodel.WifiLocationsViewModel
import com.friendat.utils.WifiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiLocationsListScreen( // Neuer Name oder dein bisheriger Name
    navController: NavController,
    viewModel: WifiLocationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    val locationsState by viewModel.wifiLocationsState.collectAsState()
    val actionState by viewModel.wifiLocationActionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Reagiere auf Änderungen im ActionState (für Snackbar-Meldungen, z.B. nach dem Löschen)
    LaunchedEffect(key1 = actionState) {
        when (val currentActionState = actionState) {
            is WifiLocationActionUiState.Success -> {
                // Diese Snackbar ist eher für Aktionen wie Löschen relevant,
                // da das Hinzufügen auf einem anderen Screen passiert.
                snackbarHostState.showSnackbar(
                    message = "Action successful!", // z.B. "Location deleted."
                    duration = SnackbarDuration.Short
                )
                viewModel.clearWifiLocationActionState() // Zustand zurücksetzen
            }
            is WifiLocationActionUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = currentActionState.message ?: "An error occurred.",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearWifiLocationActionState() // Zustand zurücksetzen
            }
            else -> { /* Idle, Loading (für Liste) werden durch UI-Elemente dargestellt */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Navigiere zum AddWifiScreen
                performPermissionRequestAndNavigate()
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new Wi-Fi Location")
            }
        },
        topBar = {
            // Temporäre TopAppBar für diesen Test-Screen
            TopAppBar(title = { Text("My Wi-Fi Locations (Test)") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = locationsState) {
                is WifiLocationListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is WifiLocationListUiState.Success -> {
                    if (state.locations.isEmpty()) {
                        Text(
                            "No Wi-Fi locations added yet. Tap the '+' button to add one.",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.locations, key = { location -> location.id }) { location ->
                                WifiLocationListItem(
                                    wifiLocation = location,
                                    onDeleteClick = { viewModel.deleteWifiLocation(location.id) },
                                )
                                Divider()
                            }
                        }
                    }
                }
                is WifiLocationListUiState.Error -> {
                    Text(
                        "Error loading locations: ${state.message ?: "Unknown error"}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Needed") },
            text = { Text("To automatically detect the Wi-Fi network (BSSID), this app needs access to your precise location. This is an Android requirement for accessing Wi-Fi information. Without it, you'll need to enter the BSSID manually.") },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    permissionLauncher.launch(permissionsToRequest)
                }) { Text("Grant Access") }
            },
            dismissButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    navigateToIdentifierAddScreen(navController, null) // Weiter ohne BSSID
                }) { Text("Continue Manually") }
            }
        )
    }
}



@Composable
fun WifiLocationListItem( // Angepasstes Item für die Liste
    wifiLocation: WifiLocation,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(wifiLocation.name, style = MaterialTheme.typography.titleMedium)
            Text("BSSID: ${wifiLocation.bssid}", style = MaterialTheme.typography.bodySmall)
            // Icon und Farbe aus dem WifiLocation-Objekt anzeigen
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Einfache Farbanzeige als Box
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            try {
                                Color(
                                    android.graphics.Color.parseColor(
                                        wifiLocation.colorHex
                                    )
                                )
                            } catch (e: IllegalArgumentException) {
                                Color.Gray // Fallback-Farbe bei ungültigem Hex
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Icon: ${wifiLocation.iconId}", style = MaterialTheme.typography.bodySmall)
                // Hier könntest du das tatsächliche Icon laden, wenn du nameToImageVector hier verwendest
                // Icon(imageVector = nameToImageVector(wifiLocation.iconId), contentDescription = null, modifier = Modifier.size(16.dp))
            }

        }
        // Ladezustand für Löschen wird global über die Snackbar oder einen globalen Indikator gehandhabt
        // Alternativ: Du könntest actionState hierher durchreichen und prüfen, ob gerade dieses Item gelöscht wird.
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete ${wifiLocation.name}")
        }
    }
}
