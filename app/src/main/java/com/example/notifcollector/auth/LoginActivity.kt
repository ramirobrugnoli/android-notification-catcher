package com.example.notifcollector.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.notifcollector.MainActivity
import com.example.notifcollector.data.net.*
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginActivity : ComponentActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var apiService: ApiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = AuthManager(this)
        apiService = Net.apiService
        
        // Si ya está logueado, ir directo a MainActivity
        if (authManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setContent {
            LoginScreen()
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LoginScreen() {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Admin Login",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    performLogin(email, password) { error ->
                        errorMessage = error
                        isLoading = false
                    }
                    isLoading = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }
            
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    
    private fun performLogin(email: String, password: String, onError: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val loginRequest = LoginRequest(email = email, password = password)
                Timber.d("Login request: email=$email")
                
                val loginResponse = apiService.login(loginRequest)
                Timber.d("Login response: accessToken=${loginResponse.accessToken}, user=${loginResponse.user}")
                
                // Guardar datos de login
                authManager.saveLoginData(
                    accessToken = loginResponse.accessToken,
                    userId = loginResponse.user.id,
                    email = loginResponse.user.email,
                    name = loginResponse.user.name
                )
                
                Timber.d("Login data saved successfully")
                
                // Navegar a MainActivity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
                
            } catch (e: Exception) {
                Timber.e(e, "Login failed with exception: ${e.message}")
                
                // Mostrar más detalles del error
                val errorMessage = when {
                    e.message?.contains("HTTP 401") == true -> "Credenciales inválidas"
                    e.message?.contains("HTTP 404") == true -> "Endpoint no encontrado"
                    e.message?.contains("HTTP 500") == true -> "Error del servidor"
                    e.message?.contains("Unable to resolve host") == true -> "Sin conexión a internet"
                    else -> "Error: ${e.message ?: "Desconocido"}"
                }
                
                onError(errorMessage)
            }
        }
    }
}