package com.example.notifcollector.data

import android.content.Context
import com.example.notifcollector.auth.AuthManager
import com.example.notifcollector.config.Settings
import com.example.notifcollector.data.local.AppDb
import com.example.notifcollector.data.local.EventEntity
import com.example.notifcollector.data.net.CreateWalletEventPayload
import com.example.notifcollector.data.net.Net
import com.example.notifcollector.data.net.RawNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class Repository(context: Context) {
    private val appCtx = context.applicationContext
    private val db = AppDb.get(appCtx)
    private val dao = db.eventDao()
    private val authManager = AuthManager(appCtx)
    private val apiService = Net.apiService

    suspend fun saveEventLocal(e: EventEntity): Long =
        withContext(Dispatchers.IO) { dao.insertIgnore(e) }

    suspend fun uploadPending() = withContext(Dispatchers.IO) {
        val bearer = Settings.bearer(appCtx) ?: run {
            Timber.w("Bearer no configurado")
            return@withContext
        }
        val api = apiService
        val pending = dao.loadPending()
        for (e in pending) {
            try {
                val userId = Settings.assignedUser(appCtx, e.provider)
                if (userId.isNullOrBlank()) {
                    Timber.i("Sin asignación local para provider=%s; skip", e.provider)
                    continue
                }
                val payload = CreateWalletEventPayload(
                    userId = userId,
                    provider = e.provider,                  // "uala" | "lemon"
                    type = e.type,                          // "transfer_in" o "debit"
                    amount = e.amount,
                    currency = e.currency,
                    occurredAt = e.occurredAt,
                    counterpartyName = e.counterpartyName,
                    counterpartyAccount = e.counterpartyAccount,
                    reference = e.reference,
                    dedupKey = e.dedupKey,
                    raw = RawNotification(
                        `package` = e.rawPackage,
                        title = e.rawTitle,
                        text = e.rawText,
                        bigText = e.rawBigText
                    )
                )
                apiService.postEvent("Bearer $bearer", e.dedupKey, payload)
                dao.update(e.copy(uploaded = true))
            } catch (t: Throwable) {
                Timber.w(t, "Upload failed; will retry")
            }
        }
    }
    
    suspend fun sendEventWithUserId(event: EventEntity, userId: String) = withContext(Dispatchers.IO) {
        try {
            val bearerToken = authManager.getBearerToken()
            if (bearerToken == null) {
                Timber.e("No bearer token available")
                return@withContext
            }
            
            val payload = CreateWalletEventPayload(
                userId = userId,
                provider = event.provider,
                type = event.type,
                amount = event.amount,
                currency = event.currency,
                occurredAt = event.occurredAt,
                counterpartyName = event.counterpartyName,
                counterpartyAccount = event.counterpartyAccount,
                reference = event.reference,
                dedupKey = event.dedupKey,
                raw = RawNotification(
                    `package` = event.rawPackage,
                    title = event.rawTitle,
                    text = event.rawText,
                    bigText = event.rawBigText
                )
            )
            
            apiService.postEvent(bearerToken, event.dedupKey, payload)
            
            // Save locally as uploaded
            dao.insert(event.copy(uploaded = true))
            
            Timber.i("✅ Event sent successfully for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to send event with userId")
            // Save locally as pending for retry
            dao.insert(event.copy(uploaded = false))
            throw e
        }
    }
}