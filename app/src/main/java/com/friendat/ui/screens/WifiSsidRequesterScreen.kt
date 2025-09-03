package com.friendat.ui.screens // DEIN PAKETNAME HIER

import android.Manifest
import android.app.Activity // Wichtig für shouldShowRequestPermissionRationale
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.friendat.navigation.NavRoute
import com.friendat.utils.WifiUtils // Stelle sicher, dass der Import korrekt ist!
// Kein WeakReference-Hack mehr nötig, wenn wir den Kontext richtig verwenden

@Composable
fun WifiSsidRequesterScreen(navController: NavController) {
    val context = LocalContext.current // Dieser Kontext ist für die meisten Dinge OK
    val activity = LocalContext.current as? Activity // Versuche, den Activity-Kontext zu bekommen

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsRedirectDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }


    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else { // Android 7.1 (API 25) bis Android 11 (API 30)
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            // COARSE_LOCATION wird hier nicht explizit benötigt, da ACCESS_FINE_LOCATION es beinhaltet
            // oder für die SSID-Erkennung nicht die primäre Wahl war wie FINE.
            // Das System gibt dem Nutzer bei FINE-Anfrage auf <12 keine separate Grob/Fein-Auswahl.
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val fineLocationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            if (fineLocationGranted) {
                Log.d("SsidRequesterScreen", "ACCESS_FINE_LOCATION GRANTED")
                val ssid = WifiUtils.getCurrentBssid(context)
                if (ssid != null && ssid != "<unknown ssid>") {
                    feedbackMessage = "SSID: $ssid. Navigating..."
                    navigateToSsidAddScreen(navController, ssid)
                } else {
                    feedbackMessage = "Could not get SSID. Ensure WiFi is connected and location services are enabled."
                    Log.w("SsidRequesterScreen", "Fine location granted, but SSID is '$ssid'")
                }
            } else {
                Log.d("SsidRequesterScreen", "ACCESS_FINE_LOCATION DENIED")
                feedbackMessage = "Precise location permission is required to automatically detect SSID."

                if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d("SsidRequesterScreen", "Permission likely denied permanently.")
                    showSettingsRedirectDialog = true
                } else {
                    showRationaleDialog = true
                }
            }
        }
    )

    fun requestPermissions() {
        permissionLauncher.launch(permissionsToRequest)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            feedbackMessage = null // Nachricht zurücksetzen

            // Da minSdk = 25, benötigen wir immer Laufzeitberechtigungen.
            val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (fineLocationGranted) {
                Log.d("SsidRequesterScreen", "Fine location already granted. Getting SSID.")
                val ssid = WifiUtils.getCurrentBssid(context)
                if (ssid != null && ssid != "<unknown ssid>") {
                    feedbackMessage = "SSID: $ssid. Navigating..."
                    navigateToSsidAddScreen(navController, ssid)
                } else {
                    feedbackMessage = "Could not get SSID (already granted). Ensure WiFi and location services are on."
                    Log.w("SsidRequesterScreen", "SSID null or <unknown ssid> even with permission. SSID was: '$ssid'")
                }
            } else {
                Log.d("SsidRequesterScreen", "Fine location not granted. Checking rationale.")
                if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d("SsidRequesterScreen", "Showing rationale dialog.")
                    showRationaleDialog = true
                } else {
                    // Keine Rationale nötig (z.B. erste Anfrage) oder Activity-Kontext nicht verfügbar
                    Log.d("SsidRequesterScreen", "No rationale needed or first time / activity context null. Requesting permissions.")
                    requestPermissions()
                }
            }
        }) {
            Text("Add WiFi Location (Get Current SSID)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        feedbackMessage?.let { Text(it) }

        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = { Text("Permission Needed") },
                text = { Text("To automatically detect the WiFi SSID, this app needs access to your precise location. This is an Android requirement for accessing WiFi information. Without it, you'll need to enter the SSID manually.") },
                confirmButton = {
                    Button(onClick = {
                        showRationaleDialog = false
                        requestPermissions()
                    }) { Text("Grant Access") }
                },
                dismissButton = {
                    Button(onClick = {
                        showRationaleDialog = false
                        navigateToSsidAddScreen(navController, null) // Weiter ohne SSID
                    }) { Text("Continue Manually") }
                }
            )
        }

        if (showSettingsRedirectDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsRedirectDialog = false },
                title = { Text("Permission Required") },
                text = { Text("Precise Location permission was denied permanently. To use this feature, please enable it in the app settings.") },
                confirmButton = {
                    Button(onClick = {
                        showSettingsRedirectDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }) { Text("Open Settings") }
                },
                dismissButton = {
                    Button(onClick = {
                        showSettingsRedirectDialog = false
                        navigateToSsidAddScreen(navController, null)
                    }) { Text("Cancel & Continue Manually") }
                }
            )
        }
    }
}

// Hilfsfunktion bleibt gleich
private fun navigateToSsidAddScreen(navController: NavController, detectedSsid: String?) {
    // Verwende die typsichere createRoute-Funktion aus deinem NavRoute-Objekt
    val targetRoute = NavRoute.AddWifi.createRoute(detectedSsid)

    // Der Log-Tag sollte konsistent sein, z.B. der Klassenname oder ein spezifischer Tag
    Log.d("WifiSsidRequesterScreen", "Navigating to target route: $targetRoute") // Wird jetzt "add_wifi_screen/..." ausgeben

    navController.navigate(targetRoute)
}

