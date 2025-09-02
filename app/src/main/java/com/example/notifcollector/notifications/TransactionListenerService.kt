package com.example.notifcollector.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notifcollector.data.WalletAssignmentManager
import com.example.notifcollector.data.Repository
import com.example.notifcollector.data.local.EventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class TransactionListenerService : NotificationListenerService() {
    
    private lateinit var walletAssignmentManager: WalletAssignmentManager
    private lateinit var repository: Repository
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        walletAssignmentManager = WalletAssignmentManager(this)
        repository = Repository(this)
        Timber.i("🔌 NotificationListener CONNECTED")
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Timber.w("🔌 NotificationListener DISCONNECTED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        com.example.notifcollector.notifications.logRawNotification(sbn)

        val pkg = sbn.packageName ?: return
        val extras = sbn.notification.extras
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").trim()
        val text  = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").trim()
        val big   = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "").trim()
        val body = listOf(title, if (big.isNotEmpty()) big else text).joinToString(" | ").trim()

        val provider = detectSource(pkg) // devuelve "uala" | "lemon" | ...
        if (provider == "unknown") return

        val amountCur = parseAmountAndCurrency(body) ?: return
        val (amount, currency) = amountCur

        val type =
            if (body.contains("acredit", true) || body.contains("recib", true) || body.contains("ingres", true))
                "transfer_in"
            else "unknown"

        val counterpartyName = Regex("""\bde\s+([^\n|]+)$""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)?.trim()

        val ref = Regex("""(?:Ref(?:erencia)?|Alias|CBU|CVU|Id)[:\s]+(.+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)?.trim()

        val occurredAt = sbn.postTime
        val dedup = stableDedupKey(provider, amount, occurredAt, ref, body)

        serviceScope.launch {
            // Get the userId assigned to this provider
            val userId = walletAssignmentManager.getUserIdForProvider(provider)
            
            if (userId == null) {
                Timber.w("No user assigned to provider $provider, skipping notification")
                return@launch
            }
            
            val entity = EventEntity(
                provider = provider,
                type = type,
                amount = amount,
                currency = currency,
                occurredAt = occurredAt,
                counterpartyName = counterpartyName,
                counterpartyAccount = null,
                reference = ref,
                rawPackage = pkg,
                rawTitle = title,
                rawText = text,
                rawBigText = big,
                dedupKey = dedup
            )
            
            // Send to backend with userId
            try {
                repository.sendEventWithUserId(entity, userId)
                Timber.i("✅ Sent event for provider $provider to user $userId")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to send event for provider $provider")
            }
        }
    }
}

// Helper functions
fun detectSource(packageName: String): String = when {
    packageName.contains("uala", true) -> "uala"
    packageName.contains("lemon", true) -> "lemon"
    packageName.contains("applemoncash", true) -> "lemon"
    else -> "unknown"
}

fun parseAmountAndCurrency(body: String): Pair<Double, String>? {
    // Try format: "1.234,56 ARS" or "1 ARS"
    val regex1 = Regex("""([0-9.,]+)\s+(ARS|USD|USDC)""", RegexOption.IGNORE_CASE)
    regex1.find(body)?.let { match ->
        val amountStr = match.groupValues[1]
        val currency = match.groupValues[2].uppercase()
        return try {
            val amount = normalizeAmount(amountStr)
            Pair(amount, currency)
        } catch (e: Exception) {
            null
        }
    }
    
    // Try format: "$1.234,56"
    val regex2 = Regex("""(?:\$|AR\$|USD?)\s?([0-9.,]+)""", RegexOption.IGNORE_CASE)
    regex2.find(body)?.let { match ->
        val amountStr = match.groupValues[1]
        return try {
            val amount = normalizeAmount(amountStr)
            Pair(amount, "ARS") // Default to ARS
        } catch (e: Exception) {
            null
        }
    }
    
    return null
}

fun stableDedupKey(provider: String, amount: Double, occurredAt: Long, ref: String?, body: String): String {
    val seed = "$provider|$amount|$occurredAt|${ref ?: ""}|${body.take(64)}"
    return java.security.MessageDigest.getInstance("SHA-256")
        .digest(seed.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

private fun normalizeAmount(raw: String): Double {
    val cleaned = raw.replace("\\s".toRegex(), "")
    val normalized = if (cleaned.count { it == ',' } == 1 && cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
        // Formato LATAM: puntos miles, coma decimal
        cleaned.replace(".", "").replace(",", ".")
    } else {
        // US format o sin miles
        cleaned.replace(",", "")
    }
    return normalized.toDouble()
}
