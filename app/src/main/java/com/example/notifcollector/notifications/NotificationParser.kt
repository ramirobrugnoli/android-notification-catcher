package com.example.notifcollector.notifications

import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

// ─── Salida normalizada (lo que consumirá tu backend) ─────────────────────────
data class NormalizedEvent(
    val provider: String,        // lemon | uala | mp | brubank | unknown
    val type: String,            // transfer_in | payment_in | ...
    val amount: Double,
    val currency: String,        // ARS | USD | USDC ...
    val occurredAt: Long,        // sbn.postTime (epoch millis)
    val counterpartyName: String? = null,
    val counterpartyAccount: String? = null, // Alias/CBU/CVU si aparece
    val reference: String? = null,           // "Referencia/Id ..." si aparece
    val packageName: String,
    val rawTitle: String?,
    val rawText: String?,
    val rawBigText: String?,
    val dedupKey: String          // sha256 estable para idempotencia
) {
    fun toJson(): String = JSONObject(
        mapOf(
            "provider" to provider,
            "type" to type,
            "amount" to amount,
            "currency" to currency,
            "occurredAt" to occurredAt,
            "counterpartyName" to counterpartyName,
            "counterpartyAccount" to counterpartyAccount,
            "reference" to reference,
            "dedupKey" to dedupKey,
            "raw" to mapOf(
                "package" to packageName,
                "title" to rawTitle,
                "text" to rawText,
                "bigText" to rawBigText
            )
        )
    ).toString()
}

// ─── Router principal ─────────────────────────────────────────────────────────
object NotificationParser {

    fun detectProvider(packageName: String): String = when {
        packageName.contains("mercadopago", true) -> "mercadopago"
        packageName.contains("uala", true)        -> "uala"
        packageName.contains("brubank", true)     -> "brubank"
        packageName.contains("applemoncash", true) -> "lemon"   // ← LEMON real: com.applemoncash
        packageName.contains("lemon", true)       -> "lemon"
        else -> "unknown"
    }

    /**
     * Intenta parsear a NormalizedEvent. Devuelve null si no reconoce el formato.
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        occurredAt: Long
    ): NormalizedEvent? {
        val provider = detectProvider(packageName)
        val effectiveText = when {
            !bigText.isNullOrBlank() -> bigText
            !text.isNullOrBlank()    -> text
            else -> title ?: ""
        }.trim()

        return when (provider) {
            "lemon" -> parseLemon(packageName, title, text, bigText, occurredAt, effectiveText)
            // "uala" -> TODO cuando tengamos ejemplos
            // "mercadopago" -> TODO
            // "brubank" -> TODO
            else -> parseGeneric(packageName, title, text, bigText, occurredAt, effectiveText)
        }
    }

    // ─── LEMON ────────────────────────────────────────────────────────────────
    // Ejemplo real tuyo:
    // title=Recibiste una transferencia 💸
    // text/big=Recibiste 1 ARS de Ramiro Brugnoli
    private fun parseLemon(
        packageName: String,
        title: String?, text: String?, bigText: String?,
        occurredAt: Long,
        body: String
    ): NormalizedEvent? {
        // 1) Recibiste 1 ARS de Nombre
        val r1 = Regex(
            """Recibiste\s+([0-9.,]+)\s+(ARS|USD|USDC)\s+de\s+(.+?)$""",
            setOf(RegexOption.IGNORE_CASE)
        )
        r1.find(body)?.let { m ->
            val amount = normalizeAmount(m.groupValues[1])
            val currency = m.groupValues[2].uppercase(Locale.ROOT)
            val name = m.groupValues[3].trim()

            val dedup = dedupKey(
                provider = "lemon",
                type = "transfer_in",
                amount = amount,
                currency = currency,
                occurredAt = occurredAt,
                ref = null,
                body = body
            )
            return NormalizedEvent(
                provider = "lemon",
                type = "transfer_in",
                amount = amount,
                currency = currency,
                occurredAt = occurredAt,
                counterpartyName = name,
                packageName = packageName,
                rawTitle = title,
                rawText = text,
                rawBigText = bigText,
                dedupKey = dedup
            )
        }

        // Si cambia el copy, intentamos con genérico
        return parseGeneric(packageName, title, text, bigText, occurredAt, body)
    }

    // ─── Genérico (fallback): intenta captar "Recibiste $X ..." o "$X ARS ..." ─
    private fun parseGeneric(
        packageName: String,
        title: String?, text: String?, bigText: String?,
        occurredAt: Long,
        body: String
    ): NormalizedEvent? {
        // a) "$ 1.234,56" o "US$ 10"
        val moneyLeading = Regex("""(?:(US\$|USD|AR\$|\$)\s?)([0-9.\,]+)""", RegexOption.IGNORE_CASE)
        // b) "1 ARS" / "10 USD" / "20 USDC"
        val moneyTrailing = Regex("""([0-9.\,]+)\s+(ARS|USD|USDC)""", RegexOption.IGNORE_CASE)

        var amount: Double? = null
        var currency: String? = null

        moneyLeading.find(body)?.let { m ->
            amount = normalizeAmount(m.groupValues[2])
            currency = when (m.groupValues[1].uppercase(Locale.ROOT)) {
                "US$", "USD" -> "USD"
                else -> "ARS"
            }
        }
        if (amount == null || currency == null) {
            moneyTrailing.find(body)?.let { m ->
                amount = normalizeAmount(m.groupValues[1])
                currency = m.groupValues[2].uppercase(Locale.ROOT)
            }
        }

        if (amount == null || currency == null) return null

        val isIncoming = body.contains("recib", true) || body.contains("acredit", true) || body.contains("transferencia", true)
        val type = if (isIncoming) "transfer_in" else "unknown"

        val ref = Regex("""(?:Ref(?:erencia)?|Id)[:\s]+([A-Za-z0-9\.\-\_]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)

        val name = Regex("""de\s+([^\n|]+)$""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)?.trim()

        val dedup = dedupKey(
            provider = detectProvider(packageName),
            type = type,
            amount = amount!!,
            currency = currency!!,
            occurredAt = occurredAt,
            ref = ref,
            body = body
        )

        return NormalizedEvent(
            provider = detectProvider(packageName),
            type = type,
            amount = amount!!,
            currency = currency!!,
            occurredAt = occurredAt,
            counterpartyName = name,
            reference = ref,
            packageName = packageName,
            rawTitle = title,
            rawText = text,
            rawBigText = bigText,
            dedupKey = dedup
        )
    }

    // ─── Utils ────────────────────────────────────────────────────────────────
    private fun normalizeAmount(raw: String): Double {
        // "1.234,56" -> 1234.56  |  "1,234.56" -> 1234.56  |  "1" -> 1.0
        val cleaned = raw.replace("\\s".toRegex(), "")
        val normalized = if (cleaned.count { it == ',' } == 1 && cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
            // Formato LATAM/ES: puntos miles, coma decimal
            cleaned.replace(".", "").replace(",", ".")
        } else {
            // US format o sin miles
            cleaned.replace(",", "")
        }
        return normalized.toDouble()
    }

    private fun dedupKey(
        provider: String,
        type: String,
        amount: Double,
        currency: String,
        occurredAt: Long,
        ref: String?,
        body: String
    ): String {
        val seed = "$provider|$type|$amount|$currency|$occurredAt|${ref ?: ""}|${body.take(64)}"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(seed.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
