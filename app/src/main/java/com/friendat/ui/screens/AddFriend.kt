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
                OutlinedButton(onClick = {navController.navigate(NavRoute.Home.route)}, modifier = Modifier.padding(32.dp), colors = ButtonColors(
                    containerColor = BackGround,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White,
                    disabledContentColor = Color.Gray,
                )) {
                    Text("Cancel")
                }
            }
            var requestSend by remember { mutableStateOf(false) }
            searchState.searchResultUser?.let { user ->
                if (user.uid.isNotBlank()==!requestSend) {
                    viewModel.onEvent(FriendScreenUiEvent.SendFriendRequest(user.uid))
                    requestSend = true

                }
            }
            searchState.errorMessage?.let {
                if(it.startsWith("Failed to send") || it.startsWith("Cannot send")) { // Fehler, die das Senden betreffen
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    requestSend = false
                }else if(it.startsWith("Friend request sent!")){
                    navController.navigate(NavRoute.Home.route)
                }
            }
        }
    }

}

