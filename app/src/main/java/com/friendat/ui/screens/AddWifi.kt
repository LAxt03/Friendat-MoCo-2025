package com.friendat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendat.model.database.entity.WifiLocation
import com.friendat.model.iconList
import com.friendat.model.nameToResId
import com.friendat.ui.theme.*
import kotlin.math.roundToInt


@Composable
fun Add_wifi(ssid:String, onAccept:(WifiLocation)->Unit, onCancel:()->Unit) {
    var text by remember { mutableStateOf("") } // Holds the input text
    Box(Modifier
        .fillMaxSize()
        .background(color = BackGround)) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val locationName: String by remember { mutableStateOf("") }
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
            var sliderPositionRed by remember { mutableFloatStateOf(100f) }
            Column(modifier = Modifier.padding(20.dp)) {
                Slider(
                    value = sliderPositionRed,
                    onValueChange = { sliderPositionRed = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red,
                        inactiveTrackColor = Color(220,220,220),
                    ),
                    steps = 10,
                    valueRange = 0f..255f,
                )
                Text(text = sliderPositionRed.roundToInt().toString())
            }
            var sliderPositionGreen by remember { mutableFloatStateOf(100f) }
            Column(modifier = Modifier.padding(20.dp)) {
                Slider(
                    value = sliderPositionGreen,
                    onValueChange = { sliderPositionGreen = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Green,
                        activeTrackColor = Color.Green,
                        inactiveTrackColor = Color(220,220,220),
                    ),
                    steps = 10,
                    valueRange = 0f..255f,
                )
                Text(text = sliderPositionGreen.roundToInt().toString())
            }
            var sliderPositionBlue by remember { mutableFloatStateOf(100f) }
            Column(modifier = Modifier.padding(20.dp)) {
                Slider(
                    value = sliderPositionBlue,
                    onValueChange = { sliderPositionBlue = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Blue,
                        activeTrackColor = Color.Blue,
                        inactiveTrackColor = Color(220,220,220),
                    ),
                    steps = 10,
                    valueRange = 0f..255f,
                )
                Text(text = sliderPositionRed.roundToInt().toString())
            }
            var iconNum by remember { mutableIntStateOf(2) }
            fun nextIcon(){iconNum = if(iconNum==iconList.size-1){0}else{iconNum+1}}
            fun lastIcon(){iconNum = if(iconNum==0){iconList.size-1}else{iconNum-1}}
            Row {
                IconButton(::nextIcon,modifier=Modifier.size(100.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Previous Icon",
                        Modifier.size(50.dp))
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(
                            Color(
                                sliderPositionRed.roundToInt(),
                                sliderPositionGreen.roundToInt(),
                                sliderPositionBlue.roundToInt()
                            )
                        )
                        .size(100.dp),
                    Alignment.Center
                ) {
                    Icon(
                        painterResource(nameToResId(iconList.get(iconNum))),
                        contentDescription = iconList.get(iconNum),
                        Modifier.size(50.dp)
                    )
                }
                IconButton(::lastIcon,modifier=Modifier.size(100.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = "Previous Icon",
                        Modifier.size(50.dp))
                }
            }
            Row{
                Button(onClick = { onAccept(WifiLocation(
                    ssid = ssid,
                    locationName,
                    iconList.get(iconNum),
                    color=Color(sliderPositionRed.roundToInt(), sliderPositionGreen.roundToInt(), sliderPositionBlue.roundToInt()).value.toLong(),
                    0)) }, modifier = Modifier.padding(32.dp), colors = ButtonColors(
                    containerColor = Sekundary, contentColor = Color.White,
                    disabledContainerColor = Color.DarkGray,
                    disabledContentColor = Color.Black
                )) {
                    Text("Save")
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