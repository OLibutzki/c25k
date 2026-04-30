package com.example.c25k

import android.content.Context
import androidx.room.withTransaction
import com.example.c25k.data.AppDatabase
import com.example.c25k.data.PlanSeeder
import com.example.c25k.domain.PlanRepository
import com.example.c25k.domain.TransactionRunner
import com.example.c25k.domain.WorkoutRepository
import com.example.c25k.location.LocationTracker
import com.example.c25k.settings.LanguageRepository
import com.example.c25k.settings.WorkoutDebugRepository
import com.example.c25k.tts.CueFormatter

class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    private val planDao = database.planDao()
    private val transactionRunner = TransactionRunner { block -> database.withTransaction { block() } }

    val languageRepository = LanguageRepository(context)
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
