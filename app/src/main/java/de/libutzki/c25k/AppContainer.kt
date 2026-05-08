package de.libutzki.c25k

import android.content.Context
import androidx.room.withTransaction
import de.libutzki.c25k.data.AppDatabase
import de.libutzki.c25k.data.PlanSeeder
import de.libutzki.c25k.domain.PlanRepository
import de.libutzki.c25k.domain.TransactionRunner
import de.libutzki.c25k.domain.WorkoutRepository
import de.libutzki.c25k.location.LocationTracker
import de.libutzki.c25k.settings.LanguageRepository
import de.libutzki.c25k.settings.WarmupCooldownRepository
import de.libutzki.c25k.settings.WorkoutDebugRepository
import de.libutzki.c25k.tts.CueFormatter

class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    private val planDao = database.planDao()
    private val transactionRunner = TransactionRunner { block -> database.withTransaction { block() } }

    val languageRepository = LanguageRepository(context)
    val warmupCooldownRepository = WarmupCooldownRepository(context)
    val workoutDebugRepository = WorkoutDebugRepository(context)
    val planRepository = PlanRepository(planDao)
    val workoutRepository = WorkoutRepository(
        transactionRunner = transactionRunner,
        workoutDao = database.workoutDao(),
        planDao = planDao
    )
    val planSeeder = PlanSeeder(context, planDao)
    val cueFormatter = CueFormatter(context)
    val locationTracker = LocationTracker(context)
}
