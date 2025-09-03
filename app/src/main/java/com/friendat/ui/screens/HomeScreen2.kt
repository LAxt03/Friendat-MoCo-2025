package com.friendat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.friendat.navigation.NavRoute

@Composable
fun HomeScreen2(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Home Screen")

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            navController.navigate(NavRoute.WifiLocationsList.route)
        }) {
            Text("Go to WiFi Locations")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate(NavRoute.FriendManagement.route)
        }) {
            Text("Manage Friends")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Beispiel: Navigiere zurück (würde hier die App beenden, wenn Home der Start ist nach Login)
            // oder zu einem anderen Screen, falls es einen gäbe
            // navController.popBackStack() // Einfach als Beispiel für einen Button
            // Für einen dedizierten Logout-Button wäre es besser,
            // die AuthRepository.signOut() Methode aufzurufen und dann zum Login-Screen zu navigieren.
            // Z.B.:
            // viewModel.signOut() -> dann im ViewModel: navController.navigate(NavRoute.Login.route) { popUpTo(NavRoute.Home2.route) { inclusive = true } }


        }) {
            Text("Placeholder Action (z.B. Logout)")
        }
    }
}
