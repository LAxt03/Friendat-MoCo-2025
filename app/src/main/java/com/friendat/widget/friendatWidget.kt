package com.friendat.widget

import android.content.Context
import androidx.annotation.DimenRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import com.friendat.R
import com.friendat.model.database.entity.Friend
import com.friendat.model.getColor
import com.friendat.model.getIcon
import com.friendat.model.nameToResId
import com.friendat.ui.livestatus.FriendLiveStatusViewModel
import com.friendat.ui.theme.Primary
import com.friendat.ui.viewmodel.FriendViewModel

class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = friendatWidget()
}

class friendatWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(60.dp, 60.dp)
        private val MEDIUM= DpSize(80.dp, 80.dp)
        private val LARGE = DpSize(120.dp, 180.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL,
            MEDIUM,
            LARGE
        )
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {

        provideContent {
            // create your AppWidget here

            val friendList = listOf<Friend>(
                Friend("Louis","LAxt03","Mountain",Color(red=0,green=150,blue = 50).value.toLong(),"Home"),
                Friend("Louis","LAxt03","Flood",Color(red=255,green=150,blue = 50).value.toLong(),"Other Home"),
                Friend("Louis","LAxt03","Person",Color(red=255,green=0,blue = 50).value.toLong(),"Aarons Crib"),
                Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong(),"random house"),
                Friend("Louis","LAxt03","Car",Color(red=255,green=150,blue = 255).value.toLong(),"CACHOU"),
                Friend("Louis","LAxt03","Home",Color(red=255,green=150,blue = 50).value.toLong(),"random house")
            )
            var side by remember { mutableIntStateOf(0) }
            var friendNr by remember { mutableIntStateOf(0) }
            fun nextFriend(){friendNr = if(friendNr==friendList.size-1){0}else{friendNr+1}}
            fun lastFriend(){friendNr = if(friendNr==0){friendList.size-1}else{friendNr-1}}
            fun changeSide(){side = if(side==0){1}else{0}}
            val size = LocalSize.current
            if(friendList.size==0){
                Empty()
            }else{
                if(size.height<=SMALL.height){
                    ShowFriend(friendList[friendNr],side,{changeSide()})
                }else if(size.height>SMALL.height&&size.height<=MEDIUM.height){
                    ShowFriendNav(friendList,side,friendNr,{changeSide()},{nextFriend()},{lastFriend()})
                }else{
                    ShowFriends(friendList)
                }
            }
            //widget(friends)
        }
    }
}
/*
@Composable
fun MyContent() {
    val repository = remember { FriendRepository.getInstance() }
    // Retrieve the cache data everytime the content is refreshed
    val destinations by repository.destinations.collectAsState(State.Loading)

    when (destinations) {
        is State.Loading -> {
            Column(GlanceModifier.fillMaxSize().background(Color.Green)) {  }
        }

        is State.Error -> {
            Column(GlanceModifier.fillMaxSize().background(Color.Red)) {  }
        }

        is State.Completed -> {
            widget()
        }
    }friend.getColor()
}*/
@Composable
fun ShowFriendNav(friendList: List<Friend>,
                  side:Int,
                  friendNr:Int,
                  changeSide: () -> Unit,
                  nextFriend:()->Unit,
                  lastFriend:()-> Unit){
    val friend= friendList[friendNr]
    Box (GlanceModifier.background(friend.getColor()).fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = GlanceModifier, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Button("Previous",lastFriend)
            Box(
                GlanceModifier.background(friend.getColor()).clickable(changeSide).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (side) {
                    0 -> {
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Image(
                                ImageProvider(nameToResId(iconName = friend.iconName)),
                                friend.locationName,
                                GlanceModifier.padding(6.dp).size(50.dp)
                            )
                            Text(friend.locationName, GlanceModifier.padding(3.dp))
                        }
                    }

                    else -> {
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Image(
                                ImageProvider(nameToResId(iconName = friend.iconName)),
                                friend.locationName,
                                GlanceModifier.padding(6.dp).size(50.dp)
                            )
                            Text(friend.name, GlanceModifier.padding(3.dp))
                        }
                    }
                }
            }
            Button("Next",nextFriend)
        }
    }
}
@Composable
fun ShowFriend(friend: Friend,side:Int,changeSide:()->Unit){
    Box(GlanceModifier.fillMaxSize().background(friend.getColor()).clickable(changeSide).fillMaxWidth(), contentAlignment = Alignment.Center){
        when(side){
            0-> {
                Column {
                    Image(
                        ImageProvider(nameToResId(iconName = friend.iconName)),
                        friend.locationName,
                        GlanceModifier.padding(6.dp)
                    )
                    Text(friend.locationName, GlanceModifier.padding(3.dp))
                }
            }
            else -> {
                Column {
                    Image(
                        ImageProvider(nameToResId(iconName = friend.iconName)),
                        friend.locationName,
                        GlanceModifier.padding(6.dp)
                    )
                    Text(friend.name, GlanceModifier.padding(3.dp))
                }
            }
        }
    }
}
@Composable
fun Empty(){
    Column(GlanceModifier.background(Primary).fillMaxSize()){
        Text("Add Friends")
    }
}
@Composable
fun ShowFriends(friendList: List<Friend>){
    var side by remember { mutableIntStateOf(0) }
    fun changeSide(){side = if(side==0){1}else{0}}
    LazyColumn(GlanceModifier.fillMaxSize().background(Color.DarkGray)) {
        items(friendList) { friend ->
            FriendView(friend = friend,side,::changeSide)
        }
    }
}
@Composable
fun FriendView(friend: Friend,side:Int,onClick:()-> Unit){
    Box(GlanceModifier.height(89.dp).fillMaxWidth().background(friend.getColor()).clickable(onClick), contentAlignment = Alignment.Center){
        when(side){
            0->{
                Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        ImageProvider(nameToResId(iconName = friend.iconName)),
                        friend.locationName,
                        GlanceModifier.padding(6.dp).size(60.dp)
                    )
                    Text(friend.locationName,GlanceModifier.padding(3.dp))
                }
            }
            else ->{
                Text(friend.name,GlanceModifier.padding(3.dp))
            }
        }
    }
}
