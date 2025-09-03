package com.friendat.ui.screens // Oder dein gewähltes Paket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.friendat.navigation.NavRoute // Importiere deine Routen

@Composable
fun HomeScreen2(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Home Screen")
        Button(onClick = {
            // Beispiel: Navigiere zurück (würde hier die App beenden, wenn Home der Start ist nach Login)
            // oder zu einem anderen Screen, falls es einen gäbe
            navController.popBackStack() // Einfach als Beispiel für einen Button
        }) {
            Text("Go Back (oder andere Aktion)")
        }
    }
}