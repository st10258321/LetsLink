package com.example.API_related

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlin.jvm.java
import com.example.letslink.model.User

@Database(entities = [User::class,Group::class], version = 6, exportSchema = false)
@TypeConverters(UUIDConverter::class)
abstract class LetsLinkDB : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    companion object {
        @Volatile
        private var INSTANCE: LetsLinkDB? = null

        fun getDatabase(context: Context): LetsLinkDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LetsLinkDB::class.java,
                    "eddieDB.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}