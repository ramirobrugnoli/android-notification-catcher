package com.example.notifcollector.data.local

import androidx.room.*

@Entity(tableName = "wallet_assignments", primaryKeys = ["provider", "deviceId"])
data class WalletAssignment(
    val provider: String,  // "uala" | "lemon"
    val userId: String,    // ID del usuario final
    val deviceId: String,  // ID del dispositivo
    val assignedAt: Long = System.currentTimeMillis()
)

@Dao
interface WalletAssignmentDao {
    @Query("SELECT * FROM wallet_assignments WHERE provider = :provider AND deviceId = :deviceId LIMIT 1")
    suspend fun getAssignmentForProvider(provider: String, deviceId: String): WalletAssignment?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: WalletAssignment)
    
    @Query("DELETE FROM wallet_assignments WHERE provider = :provider AND deviceId = :deviceId")
    suspend fun deleteAssignmentForProvider(provider: String, deviceId: String)
    
    @Query("SELECT * FROM wallet_assignments WHERE deviceId = :deviceId")
    suspend fun getAssignmentsForDevice(deviceId: String): List<WalletAssignment>
    
    @Query("SELECT * FROM wallet_assignments")
    suspend fun getAllAssignments(): List<WalletAssignment>
}