package com.friendat.model.database.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.*

@Entity
data class Friend(val name:String, @PrimaryKey val userName: String,  val iconName:String="", val color: Long=0, val locationName: String="")
