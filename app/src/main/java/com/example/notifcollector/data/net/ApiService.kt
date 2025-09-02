package com.example.notifcollector.data.net

import com.squareup.moshi.JsonClass
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class RawNotification(
    val `package`: String,
    val title: String,
    val text: String,
    val bigText: String
)

@JsonClass(generateAdapter = true)
data class CreateWalletEventPayload(
    val userId: String,          // <- requerido para enrutar al usuario final
    val provider: String,        // "uala" | "lemon"
    val type: String,            // "transfer_in" (o "debit" si preferís)
    val amount: Double,
    val currency: String,        // "ARS" | "USD"
    val occurredAt: Long,        // epoch ms
    val counterpartyName: String? = null,
    val counterpartyAccount: String? = null,
    val reference: String? = null,
    val dedupKey: String,
    val raw: RawNotification
)

@JsonClass(generateAdapter = true)
data class UserSummary(
    val id: String,
    val email: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class UsersResp(
    val users: List<UserSummary>,
    val total: Int
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val accessToken: String,
    val user: UserProfile
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val picture: String?
)

interface ApiService {
    @POST("/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @GET("/users")
    suspend fun listUsers(@Header("Authorization") bearer: String): UsersResp

    @POST("/assignments")
    suspend fun createAssignment(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String> // { userId, provider }
    )

    @DELETE("/assignments/{userId}/{provider}")
    suspend fun deleteAssignment(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String,
        @Path("provider") provider: String
    )

    @POST("/events")
    suspend fun postEvent(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") idempotency: String,
        @Body event: CreateWalletEventPayload
    )
}