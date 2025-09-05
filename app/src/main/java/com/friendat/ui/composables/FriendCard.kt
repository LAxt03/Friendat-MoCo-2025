package com.friendat.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendat.data.model.Friend
import com.friendat.data.sources.local.converters.getColor
import com.friendat.data.sources.local.converters.getIcon
import com.friendat.ui.theme.Sekundary

@Composable
fun FriendCard(
    friend: Friend, editClick: ()-> Unit, deleteClick: () -> Unit, modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(friend.getIcon(), friend.getColor(), editClick)
        Spacer(Modifier.size(20.dp))
        Column(Modifier.weight(3f)) {
            Text(friend.name, fontSize = 20.sp)
            Text(friend.id, fontSize = 15.sp)
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


@Preview(showBackground = true)
@Composable()
fun FriendCardPrev(){
    FriendCard(Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong()),{},{},
        Modifier)
}