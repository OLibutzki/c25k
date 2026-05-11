package de.libutzki.c25k.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate5To6_backfillsRunAndWalkPaceFromSegmentData() {
        val dbName = "migration-test-5-6"
        helper.createDatabase(dbName, 5).apply {
            execSQL(
                """
                INSERT INTO workouts
                    (id, sessionId, startedAtEpochMs, completedAtEpochMs, distanceMeters, avgPaceSecPerKm)
                VALUES
                    (1, 42, 1000, 2000, 2000.0, 360.0),
                    (2, 43, 3000, 4000, 1000.0, 600.0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_segments
                    (id, workoutId, segmentOrder, type, startEpochMs, endEpochMs, durationSec, distanceMeters, paceSecPerKm)
                VALUES
                    (1, 1, 0, 'RUN', 1000, 1300, 300, 1000.0, 300.0),
                    (2, 1, 1, 'WALK', 1300, 2100, 480, 1000.0, 480.0),
                    (3, 2, 0, 'RUN', 3000, 3600, 600, 0.0, NULL)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(dbName, 6, true, DatabaseMigrations.MIGRATION_5_6).use { db ->
            db.query(
                "SELECT runPaceSecPerKm, walkPaceSecPerKm FROM workouts WHERE id = 1"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(300.0, cursor.getDouble(0), 0.0001)
                assertEquals(480.0, cursor.getDouble(1), 0.0001)
            }
            db.query(
                "SELECT runPaceSecPerKm, walkPaceSecPerKm FROM workouts WHERE id = 2"
            ).use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
                assertTrue(cursor.isNull(1))
            }
        }
    }
}
