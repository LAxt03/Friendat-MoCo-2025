package com.friendat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friendat.data.repository.UserAuthResult // Dein UserAuthResult
import com.friendat.ui.theme.BackGround
import com.friendat.ui.theme.Sekundary
import com.friendat.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {

    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val authUiState by authViewModel.authUiState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = authUiState) {
        when (val state = authUiState) { // state ist hier UserAuthResult?
            is UserAuthResult.Success -> {
                onLoginSuccess()
                authViewModel.clearAuthUiState() // Zustand im ViewModel zurücksetzen
            }
            is UserAuthResult.Failure -> {
                snackbarHostState.showSnackbar(
                    message = state.exception.message ?: "An unknown error occurred",
                    duration = SnackbarDuration.Short
                )
                authViewModel.clearAuthUiState() // Zustand im ViewModel zurücksetzen
            }
            null -> {

            }

        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(        modifier = Modifier.background(BackGround).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Login / Sign Up", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { authViewModel.onEmailChange(it) },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { authViewModel.onPasswordChange(it) },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(
                            onClick = { authViewModel.signIn() },
                            enabled = email.isNotBlank() && password.isNotBlank(),
                            colors = ButtonColors(Sekundary, Color.White, Color.White, Color.Gray)
                        ) {
                            Text("Sign In")
                        }
                        Button(
                            onClick = { authViewModel.signUp() },
                            enabled = email.isNotBlank() && password.isNotBlank(),
                            colors = ButtonColors(Sekundary, Color.White, Color.White, Color.Gray)
                        ) {
                            Text("Sign Up")
                        }
                    }
                }
                Spacer(Modifier.height(200.dp))
            }
        }
    }
}
