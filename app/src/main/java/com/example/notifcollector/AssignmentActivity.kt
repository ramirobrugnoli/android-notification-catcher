@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.notifcollector

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.notifcollector.auth.AuthManager
import com.example.notifcollector.auth.LoginActivity
import com.example.notifcollector.data.WalletAssignmentManager
import com.example.notifcollector.data.net.*
import kotlinx.coroutines.launch
import timber.log.Timber

class AssignmentActivity : ComponentActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var apiService: ApiService
    private lateinit var walletAssignmentManager: WalletAssignmentManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = AuthManager(this)
        apiService = Net.apiService
        walletAssignmentManager = WalletAssignmentManager(this)
        
        if (!authManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContent {
            AssignmentScreen()
        }
    }
    
    @Composable
    private fun AssignmentScreen() {
        var users by remember { mutableStateOf<List<UserSummary>>(emptyList()) }
        var assignments by remember { mutableStateOf<List<com.example.notifcollector.data.net.WalletAssignmentResponse>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var selectedUserId by remember { mutableStateOf<String?>(null) }
        var selectedProvider by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(Unit) {
            loadUsers { userList ->
                users = userList
            }
            loadDeviceAssignments { assignmentList ->
                assignments = assignmentList
                isLoading = false
            }
        }
        
        MaterialTheme {
            Scaffold(
                topBar = { 
                    TopAppBar(
                        title = { Text("Wallet Assignment") },
                        actions = {
                            TextButton(onClick = {
                                authManager.logout()
                                startActivity(Intent(this@AssignmentActivity, LoginActivity::class.java))
                                finish()
                            }) {
                                Text("Logout", color = Color.Red)
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Admin: ${authManager.getUserName() ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                authManager.getUserEmail() ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "Wallet Assignment",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedProvider = "uala" },
                            colors = if (selectedProvider == "uala") 
                                ButtonDefaults.buttonColors() 
                            else 
                                ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ualá")
                        }
                        
                        Button(
                            onClick = { selectedProvider = "lemon" },
                            colors = if (selectedProvider == "lemon") 
                                ButtonDefaults.buttonColors() 
                            else 
                                ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lemon")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(users) { user ->
                                UserCard(
                                    user = user,
                                    isSelected = selectedUserId == user.id,
                                    selectedProvider = selectedProvider,
                                    assignments = assignments.filter { it.userId == user.id },
                                    onSelect = { selectedUserId = user.id },
                                    onAssign = { 
                                        if (selectedProvider != null) {
                                            assignWallet(user.id, selectedProvider!!) {
                                                loadDeviceAssignments { assignmentList ->
                                                    assignments = assignmentList
                                                }
                                            }
                                        }
                                    },
                                    onRemove = { provider ->
                                        removeAssignment(user.id, provider) {
                                            loadDeviceAssignments { assignmentList ->
                                                assignments = assignmentList
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun UserCard(
        user: UserSummary,
        isSelected: Boolean,
        selectedProvider: String?,
        assignments: List<com.example.notifcollector.data.net.WalletAssignmentResponse>,
        onSelect: () -> Unit,
        onAssign: () -> Unit,
        onRemove: (String) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = if (isSelected) CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) else CardDefaults.cardColors()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            user.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { if (it) onSelect() }
                    )
                }
                
                if (isSelected && selectedProvider != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAssign,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Assign ${selectedProvider.uppercase()} Wallet")
                    }
                }
                
                // Show existing assignments for this device
                Spacer(modifier = Modifier.height(8.dp))
                if (assignments.isNotEmpty()) {
                    Text(
                        "Current assignments:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    assignments.forEach { assignment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                assignment.provider.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { onRemove(assignment.provider) }
                            ) {
                                Text("Remove", color = Color.Red)
                            }
                        }
                    }
                } else {
                    Text(
                        "Current assignments: None",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
    
    private fun loadUsers(onResult: (List<UserSummary>) -> Unit) {
        lifecycleScope.launch {
            try {
                val bearerToken = authManager.getBearerToken()
                Timber.d("Bearer token: $bearerToken")
                if (bearerToken != null) {
                    val response = apiService.listUsers(bearerToken)
                    onResult(response.users)
                } else {
                    Timber.e("No bearer token available")
                    Timber.d("Access token: ${authManager.getAccessToken()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load users")
                onResult(emptyList())
            }
        }
    }
    
    private fun loadDeviceAssignments(onResult: (List<com.example.notifcollector.data.net.WalletAssignmentResponse>) -> Unit) {
        lifecycleScope.launch {
            try {
                val assignments = walletAssignmentManager.getDeviceAssignments()
                onResult(assignments)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load device assignments")
                onResult(emptyList())
            }
        }
    }
    
    private fun assignWallet(userId: String, provider: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                walletAssignmentManager.assignWallet(userId, provider)
                Timber.i("Assigned $provider wallet to user $userId")
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to assign wallet")
            }
        }
    }
    
    private fun removeAssignment(userId: String, provider: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                walletAssignmentManager.removeAssignment(userId, provider)
                Timber.i("Removed $provider wallet from user $userId")
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove assignment")
            }
        }
    }
}