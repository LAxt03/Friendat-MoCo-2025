package com.friendat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.navigation.NavRoute
import com.friendat.ui.friends.UserDisplayCard
import com.friendat.ui.theme.BackGround
import com.friendat.ui.theme.Sekundary
import com.friendat.ui.viewmodel.FriendScreenUiEvent
import com.friendat.ui.viewmodel.FriendViewModel
import com.friendat.ui.viewmodel.UserSearchResultUiState

@Composable
fun AddFriend(
    userId:String,
    cancel:()-> Unit
){
    var friendId: String by remember { mutableStateOf("") }
    var friendNickname: String by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(BackGround)){
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Text("Your ID:",Modifier.padding(10.dp), fontSize = 32.sp)
            Text(userId,Modifier.padding(10.dp), fontSize = 24.sp)
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
                Button(onClick = {},
                    enabled = friendId.isNotBlank() && friendNickname.isNotBlank(),
                    modifier = Modifier.padding(32.dp), colors = ButtonColors(
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
                    Text("Cancel")
                }
            }
        }
    }

}

@Composable
fun AddFriend2(
    navController: NavController,
    viewModel: FriendViewModel = hiltViewModel()
){
    val searchState by viewModel.userSearchResultUiState.collectAsState()
    val friendsListState by viewModel.friendsListUiState.collectAsState()
//    var friendId: String by remember { mutableStateOf("") }
    var friendNickname: String by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf(searchState.searchInput) }

    Box(Modifier.fillMaxSize().background(BackGround)){
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Spacer(Modifier.height(100.dp))
            TextField(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Enter Friend Email") },
                colors = OutlinedTextFieldDefaults.colors( unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
            if (searchState.isLoading && searchState.searchResultUser == null) { // Ladeindikator nur für die Suche
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            searchState.errorMessage?.let {
                if (!it.startsWith("Friend request sent") && !it.startsWith("Failed to send")) { // Fehler, die nicht direkt das Senden betreffen
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            TextField(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                value = friendNickname,
                onValueChange = { newText -> friendNickname = newText },
                label = { Text("Give your friend a nice nickname") },
                colors = OutlinedTextFieldDefaults.colors( unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
            Row{
                Button(onClick = { viewModel.onEvent(FriendScreenUiEvent.SearchUserByEmail(emailInput)) },
                    enabled = emailInput.isNotBlank() && friendNickname.isNotBlank(),
                    modifier = Modifier.padding(32.dp), colors = ButtonColors(
                        containerColor = Sekundary, contentColor = Color.White,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Color.Gray
                    )) {
                    Text("Save")
                }
                OutlinedButton(onClick = { }, modifier = Modifier.padding(32.dp), colors = ButtonColors(
                    containerColor = BackGround,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White,
                    disabledContentColor = Color.Gray,
                )) {
                    Text("Cancel")
                }
            }
            searchState.searchResultUser?.let { user ->
                if (user.uid.isNotBlank()) {
                    viewModel.onEvent(FriendScreenUiEvent.SendFriendRequest(user.uid))
                    searchState.errorMessage?.let {
                        if(it.startsWith("Failed to send") || it.startsWith("Cannot send")) { // Fehler, die das Senden betreffen
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }else if(it.startsWith("Friend request sent!")){
                                navController.navigate(NavRoute.Home.route)
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun UserSearchSectioni(
    searchState: UserSearchResultUiState,
    onEvent: (FriendScreenUiEvent) -> Unit
) {
    var emailInput by remember { mutableStateOf(searchState.searchInput) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Search User by Email", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = searchState.errorMessage?.contains("Email") == true || searchState.errorMessage?.contains("not found") == true
        )
        if (searchState.isLoading && searchState.searchResultUser == null) { // Ladeindikator nur für die Suche
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        searchState.errorMessage?.let {
            if (!it.startsWith("Friend request sent") && !it.startsWith("Failed to send")) { // Fehler, die nicht direkt das Senden betreffen
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { onEvent(FriendScreenUiEvent.SearchUserByEmail(emailInput)) }) {
                Text("Search")
            }
            if (searchState.searchResultUser != null || searchState.searchInput.isNotEmpty()) {
                Button(onClick = {
                    emailInput = "" // Textfeld auch leeren
                    onEvent(FriendScreenUiEvent.ClearSearch)
                }) {
                    Text("Clear")
                }
            }
        }


        searchState.searchResultUser?.let { user ->
            if (user.uid.isNotBlank()) { // Nur anzeigen, wenn eine gültige UID vorhanden ist
                Spacer(modifier = Modifier.height(8.dp))
                Text("Search Result:", style = MaterialTheme.typography.titleSmall)
                UserDisplayCard(user = user) {
                    // Button zum Senden der Anfrage
                    Button(
                        onClick = { onEvent(FriendScreenUiEvent.SendFriendRequest(user.uid)) },
                        enabled = !searchState.isLoading // Deaktivieren während des Sendens
                    ) {
                        if (searchState.isLoading && searchState.requestSentMessage == null) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Request")
                        }
                    }
                }
                searchState.errorMessage?.let {
                    if(it.startsWith("Failed to send") || it.startsWith("Cannot send")) { // Fehler, die das Senden betreffen
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun AddFriendPrev(){
    AddFriend("friuhqperiugherugh",{})
}

