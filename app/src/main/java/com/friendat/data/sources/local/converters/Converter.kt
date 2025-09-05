package com.friendat.data.sources.local.converters

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.room.TypeConverter
import com.friendat.R
import com.friendat.data.model.Friend
import com.friendat.data.model.WifiLocation


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
        "Ramen"->R.drawable.ic_ramen
        else ->R.drawable.ic_error
    }
}
val iconList=listOf<String>("Home","Person","Heart","Car","Mountain","People","Apartment","Flood","Ramen")
fun longToColor(long: Long): Color{
    return Color(long.toULong())
}

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




