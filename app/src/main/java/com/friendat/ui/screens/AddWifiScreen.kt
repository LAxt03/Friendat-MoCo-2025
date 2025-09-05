package com.friendat.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack // Standard ArrowBack für die TopAppBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.data.sources.local.converters.nameToResId
import com.friendat.ui.theme.*
import com.friendat.ui.viewmodel.WifiLocationActionUiState
import com.friendat.ui.viewmodel.WifiLocationsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWifiScreen(
    navController: NavController,
    bssid: String,
    viewModel: WifiLocationsViewModel = hiltViewModel()

) {

    Log.d("AddWifiScreen", "Composable called. Received BSSID parameter: '$bssid'")
    // States aus dem ViewModel
    val locationName by viewModel.newWifiLocationName.collectAsState()
    val locationBssid by viewModel.newWifiLocationBssid.collectAsState() // Für manuelle BSSID-Eingabe
    val actionState by viewModel.wifiLocationActionState.collectAsState()

    val availableIcons by viewModel.availableIcons.collectAsState()
    val selectedIconName by viewModel.selectedIconName.collectAsState()
    var currentIconIndex by remember(availableIcons, selectedIconName) { // Index basierend auf ViewModel-State
        mutableIntStateOf(availableIcons.indexOf(selectedIconName).coerceAtLeast(0))
    }

    val colorRed by viewModel.selectedColorRed.collectAsState()
    val colorGreen by viewModel.selectedColorGreen.collectAsState()
    val colorBlue by viewModel.selectedColorBlue.collectAsState()

    LaunchedEffect(key1 = bssid, key2 = viewModel) {
        if (bssid.isNotBlank()) {
            viewModel.setInitialBssid(bssid)
        }
    }


    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()


    LaunchedEffect(key1 = actionState) {
        when (val currentActionState = actionState) {
            is WifiLocationActionUiState.Success -> {

                navController.popBackStack()
                viewModel.clearWifiLocationActionState()
            }
            is WifiLocationActionUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = currentActionState.message ?: "An error occurred.",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearWifiLocationActionState() // Zustand zurücksetzen
            }
            else -> {  }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Wi-Fi Location") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarColors(BackGround,BackGround,Color.Black,Color.Black,Color.Black)
            )
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .background(color = BackGround) // Deine Hintergrundfarbe
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState) // Für längere Inhalte scrollbar machen
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    value = locationName,
                    onValueChange = { viewModel.onNewWifiLocationNameChange(it) },
                    label = { Text("Enter location name (e.g., Home)") },
                    singleLine = true,
                )


                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    value = locationBssid,
                    onValueChange = { viewModel.onNewWifiLocationBssidChange(it) },
                    label = { Text("Enter Wi-Fi BSSID (Network Name)") },
                    singleLine = true,
                )

                Text("Icon Color:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                // Red Slider
                ColorSlider(
                    label = "Red",
                    value = colorRed,
                    onValueChange = { viewModel.onSelectedColorRedChange(it) },
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red
                )
                // Green Slider
                ColorSlider(
                    label = "Green",
                    value = colorGreen,
                    onValueChange = { viewModel.onSelectedColorGreenChange(it) },
                    thumbColor = Color.Green,
                    activeTrackColor = Color.Green
                )
                // Blue Slider
                ColorSlider(
                    label = "Blue",
                    value = colorBlue,
                    onValueChange = { viewModel.onSelectedColorBlueChange(it) },
                    thumbColor = Color.Blue,
                    activeTrackColor = Color.Blue
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Icon:", style = MaterialTheme.typography.titleMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (availableIcons.isNotEmpty()) {
                                currentIconIndex = if (currentIconIndex == 0) availableIcons.size - 1 else currentIconIndex - 1
                                viewModel.onSelectedIconNameChange(availableIcons[currentIconIndex])
                            }
                        },
                        modifier = Modifier.size(60.dp),
                        enabled = availableIcons.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Previous Icon",
                            Modifier.size(30.dp)
                        )
                    }

                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Color(
                                    colorRed.roundToInt(),
                                    colorGreen.roundToInt(),
                                    colorBlue.roundToInt()
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (availableIcons.isNotEmpty() && currentIconIndex < availableIcons.size) {
                            Icon(
                                painter = painterResource(nameToResId(availableIcons[currentIconIndex])),
                                contentDescription = availableIcons[currentIconIndex],
                                Modifier.size(40.dp),
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (availableIcons.isNotEmpty()) {
                                currentIconIndex = if (currentIconIndex == availableIcons.size - 1) 0 else currentIconIndex + 1
                                viewModel.onSelectedIconNameChange(availableIcons[currentIconIndex])
                            }
                        },
                        modifier = Modifier.size(60.dp),
                        enabled = availableIcons.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = "Next Icon",
                            Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    // Save Button
                    onClick = { viewModel.addWifiLocation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = locationName.isNotBlank() && locationBssid.isNotBlank() && actionState !is WifiLocationActionUiState.Loading,
                    colors = ButtonColors(Sekundary,Color.White, Color.White,Color.Gray)
                ) {
                    if (actionState is WifiLocationActionUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), color = Color.White)
                    } else {
                        Text("Save Location", fontSize = 16.sp)
                    }
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(top = 8.dp),
                    enabled = actionState !is WifiLocationActionUiState.Loading,
                    colors= ButtonColors(Sekundary,Color.White, Color.White,Color.Gray)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Hilfs-Composable für die Slider
@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    thumbColor: Color,
    activeTrackColor: Color
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text("$label: ${value.roundToInt()}", fontSize = 16.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = activeTrackColor,
                inactiveTrackColor = Color(220, 220, 220),
            ),
            steps = 254, // Für 256 Schritte (0-255)
            valueRange = 0f..255f,
        )
    }
}

