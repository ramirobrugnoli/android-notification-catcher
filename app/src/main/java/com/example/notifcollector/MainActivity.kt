@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.notifcollector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.notifcollector.auth.AuthManager

class MainActivity : ComponentActivity() {
    
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = AuthManager(this)
        
        setContent { App() }
    }

    @Composable
    private fun App() {
        val ctx = this

        // Estado del permiso de notificaciones (granted en <33 por compatibilidad)
        val initialGranted = remember {
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        }
        var notifGranted by remember { mutableStateOf(initialGranted) }

        // Launcher para pedir el permiso en Android 13+
        val notifPermLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted -> notifGranted = granted }

        // Pedir el permiso automáticamente al abrir (solo 33+)
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= 33 && !notifGranted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        MaterialTheme {
            Scaffold(topBar = { TopAppBar(title = { Text("Notif Collector") }) }) { pv ->
                Column(
                    Modifier
                        .padding(pv)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Estado del permiso
                    Text(
                        if (Build.VERSION.SDK_INT >= 33)
                            "Permiso de notificaciones: ${if (notifGranted) "concedido" else "pendiente"}"
                        else
                            "Esta versión de Android no requiere permiso de notificaciones."
                    )
                    Spacer(Modifier.height(12.dp))

                    if (Build.VERSION.SDK_INT >= 33 && !notifGranted) {
                        Button(onClick = {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) { Text("Conceder permiso") }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Abrir ajustes para habilitar el Notification Listener
                    Button(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text("Habilitar acceso a notificaciones (listener)") }

                    Spacer(Modifier.height(24.dp))
                    
                    // Navigate to assignment screen
                    Button(onClick = {
                        ctx.startActivity(Intent(ctx, AssignmentActivity::class.java))
                    }) {
                        Text("Wallet Assignment")
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { TestNotifs.sendExample(ctx) }) {
                        Text("Notificación de prueba")
                    }

                }
            }
        }
    }
}
