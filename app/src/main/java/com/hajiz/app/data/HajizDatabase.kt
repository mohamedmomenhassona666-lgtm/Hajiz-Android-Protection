package com.hajiz.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockedDomain::class], version = 1, exportSchema = false)
abstract class HajizDatabase : RoomDatabase() {
    abstract fun blockedDomainDao(): BlockedDomainDao

    companion object {
        @Volatile private var instance: HajizDatabase? = null

        fun getInstance(context: Context): HajizDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HajizDatabase::class.java,
                    "hajiz.db",
                ).build().also { instance = it }
            }
    }
}