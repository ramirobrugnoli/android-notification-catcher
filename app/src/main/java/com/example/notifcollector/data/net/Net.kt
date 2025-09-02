package com.example.notifcollector.data.net

import android.content.Context
import com.example.notifcollector.config.Settings
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Net {
    fun api(ctx: Context): ApiService {
        val base = Settings.baseUrl(ctx) ?: error("Base URL no configurada")
        val norm = if (base.endsWith("/")) base else "$base/"
        val client = OkHttpClient.Builder().build()
        return Retrofit.Builder()
            .baseUrl(norm)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    // Static instance using BASE_URL from Settings
    val apiService: ApiService by lazy {
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl("${Settings.BASE_URL}/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}