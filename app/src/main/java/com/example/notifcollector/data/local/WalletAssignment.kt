package com.example.notifcollector.data.local

import androidx.room.*

@Entity(tableName = "wallet_assignments", primaryKeys = ["provider"])
data class WalletAssignment(
    val provider: String,  // "uala" | "lemon"
    val userId: String,    // ID del usuario final
    val assignedAt: Long = System.currentTimeMillis()
)

@Dao
interface WalletAssignmentDao {
    @Query("SELECT * FROM wallet_assignments WHERE provider = :provider LIMIT 1")
    suspend fun getAssignmentForProvider(provider: String): WalletAssignment?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: WalletAssignment)
    
    @Query("DELETE FROM wallet_assignments WHERE provider = :provider")
    suspend fun deleteAssignmentForProvider(provider: String)
    
    @Query("SELECT * FROM wallet_assignments")
    suspend fun getAllAssignments(): List<WalletAssignment>
}