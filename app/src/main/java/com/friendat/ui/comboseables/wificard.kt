package com.friendat.ui.comboseables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendat.model.database.entity.WifiLocation
import com.friendat.model.getColor
import com.friendat.model.getIcon
import com.friendat.ui.theme.Sekundary

@Composable
fun WifiCard(
    wifiLocation: WifiLocation,editClick: ()-> Unit, deleteClick: () -> Unit,modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(wifiLocation.getIcon(), wifiLocation.getColor(), editClick)
        Spacer(Modifier.size(20.dp))
        Column(Modifier.weight(3f)) {
            Text(wifiLocation.location_name, fontSize = 20.sp)
            Text(wifiLocation.ssid, fontSize = 15.sp)
        }

        IconButton(
            onClick = {
                if (confirmDelete) {
                    // Second click → actually delete
                    deleteClick()
                    confirmDelete = false
                } else {
                    // First click → just warn
                    confirmDelete = true
                }
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = if (confirmDelete) Color.Red else Sekundary
            )
        }

    }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            kotlinx.coroutines.delay(2000) // 2 seconds
            confirmDelete = false
        }
    }
}

@Composable
fun Avatar(icon: ImageVector,color:Color, onClick:()-> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .size(50.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = "Icon ${icon.name}")
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ContactCardPreview() {
    WifiCard(
        WifiLocation("Fritz!Box","Zu Hause","Home",Color(red=255,green=150,blue = 50).value.toLong(),1),
        {},{},
        modifier = Modifier
    )
}
