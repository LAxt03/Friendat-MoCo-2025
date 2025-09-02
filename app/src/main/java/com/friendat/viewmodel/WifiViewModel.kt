package com.friendat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.model.repository.WifiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WifiNetwork(
    val ssid: String,
    val name: String,
    val color: Long
)

class WifiViewModel(
    private val wifiRepo: WifiRepository
) : ViewModel() {

    private val _wifiNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<WifiNetwork>> = _wifiNetworks

    fun loadWifiNetworks() {
        viewModelScope.launch {
            val result = wifiRepo.getWifiNetworks()
            _wifiNetworks.value = result.mapNotNull { map ->
                val ssid = map["ssid"] as? String
                val name = map["name"] as? String
                val color = map["color"] as? Long
                if (ssid != null && name != null && color != null) {
                    WifiNetwork(ssid, name, color)
                } else null
            }
        }
    }

    fun addWifiNetwork(ssid: String, name: String, color: Long) {
        viewModelScope.launch {
            wifiRepo.saveWifiNetwork(ssid, name, color)
            loadWifiNetworks() // refresh
        }
    }

    fun deleteWifiNetwork(ssid: String) {
        viewModelScope.launch {
            wifiRepo.deleteWifiNetwork(ssid)
            loadWifiNetworks() // refresh
        }
    }
}
