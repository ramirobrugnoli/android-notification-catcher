package com.example.notifcollector.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventEntity::class, WalletAssignment::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun walletAssignmentDao(): WalletAssignmentDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(context: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "notif-db"
            )
                .fallbackToDestructiveMigration()  // <- simple para dev
                .build().also { INSTANCE = it }
        }
    }
}