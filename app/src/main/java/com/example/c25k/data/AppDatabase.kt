package com.example.c25k.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PlanSessionEntity::class,
        PlanSegmentEntity::class,
        WorkoutEntity::class,
        WorkoutSegmentEntity::class,
        TrackPointEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "c25k.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
