package com.friendat.ui.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.data.model.WifiLocation
import com.friendat.data.repository.WifiLocationRepository
import com.friendat.data.sources.local.dao.WifiLocationDao
import com.friendat.model.iconList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface WifiLocationListUiState {
    object Loading : WifiLocationListUiState
    data class Success(val locations: List<WifiLocation>) : WifiLocationListUiState
    data class Error(val message: String?) : WifiLocationListUiState
}

sealed interface WifiLocationActionUiState {
    object Idle : WifiLocationActionUiState
    object Loading : WifiLocationActionUiState
    object Success : WifiLocationActionUiState
    data class Error(val message: String?) : WifiLocationActionUiState
}



@HiltViewModel
class WifiLocationsViewModel @Inject constructor(
    private val wifiLocationRepository: WifiLocationRepository,
    private val wifiLocationDao: WifiLocationDao
) : ViewModel() {

    //Zustände
    private val _wifiLocationsState = MutableStateFlow<WifiLocationListUiState>(
        WifiLocationListUiState.Loading)
    val wifiLocationsState: StateFlow<WifiLocationListUiState> = _wifiLocationsState.asStateFlow()

    private val _wifiLocationActionState = MutableStateFlow<WifiLocationActionUiState>(
        WifiLocationActionUiState.Idle)
    val wifiLocationActionState: StateFlow<WifiLocationActionUiState> = _wifiLocationActionState.asStateFlow()

    private val _newWifiLocationName = MutableStateFlow("")
    val newWifiLocationName: StateFlow<String> = _newWifiLocationName.asStateFlow()

    private val _newWifiLocationBssid = MutableStateFlow("")
    val newWifiLocationBssid: StateFlow<String> = _newWifiLocationBssid.asStateFlow()

    //Farb und Icon Zustände
    private val _availableIcons = MutableStateFlow(iconList)
    val availableIcons: StateFlow<List<String>> = _availableIcons.asStateFlow()

    private val _selectedIconName = MutableStateFlow(iconList.firstOrNull() ?: "default_icon")
    val selectedIconName: StateFlow<String> = _selectedIconName.asStateFlow()

    private val _selectedColorRed = MutableStateFlow(100f)
    val selectedColorRed: StateFlow<Float> = _selectedColorRed.asStateFlow()

    private val _selectedColorGreen = MutableStateFlow(100f)
    val selectedColorGreen: StateFlow<Float> = _selectedColorGreen.asStateFlow()

    private val _selectedColorBlue = MutableStateFlow(100f)
    val selectedColorBlue: StateFlow<Float> = _selectedColorBlue.asStateFlow()


    init {
        loadWifiLocationsFromRoom()
    }


    private fun loadWifiLocations() {
        viewModelScope.launch {
            wifiLocationRepository.getWifiLocationsForCurrentUser()
                .collect { result ->
                    _wifiLocationsState.value = when {
                        result.isSuccess -> WifiLocationListUiState.Success(result.getOrNull() ?: emptyList())
                        result.isFailure -> WifiLocationListUiState.Error(result.exceptionOrNull()?.message)
                        else -> WifiLocationListUiState.Error("Unknown error occurred") // Sollte nicht passieren mit Result
                    }
                }
        }
    }

    private fun loadWifiLocationsFromRoom() {
        _wifiLocationsState.value = WifiLocationListUiState.Loading // Setze Ladezustand initial
        viewModelScope.launch {
            // Hole die userId sicher, z.B. aus dem AuthRepository oder Firebase Auth direkt
            // Für dieses Beispiel gehen wir davon aus, dass das Repository das handhaben kann
            // oder du eine Methode hast, um die aktuelle userId zu bekommen.
            // Hier wäre es besser, wenn das Repository den userId-Check macht.
            // Für den DAO-Aufruf brauchen wir aber die userId.
            // Eine Möglichkeit:
            // val userId = firebaseAuth.currentUser?.uid // (FirebaseAuth müsste injiziert werden)
            // if (userId == null) {
            //     _wifiLocationsState.value = WifiLocationListUiState.Error("User nicht angemeldet")
            //     return@launch
            // }

            // wifiLocationDao.getAllLocationsForUserFlow(userId) // <- Braucht die userId
            // Besser: Das Repository bietet einen Flow aus Room an, der die userId intern behandelt

            // DAHER: Wir erweitern das Repository, um einen Flow aus Room anzubieten

            wifiLocationRepository.getWifiLocationsForCurrentUserFromRoom() // NEUE METHODE IM REPOSITORY
                .collect { locations -> // Diese Methode gibt direkt List<WifiLocation> oder Result<List<WifiLocation>> zurück
                    // Je nachdem, wie die neue Repository-Methode implementiert ist:
                    // Variante A: Repository-Methode gibt Flow<List<WifiLocation>> zurück
                    _wifiLocationsState.value = WifiLocationListUiState.Success(locations)

                    // Variante B: Repository-Methode gibt Flow<Result<List<WifiLocation>>> zurück (ähnlich wie Firestore-Version)
                    // _wifiLocationsState.value = when {
                    //    result.isSuccess -> WifiLocationListUiState.Success(result.getOrNull() ?: emptyList())
                    //    result.isFailure -> WifiLocationListUiState.Error(result.exceptionOrNull()?.message)
                    //    else -> WifiLocationListUiState.Error("Unknown error")
                    // }
                }
        }
    }




        //Event Handling
    fun onNewWifiLocationNameChange(name: String) {
        _newWifiLocationName.value = name
    }

    fun onNewWifiLocationBssidChange(bssid: String) {
        _newWifiLocationBssid.value = bssid
    }

    fun setInitialBssid(bssid: String) {
        _newWifiLocationBssid.value = bssid
        Log.d("WifiViewModel", "Initial SSID set to: $bssid")
    }

    fun onSelectedIconNameChange(iconName: String) {
        _selectedIconName.value = iconName
    }

    fun onSelectedColorRedChange(value: Float) {
        _selectedColorRed.value = value
    }

    fun onSelectedColorGreenChange(value: Float) {
        _selectedColorGreen.value = value
    }

    fun onSelectedColorBlueChange(value: Float) {
        _selectedColorBlue.value = value
    }


    fun addWifiLocation() {
        if (_wifiLocationActionState.value == WifiLocationActionUiState.Loading) return // Verhindere doppelte Ausführung

        val name = newWifiLocationName.value.trim()
        val bssid = newWifiLocationBssid.value.trim()

        if (name.isBlank() || bssid.isBlank()) {
            _wifiLocationActionState.value = WifiLocationActionUiState.Error("Name and BSSID cannot be empty.")
            return
        }

        val color = Color(
            selectedColorRed.value.roundToInt(),
            selectedColorGreen.value.roundToInt(),
            selectedColorBlue.value.roundToInt()
        )

        val colorHex = String.format("#%02X%02X%02X", color.redInt, color.greenInt, color.blueInt)

        viewModelScope.launch {
            _wifiLocationActionState.value = WifiLocationActionUiState.Loading
            val newLocation = WifiLocation(
                // id wird von Firestore generiert, userId wird im Repository gesetzt
                name = name,
                bssid = bssid,
                iconId = selectedIconName.value,
                colorHex = colorHex
            )
            val result = wifiLocationRepository.addWifiLocation(newLocation)

            if (result.isSuccess) {
                _wifiLocationActionState.value = WifiLocationActionUiState.Success
                // Felder zurücksetzen nach Erfolg
                _newWifiLocationName.value = ""
                _newWifiLocationBssid.value = ""
                _selectedIconName.value = iconList.firstOrNull() ?: "default_icon"
                _selectedColorRed.value = 100f
                _selectedColorGreen.value = 100f
                _selectedColorBlue.value = 100f
            } else {
                _wifiLocationActionState.value = WifiLocationActionUiState.Error(result.exceptionOrNull()?.message ?: "Fehler beim Hinzufügen von Wifi Location")
            }
        }
    }

    //Hilfsfunktionen zum Konvertieren von Color zu HEX
    private val Color.redInt: Int get() = (this.red * 255).roundToInt()
    private val Color.greenInt: Int get() = (this.green * 255).roundToInt()
    private val Color.blueInt: Int get() = (this.blue * 255).roundToInt()


    fun deleteWifiLocation(locationId: String) {
        if (_wifiLocationActionState.value == WifiLocationActionUiState.Loading) return

        viewModelScope.launch {
            _wifiLocationActionState.value = WifiLocationActionUiState.Loading
            val result = wifiLocationRepository.deleteWifiLocation(locationId)
            _wifiLocationActionState.value = if (result.isSuccess) {
                WifiLocationActionUiState.Success
            } else {
                WifiLocationActionUiState.Error(result.exceptionOrNull()?.message ?: "Fehler beim Löschen von Wifi Location")
            }
        }
    }

    //Helper Methode
    fun clearWifiLocationActionState() {
        _wifiLocationActionState.value = WifiLocationActionUiState.Idle
    }
}
