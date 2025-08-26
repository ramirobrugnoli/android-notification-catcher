package com.example.notifcollector.notifications

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "RAW"           // fácil de filtrar en Logcat
private const val CHUNK = 3500          // ~4k es el límite por línea

// Convierte Bundle → JSON (sin romper si hay arrays/objetos)
private fun Any?.toJsonValue(): Any? = when (this) {
    null -> JSONObject.NULL
    is Bundle -> JSONObject(this.keySet().associateWith { get(it).toJsonValue() })
    is Array<*> -> JSONArray(this.map { it?.toString() })
    is Iterable<*> -> JSONArray(this.map { it?.toString() })
    is CharSequence -> this.toString()
    else -> this
}
private fun bundleToJson(bundle: Bundle?): String {
    if (bundle == null) return "{}"
    val map = bundle.keySet().associateWith { bundle.get(it).toJsonValue() }
    return JSONObject(map).toString()
}

// Evita que Android recorte logs largos
private fun logLong(tag: String, text: String) {
    var i = 0
    while (i < text.length) {
        val end = (i + CHUNK).coerceAtMost(text.length)
        Log.i(tag, text.substring(i, end))
        i = end
    }
}

// Llamalo desde onNotificationPosted
fun logRawNotification(sbn: StatusBarNotification) {
    val n = sbn.notification
    val extras = n.extras
    val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").trim()
    val text  = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").trim()
    val big   = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "").trim()

    val json = JSONObject(
        mapOf(
            "pkg" to sbn.packageName,
            "postTime" to sbn.postTime,
            "category" to n.category,
            "title" to title,
            "text" to text,
            "bigText" to big,
            "extras" to JSONObject(bundleToJson(extras))
        )
    ).toString()

    logLong(TAG, json)
}
