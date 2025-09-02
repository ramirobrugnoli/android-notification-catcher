package com.example.notifcollector.data

import android.content.Context
import com.example.notifcollector.auth.AuthManager
import com.example.notifcollector.data.local.AppDb
import com.example.notifcollector.data.local.WalletAssignment
import com.example.notifcollector.data.net.CreateAssignmentRequest
import com.example.notifcollector.data.net.Net
import com.example.notifcollector.data.net.WalletAssignmentResponse
import com.example.notifcollector.utils.DeviceUtils
import timber.log.Timber

class WalletAssignmentManager(private val context: Context) {
    
    private val db = AppDb.get(context)
    private val authManager = AuthManager(context)
    private val apiService = Net.apiService
    
    suspend fun assignWallet(userId: String, provider: String) {
        try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val bearerToken = authManager.getBearerToken()
            if (bearerToken != null) {
                // Call backend API
                val response = apiService.createAssignment(
                    bearerToken,
                    CreateAssignmentRequest(userId, provider, deviceId)
                )
                
                // Store locally
                db.walletAssignmentDao().insertAssignment(
                    WalletAssignment(
                        provider = provider,
                        userId = userId,
                        deviceId = deviceId
                    )
                )
                
                Timber.i("Assigned $provider wallet to user $userId on device $deviceId")
            } else {
                Timber.e("No bearer token available")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to assign wallet")
            throw e
        }
    }
    
    suspend fun removeAssignment(userId: String, provider: String) {
        try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val bearerToken = authManager.getBearerToken()
            if (bearerToken != null) {
                // Call backend API
                apiService.deleteAssignment(bearerToken, userId, provider)
                
                // Remove locally
                db.walletAssignmentDao().deleteAssignmentForProvider(provider, deviceId)
                
                Timber.i("Removed $provider wallet from user $userId on device $deviceId")
            } else {
                Timber.e("No bearer token available")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove assignment")
            throw e
        }
    }
    
    suspend fun getUserIdForProvider(provider: String): String? {
        return try {
            val deviceId = DeviceUtils.getDeviceId(context)
            db.walletAssignmentDao().getAssignmentForProvider(provider, deviceId)?.userId
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user ID for provider $provider")
            null
        }
    }
    
    suspend fun getDeviceAssignments(): List<WalletAssignmentResponse> {
        return try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val bearerToken = authManager.getBearerToken()
            Timber.d("Getting device assignments for deviceId: $deviceId")
            Timber.d("Bearer token: $bearerToken")
            if (bearerToken != null) {
                val result = apiService.getDeviceAssignments(bearerToken, deviceId)
                Timber.d("Found ${result.size} assignments")
                result
            } else {
                Timber.e("No bearer token available")
                Timber.d("Access token: ${authManager.getAccessToken()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get device assignments")
            emptyList()
        }
    }
    
    suspend fun syncAssignments() {
        try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val bearerToken = authManager.getBearerToken()
            if (bearerToken != null) {
                val assignments = apiService.getDeviceAssignments(bearerToken, deviceId)
                
                // Clear local assignments for this device and sync with backend
                val localAssignments = db.walletAssignmentDao().getAssignmentsForDevice(deviceId)
                for (assignment in assignments) {
                    db.walletAssignmentDao().insertAssignment(
                        WalletAssignment(
                            provider = assignment.provider,
                            userId = assignment.userId,
                            deviceId = deviceId
                        )
                    )
                }
                
                Timber.i("Synced ${assignments.size} assignments for device $deviceId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync assignments")
        }
    }
}