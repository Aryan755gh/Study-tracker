package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        StudySessionEntity::class,
        DailyLogEntity::class,
        GamificationProgressEntity::class,
        TaskCompletionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyOSDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun taskCompletionDao(): TaskCompletionDao

    companion object {
        @Volatile
        private var INSTANCE: StudyOSDatabase? = null

        fun getDatabase(context: Context): StudyOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyOSDatabase::class.java,
                    "study_os_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
