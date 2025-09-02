package com.example.notifcollector.auth

import android.content.Context
import android.content.SharedPreferences

class AuthManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
    
    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }
    
    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }
    
    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }
    
    fun getUserName(): String? {
        return prefs.getString("user_name", null)
    }
    
    fun getBearerToken(): String? {
        val token = getAccessToken()
        return if (token != null) "Bearer $token" else null
    }
    
    fun saveLoginData(accessToken: String, userId: String, email: String, name: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()
    }
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}