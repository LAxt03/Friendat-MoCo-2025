package com.friendat.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.TypeConverter
import com.friendat.model.database.entity.Friend
import com.friendat.model.database.entity.WifiLocation


fun nameToImageVector(iconName: String): ImageVector{
    return when(iconName){
        "Home"-> Icons.Filled.Home
        else -> Icons.Default.Clear
    }
}

fun longToColor(long: Long): Color{
    return Color(long.toULong())
}
fun WifiLocation.getIcon(): ImageVector = nameToImageVector(iconName)

fun WifiLocation.getColor(): Color = longToColor(color)
fun Friend.getIcon(): ImageVector = nameToImageVector(iconName)

fun Friend.getColor() = longToColor(color)

class Converters {
    @TypeConverter
    fun fromColor(color: Color): Long {
        // Color als Long speichern (ARGB)
        return color.value.toLong()
    }

    @TypeConverter
    fun toColor(value: Long): Color {
        return Color(value.toULong())
    }
}




