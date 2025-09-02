package com.friendat.model.database.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.*

@Entity
data class Friend( @PrimaryKey val id: String, val name:String,  val iconName:String, val color: Long)
