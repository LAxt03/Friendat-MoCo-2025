package com.friendat.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.friendat.navigation.NavRoute
import com.friendat.ui.viewmodel.AuthViewModel
import com.friendat.work.LocationCheckWorker

@Composable
fun HomeScreen2(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val context = LocalContext.current

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
            authViewModel.signOut()
            navController.navigate(NavRoute.Login.route) {
                popUpTo(NavRoute.Home2.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }

        }) {
            Text("Placeholder Action (z.B. Logout)")
        }

       Button(onClick = {
            triggerLocationCheckWorker(context)
       }) {
            Text("Test Location Worker")
        }

    }



}

private fun triggerLocationCheckWorker(context: Context) {
    // Optionale Constraints für den Worker
    val constraints = androidx.work.Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED) // Worker soll nur laufen, wenn Netzwerk verbunden ist
        .setRequiresBatteryNotLow(true) // Beispiel: Nur wenn Akku nicht niedrig ist
        .build()

    // Erstelle eine einmalige Arbeitsanforderung für den LocationCheckWorker
    val locationCheckWorkRequest = OneTimeWorkRequestBuilder<LocationCheckWorker>()
        .setConstraints(constraints)
        // Du könntest hier auch InputData übergeben, wenn dein Worker das benötigt
        // .setInputData(workDataOf("KEY_USER_ID" to "12345"))
        .addTag(LocationCheckWorker.TAG) // Optional: Füge einen Tag hinzu, um den Worker später zu finden/abzubrechen
        .build()

    // Hole die WorkManager-Instanz und plane die Anforderung ein
    WorkManager.getInstance(context).enqueue(locationCheckWorkRequest)

    // Gib eine Info aus (z.B. Toast oder Log), dass der Worker gestartet wurde
    // Für diesen Test reicht ein Log-Eintrag. In einer echten UI könntest du einen Toast anzeigen.
    android.util.Log.d("HomeScreen2", "LocationCheckWorker enqueued for one-time execution.")
    // Toast.makeText(context, "LocationCheckWorker enqueued!", Toast.LENGTH_SHORT).show()
}
