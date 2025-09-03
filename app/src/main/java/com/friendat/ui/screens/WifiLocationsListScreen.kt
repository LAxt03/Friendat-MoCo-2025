package com.friendat.ui.screens // Passe den Paketnamen ggf. an

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.data.model.WifiLocation // Dein neues Datenmodell
import com.friendat.navigation.NavRoute // Deine Navigationsrouten-Definitionen
import com.friendat.ui.viewmodel.WifiLocationActionUiState
import com.friendat.ui.viewmodel.WifiLocationListUiState
import com.friendat.ui.viewmodel.WifiLocationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiLocationsListScreen( // Neuer Name oder dein bisheriger Name
    navController: NavController,
    viewModel: WifiLocationsViewModel = hiltViewModel()
) {
    val locationsState by viewModel.wifiLocationsState.collectAsState()
    val actionState by viewModel.wifiLocationActionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

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
                navController.navigate(NavRoute.AddWifi.route) // Stelle sicher, dass diese Route existiert
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
                                    // Umbenannt von WifiLocationItem zur Klarheit
                                    wifiLocation = location,
                                    onDeleteClick = { viewModel.deleteWifiLocation(location.id) },
                                    // isLoadingAction für spezifisches Item Loading (optional für diesen Screen)
                                    // Da Löschen hier die Hauptaktion ist, könnte der globale actionState ausreichen
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
            Text("SSID: ${wifiLocation.ssid}", style = MaterialTheme.typography.bodySmall)
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
