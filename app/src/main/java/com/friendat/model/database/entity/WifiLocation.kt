package com.friendat.model.database.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class WifiLocation(val ssid: String, val location_name: String, val iconName: String, val color: Long,@PrimaryKey(autoGenerate = true) val id: Int)
