package com.friendat.ui.livestatus // Oder dein entsprechendes UI-Paket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Standard-Icons
import androidx.compose.material.icons.filled.HelpOutline // Fallback Icon
import androidx.compose.material.icons.filled.SignalWifiOff
// Importiere hier Icons, die du tatsächlich für deine Status verwenden wirst, z.B.
// import androidx.compose.material.icons.filled.Home
// import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
// Stelle sicher, dass die folgenden Importe auf deine tatsächlichen Klassen verweisen
// import com.friendat.ui.livestatus.FriendLiveStatusViewModel // Dein ViewModel
// import com.friendat.ui.livestatus.FriendWithLiveStatus // Deine Datenklasse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendLiveStatusScreen(
    viewModel: FriendLiveStatusViewModel = hiltViewModel() // ViewModel injizieren
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Friend Live Status") })
        }
    ) { paddingValues ->
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
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.friendsWithStatus, key = { it.displayInfo.uid }) { friendWithStatus ->
                        FriendLiveStatusCard(friendWithStatus = friendWithStatus)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendLiveStatusCard(friendWithStatus: FriendWithLiveStatus) {
    // Hilfsfunktion, um eine Farbe aus einem Hex-String zu parsen
    fun Color.Companion.parse(hexString: String?): Color {
        return try {
            if (hexString != null && hexString.startsWith("#") && (hexString.length == 7 || hexString.length == 9)) {
                Color(android.graphics.Color.parseColor(hexString))
            } else {
                Color.Gray // Fallback-Farbe, wenn kein Hex oder ungültig
            }
        } catch (e: IllegalArgumentException) {
            Color.LightGray // Fallback bei Parsing-Fehler
        }
    }

    // Hilfsfunktion, um ein Icon basierend auf der ID zuzuordnen
    // ERWEITERE DIES MIT DEINEN ECHTEN ICON-RESSOURCEN ODER LOGIK
    @Composable
    fun getIconForId(iconId: String?): ImageVector {
        return when (iconId?.lowercase()) {
            // "home", "ic_home" -> Icons.Default.Home // Beispiel
            // "work", "ic_work" -> Icons.Default.Work // Beispiel
            "ic_offline" -> Icons.Default.SignalWifiOff // Wenn du einen spezifischen Offline-Icon-String hast
            // Füge hier weitere Icon-Mappings für deine iconId-Strings hinzu
            else -> Icons.Default.HelpOutline // Fallback-Icon, wenn kein Mapping gefunden
        }
    }

    val status = friendWithStatus.liveStatus
    val displayName = friendWithStatus.displayInfo.displayName ?: friendWithStatus.displayInfo.email ?: "Unknown Friend"
    val networkName = if (status?.isOnline == true) status.locationName ?: "Online" else "Offline"
    val statusColor = Color.parse(status?.colorHex)
    val statusIcon = if (status?.isOnline == true) getIconForId(status.iconId) else Icons.Default.SignalWifiOff


    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (status?.isOnline == true) statusColor.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.1f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Du könntest hier auch das Profilbild des Freundes laden, wenn du es in UserDisplayInfo hast
            Icon(
                imageVector = statusIcon,
                contentDescription = "Status Icon",
                tint = if (status?.isOnline == true) statusColor else Color.Gray,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Network: $networkName",
                    fontSize = 14.sp,
                    color = if (status?.isOnline == true) LocalContentColor.current else Color.Gray
                )
                if (status?.isOnline == true && !status.bssid.isNullOrBlank()) {
                    Text(
                        text = "BSSID: ${status.bssid}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }

            // Kleiner Farbkreis als Indikator, wenn online
            if (status?.isOnline == true) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(statusColor, shape = CircleShape)
                )
            }
        }
    }
}
