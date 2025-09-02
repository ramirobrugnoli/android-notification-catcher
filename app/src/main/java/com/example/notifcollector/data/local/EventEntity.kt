package com.example.notifcollector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: String,           // "uala" | "lemon"
    val type: String,
    val amount: Double,
    val currency: String,
    val occurredAt: Long,
    val counterpartyName: String?,
    val counterpartyAccount: String?,
    val reference: String?,
    // RAW capturado
    val rawPackage: String,
    val rawTitle: String,
    val rawText: String,
    val rawBigText: String,
    val dedupKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val uploaded: Boolean = false
)