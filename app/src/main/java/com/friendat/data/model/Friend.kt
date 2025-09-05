package com.friendat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Friend(val name:String, @PrimaryKey val id: String, val iconName:String="", val color: Long=0, val locationName: String="")