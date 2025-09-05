package com.friendat.ui.screens // Dein Paketname

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.friendat.data.model.WifiLocation // Dein Datenmodell
import com.friendat.navigation.NavRoute // Deine Navigationsrouten
import com.friendat.ui.viewmodel.WifiLocationActionUiState
import com.friendat.ui.viewmodel.WifiLocationListUiState
import com.friendat.ui.viewmodel.WifiLocationsViewModel
import com.friendat.utils.WifiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiLocationsListScreen(
    navController: NavController,
    viewModel: WifiLocationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    val locationsState by viewModel.wifiLocationsState.collectAsState()
    val actionState by viewModel.wifiLocationActionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog-Status-Variablen
    var showFineLocationRationaleDialog by remember { mutableStateOf(false) }
    var showBackgroundLocationRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsRedirectDialogForFineLocation by remember { mutableStateOf(false) }

    // --- Hilfsfunktionen für Navigation und Berechtigungsprüfung ---

    fun navigateToAddScreenWithBssid(navControllerInstance: NavController, currentContext: Context) {
        val bssid = WifiUtils.getCurrentBssid(currentContext)
        Log.d("WifiListScreen", "Navigating to AddWifi with BSSID: $bssid")
        navControllerInstance.navigate(NavRoute.AddWifi.createRoute(bssid))
    }

    fun checkAndProceedWithBackgroundLocation(
        currentContext: Context,
        navControllerInstance: NavController
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                currentContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (backgroundLocationGranted) {
                Log.d("WifiListScreen", "Background Location already granted. Proceeding.")
                navigateToAddScreenWithBssid(navControllerInstance, currentContext)
            } else {
                Log.d("WifiListScreen", "Background Location NOT granted. Showing rationale.")
                showBackgroundLocationRationaleDialog = true // Zeige Dialog, um zu Einstellungen zu leiten
            }
        } else {
            // Für Versionen vor Android Q (10) ist Fine Location ausreichend
            Log.d("WifiListScreen", "Pre-Android Q. Fine Location is sufficient. Proceeding.")
            navigateToAddScreenWithBssid(navControllerInstance, currentContext)
        }
    }

    // Launcher für die Anforderung von ACCESS_FINE_LOCATION (und COARSE auf S+)
    val requestFineLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val fineLocationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (fineLocationGranted) {
            Log.d("WifiListScreen", "ACCESS_FINE_LOCATION granted by launcher.")
            checkAndProceedWithBackgroundLocation(context, navController)
        } else {
            Log.w("WifiListScreen", "ACCESS_FINE_LOCATION denied by launcher.")
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION
                )) {
                Log.d("WifiListScreen", "Fine Location likely denied permanently. Show settings redirect.")
                showSettingsRedirectDialogForFineLocation = true
            } else {
                Log.d("WifiListScreen", "Showing rationale for Fine Location (denied by launcher).")
                // Der Nutzer wird den FAB erneut klicken müssen, um den Rationale-Dialog (falls zutreffend) zu sehen
                // oder den Request erneut zu starten, falls er "Grant" im Rationale wählt.
                // Es ist auch eine Option, showFineLocationRationaleDialog hier direkt auf true zu setzen.
                // Für Konsistenz mit dem direkten Klick auf FAB, machen wir es so, dass der Nutzer den FAB neu klicken muss.
            }
        }
    }

    // Funktion, die den gesamten Berechtigungs- und Navigations-Flow startet, wenn der FAB geklickt wird
    fun performPermissionRequestAndNavigate() {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted) {
            Log.d("WifiListScreen", "Fine Location already granted. Checking background.")
            checkAndProceedWithBackgroundLocation(context, navController)
        } else {
            // Fine Location nicht erteilt, prüfe, ob Rationale gezeigt werden soll
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, fineLocationPermission)) {
                Log.d("WifiListScreen", "Showing Rationale for Fine Location before request.")
                showFineLocationRationaleDialog = true
            } else {
                // Keine Rationale nötig (erster Aufruf oder "nicht mehr fragen") -> direkt anfordern
                Log.d("WifiListScreen", "Requesting Fine Location (and Coarse on S+).")
                val permissionsToRequestArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                requestFineLocationLauncher.launch(permissionsToRequestArray)
            }
        }
    }

    // Reagiere auf Änderungen im ActionState (für Snackbar-Meldungen, z.B. nach dem Löschen)
    LaunchedEffect(key1 = actionState) {
        when (val currentActionState = actionState) {
            is WifiLocationActionUiState.Success -> {
                snackbarHostState.showSnackbar(
                   "Action successful!", // z.B. "Location deleted."
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
                performPermissionRequestAndNavigate()
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new Wi-Fi Location")
            }
        },
        topBar = {
            TopAppBar(title = { Text("My Wi-Fi Locations") }) // Dein TopAppBar-Titel
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp) // Dein Padding
        ) {
            // Deine UI zur Anzeige der Liste und Lade-/Fehlerzustände
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
                                WifiLocationListItem( // Dein Item-Composable
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

    // --- Dialoge für Berechtigungen ---

    if (showFineLocationRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showFineLocationRationaleDialog = false },
            title = { Text("Precise Location Needed") },
            text = { Text("To automatically detect the current Wi-Fi network (BSSID) when adding a new location, this app needs access to your precise location. This is an Android requirement for accessing Wi-Fi information. You can still add locations manually if you prefer.") },
            confirmButton = {
                Button(onClick = {
                    showFineLocationRationaleDialog = false
                    Log.d("WifiListScreen", "Fine Location Rationale: User clicked Grant. Requesting.")
                    val permissionsArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    requestFineLocationLauncher.launch(permissionsArray)
                }) { Text("Grant Access") }
            },
            dismissButton = {
                Button(onClick = {
                    showFineLocationRationaleDialog = false
                    Log.d("WifiListScreen", "Fine Location Rationale: User clicked Continue Manually.")
                    navController.navigate(NavRoute.AddWifi.createRoute(null)) // Navigiere zum manuellen Hinzufügen
                }) { Text("Continue Manually") }
            }
        )
    }

    if (showBackgroundLocationRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundLocationRationaleDialog = false
                // Wichtig: Auch wenn der Nutzer diesen Dialog schließt, hat er bereits Fine Location.
                // Fahre mit dem fort, was wir haben (Navigation zum Add-Screen mit BSSID, falls vorhanden).
                Log.d("WifiListScreen", "Background Rationale dismissed. Proceeding with Fine Location only for current add.")
                navigateToAddScreenWithBssid(navController, context)
            },
            title = { Text("Background Location Access (Recommended)") },
            text = { Text("To automatically update your status when you enter or leave a known Wi-Fi zone even when the app is closed, please allow location access 'All the time' in the app settings. This is optional but enables the full automatic functionality of the app.") },
            confirmButton = {
                Button(onClick = {
                    showBackgroundLocationRationaleDialog = false
                    Log.d("WifiListScreen", "Background Rationale: User clicked Go to Settings.")
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                    // Nach Rückkehr von den Einstellungen muss der Nutzer den FAB ggf. erneut klicken,
                    // damit der Flow die (hoffentlich) erteilte Hintergrund-Berechtigung erkennt.
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                Button(onClick = {
                    showBackgroundLocationRationaleDialog = false
                    Log.d("WifiListScreen", "Background Rationale: User clicked 'Maybe Later'. Proceeding with Fine Location only for current add.")
                    navigateToAddScreenWithBssid(navController, context)
                }) { Text("Maybe Later") }
            }
        )
    }

    if (showSettingsRedirectDialogForFineLocation) {
        AlertDialog(
            onDismissRequest = { showSettingsRedirectDialogForFineLocation = false },
            title = { Text("Permission Denied") },
            text = { Text("Precise location access has been permanently denied by you. To automatically detect Wi-Fi networks when adding them, you need to enable this permission in the app settings. You can still add locations manually.") },
            confirmButton = {
                Button(onClick = {
                    showSettingsRedirectDialogForFineLocation = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                Button(onClick = {
                    showSettingsRedirectDialogForFineLocation = false
                    // Optional: Zum manuellen Hinzufügen navigieren
                    // navController.navigate(NavRoute.AddWifi.createRoute(null))
                }) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun WifiLocationListItem( // Dein Item-Composable
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(wifiLocation.colorHex))
                            } catch (e: IllegalArgumentException) {
                                Color.Gray // Fallback-Farbe
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Icon: ${wifiLocation.iconId}", style = MaterialTheme.typography.bodySmall)
                // Hier könntest du das Icon laden, falls du eine Mapping-Funktion hast
                // Icon(imageVector = mapIconIdToVector(wifiLocation.iconId), contentDescription = null)
            }
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete ${wifiLocation.name}")
        }
    }
}
