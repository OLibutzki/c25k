package de.libutzki.c25k.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workouts ADD COLUMN runPaceSecPerKm REAL")
            db.execSQL("ALTER TABLE workouts ADD COLUMN walkPaceSecPerKm REAL")
            db.execSQL(
                """
                UPDATE workouts
                SET runPaceSecPerKm = (
                    SELECT CASE
                        WHEN SUM(distanceMeters) > 0 AND SUM(durationSec) > 0
                            THEN (SUM(durationSec) * 1000.0) / SUM(distanceMeters)
                        ELSE NULL
                    END
                    FROM workout_segments
                    WHERE workoutId = workouts.id AND type = 'RUN'
                ),
                walkPaceSecPerKm = (
                    SELECT CASE
                        WHEN SUM(distanceMeters) > 0 AND SUM(durationSec) > 0
                            THEN (SUM(durationSec) * 1000.0) / SUM(distanceMeters)
                        ELSE NULL
                    END
                    FROM workout_segments
                    WHERE workoutId = workouts.id AND type = 'WALK'
                )
                """.trimIndent()
            )
        }
    }

    /**
     * Append new Room migrations here when increasing [AppDatabase] version.
     *
     * Keeping this explicit prevents silent data loss on production upgrades.
     */
    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6)
}
