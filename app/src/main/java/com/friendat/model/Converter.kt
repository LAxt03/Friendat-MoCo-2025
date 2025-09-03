package com.friendat.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.room.TypeConverter
import com.friendat.R
import com.friendat.model.database.entity.Friend
import com.friendat.model.database.entity.WifiLocation


fun nameToResId(iconName: String):Int{
    return when(iconName){
        "Home"-> R.drawable.ic_home
        "Person"->R.drawable.ic_person
        "Heart"->R.drawable.ic_heart
        "Car"->R.drawable.ic_car
        "Mountain"->R.drawable.ic_mountain
        "People"->R.drawable.ic_people
        "Apartment"->R.drawable.ic_apartment
        "Flood"->R.drawable.ic_flood
        else ->R.drawable.ic_error
    }
}
val iconList=listOf<String>("Home","Person","Heart","Car","Mountain","People","Apartment","Flood")
fun longToColor(long: Long): Color{
    return Color(long.toULong())
}
@Composable
fun WifiLocation.getIcon()= painterResource(nameToResId(iconName))

fun WifiLocation.getColor(): Color = longToColor(color)
@Composable
fun Friend.getIcon()=painterResource(nameToResId(iconName))

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




