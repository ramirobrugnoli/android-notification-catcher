package com.example.notifcollector.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Settings {
    const val BASE_URL = "https://wallet-manager-production-c194.up.railway.app"
    
    private const val PREFS = "ingest_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_BEARER   = "bearer"
    private const val KEY_MAP_PREFIX = "map_" // map_lemon, map_uala => userId

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setBaseUrl(ctx: Context, url: String) = prefs(ctx).edit().putString(KEY_BASE_URL, url).apply()
    fun baseUrl(ctx: Context): String? = prefs(ctx).getString(KEY_BASE_URL, null)

    fun setBearer(ctx: Context, token: String) = prefs(ctx).edit().putString(KEY_BEARER, token).apply()
    fun bearer(ctx: Context): String? = prefs(ctx).getString(KEY_BEARER, null)

    fun assign(ctx: Context, provider: String, userId: String?) =
        prefs(ctx).edit().putString(KEY_MAP_PREFIX + provider.lowercase(), userId).apply()

    fun assignedUser(ctx: Context, provider: String): String? =
        prefs(ctx).getString(KEY_MAP_PREFIX + provider.lowercase(), null)
}