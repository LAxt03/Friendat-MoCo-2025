package com.friendat.ui.pop_up

import android.text.style.BackgroundColorSpan
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendat.ui.theme.*



@Composable
fun Add_wifi(ssid:String, onAccept:()->Unit,onCancel:()->Unit) {
    var text by remember { mutableStateOf("") } // Holds the input text
    Box(Modifier.height(400.dp).width(500.dp).background(color = BackGround)) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Add $ssid",Modifier.padding(16.dp), fontSize = 32.sp)
            TextField(
                modifier = Modifier
                    .padding(50.dp)
                    .fillMaxWidth(),
                value = text,
                onValueChange = { newText -> text = newText },
                label = { Text("Enter location name") },
                colors = OutlinedTextFieldDefaults.colors( unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
            Row{
                Button(onClick = { onAccept() }, modifier = Modifier.padding(32.dp), colors = ButtonColors(
                    containerColor = Sekundary, contentColor = Color.White,
                    disabledContainerColor = Color.DarkGray,
                    disabledContentColor = Color.Black
                )) {
                    Text("Accept")
                }
                OutlinedButton(onClick = { onCancel() }, modifier = Modifier.padding(32.dp), colors = ButtonColors(
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
@Preview(name = "add_wifi_prev", showSystemUi = false, showBackground = false)
@Composable
fun Add_wifi_test(){
    Add_wifi("Fritz!Box",{},{})
}