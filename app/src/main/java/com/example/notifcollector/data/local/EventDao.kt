package com.example.notifcollector.data.local

import androidx.room.*

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: EventEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long
    
    @Update
    suspend fun update(event: EventEntity)
    
    @Query("SELECT * FROM events WHERE uploaded = 0")
    suspend fun loadPending(): List<EventEntity>
    
    @Query("SELECT * FROM events ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<EventEntity>
    
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM events WHERE uploaded = 1 AND createdAt < :before")
    suspend fun deleteUploadedBefore(before: Long)
}