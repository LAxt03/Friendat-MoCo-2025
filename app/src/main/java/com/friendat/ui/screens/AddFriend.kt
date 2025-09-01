package com.friendat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.util.TableInfo
import com.friendat.model.database.entity.Friend
import com.friendat.model.database.entity.WifiLocation
import com.friendat.model.iconList
import com.friendat.ui.theme.BackGround
import com.friendat.ui.theme.Sekundary
import kotlin.math.roundToInt

@Composable
fun AddFriend(UserId:String,saveFriend:(Friend)->Unit,cancel:()-> Unit){
    var friendId: String by remember { mutableStateOf("") }
    var friendNickname: String by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(BackGround)){
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Text("Your ID:",Modifier.padding(10.dp), fontSize = 32.sp)
            Text(UserId,Modifier.padding(10.dp), fontSize = 24.sp)
            TextField(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                value = friendId,
                onValueChange = { newText -> friendId = newText },
                label = { Text("Enter Friend-ID") },
                colors = OutlinedTextFieldDefaults.colors( unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
            TextField(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                value = friendNickname,
                onValueChange = { newText -> friendNickname = newText },
                label = { Text("Enter Friend-Nickname") },
                colors = OutlinedTextFieldDefaults.colors( unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
            Row{
                Button(onClick = {saveFriend(Friend(friendNickname,friendId))}, modifier = Modifier.padding(32.dp), colors = ButtonColors(
                    containerColor = Sekundary, contentColor = Color.White,
                    disabledContainerColor = Color.DarkGray,
                    disabledContentColor = Color.Black
                )) {
                    Text("Save")
                }
                OutlinedButton(onClick = { cancel() }, modifier = Modifier.padding(32.dp), colors = ButtonColors(
                    containerColor = BackGround,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.DarkGray,
                    disabledContentColor = Color.Black,
                )) {
                    Text("Cancle")
                }
            }
        }
    }

}

@Preview
@Composable
fun AddFriendPrev(){
    AddFriend("friuhqperiugherugh",{},{})
}