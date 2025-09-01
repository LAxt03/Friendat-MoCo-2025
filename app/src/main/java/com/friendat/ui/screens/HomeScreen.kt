package com.friendat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.friendat.model.database.entity.Friend
import com.friendat.model.database.entity.WifiLocation
import com.friendat.ui.comboseables.FriendCard
import com.friendat.ui.comboseables.WifiCard
import com.friendat.ui.theme.*

@Composable
fun MainScreen(locations:List<WifiLocation>,friends: List<Friend>,addLocation:()->Unit,addFriend:()->Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    val labels = listOf("Locations", "Friends")


    Column {
        // 🔹 TabBar oben
        TabRow(
            selectedTabIndex = selectedTab,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab]),
                        shape = RoundedCornerShape(
                            topStart = 3.dp,
                            topEnd = 3.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp
                        ),
                        color = Sekundary
                    )
                }
            },
            containerColor = Primary
        ) {
            labels.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                )
            }
        }

        // 🔹 Content je nach Tab
        when (selectedTab) {
            0 -> LocationsScreen(locations)
            1 -> FriendsScreen(friends)
        }
    }

    fun addButton() {
        if (selectedTab == 0) {
            addLocation
        } else (addFriend)
    }
    Box(modifier = Modifier.fillMaxSize()) {
    FloatingActionButton(
        onClick = { addButton() }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = Primary
    ) {
        Icon(Icons.Default.Add, "Floating action button.")
    }
}
}

@Composable
fun LocationsScreen(locations: List<WifiLocation>) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            items(locations) { location ->
                WifiCard(location, {}, {})
            }
        }
    }
}

@Composable
fun FriendsScreen(friends:List<Friend>) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            items(friends) { friend ->
                FriendCard(friend, {}, {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPrev(){
    MainScreen(listOf(
        WifiLocation("Fritz!Box","zuHause","Home", Color(20,100,255).value.toLong(),1),
        WifiLocation("Fritz!Box","zuHause","Home", Color(20,100,255).value.toLong(),1),
        WifiLocation("Fritz!Box","zuHause","Home", Color(20,100,255).value.toLong(),1),
        WifiLocation("Fritz!Box","zuHause","Home", Color(20,100,255).value.toLong(),1),
        WifiLocation("Fritz!Box","zuHause","Home", Color(30,120,255).value.toLong(),1)
    ),
        listOf(
            Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong()),
            Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong()),
            Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong()),
            Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong()),
            Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong()),
            ),
        {},
        {}
        )

}