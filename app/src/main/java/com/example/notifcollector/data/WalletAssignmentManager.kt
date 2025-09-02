package com.example.notifcollector.data

import android.content.Context
import com.example.notifcollector.auth.AuthManager
import com.example.notifcollector.data.local.AppDb
import com.example.notifcollector.data.local.WalletAssignment
import com.example.notifcollector.data.net.Net
import timber.log.Timber

class WalletAssignmentManager(private val context: Context) {
    
    private val db = AppDb.get(context)
    private val authManager = AuthManager(context)
    private val apiService = Net.apiService
    
    suspend fun assignWallet(userId: String, provider: String) {
        try {
            // Call backend API
            val bearerToken = authManager.getBearerToken()
            if (bearerToken != null) {
                apiService.createAssignment(
                    bearerToken,
                    mapOf("userId" to userId, "provider" to provider)
                )
                
                // Store locally
                db.walletAssignmentDao().insertAssignment(
                    WalletAssignment(
                        provider = provider,
                        userId = userId
                    )
                )
                
                Timber.i("Assigned $provider wallet to user $userId")
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
            // Call backend API
            val bearerToken = authManager.getBearerToken()
            if (bearerToken != null) {
                apiService.deleteAssignment(bearerToken, userId, provider)
                
                // Remove locally
                db.walletAssignmentDao().deleteAssignmentForProvider(provider)
                
                Timber.i("Removed $provider wallet from user $userId")
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
            db.walletAssignmentDao().getAssignmentForProvider(provider)?.userId
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user ID for provider $provider")
            null
        }
    }
    
    suspend fun syncAssignments() {
        try {
            val bearerToken = authManager.getBearerToken()
            if (bearerToken != null) {
                // TODO: Implement endpoint to fetch current assignments
                // For now, we rely on local storage only
                Timber.i("Assignment sync not implemented yet")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync assignments")
        }
    }
}