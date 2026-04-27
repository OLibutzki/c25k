package com.example.c25k

import android.content.Context
import com.example.c25k.data.AppDatabase
import com.example.c25k.data.PlanSeeder
import com.example.c25k.domain.PlanRepository
import com.example.c25k.domain.WorkoutRepository
import com.example.c25k.location.LocationTracker
import com.example.c25k.settings.LanguageRepository
import com.example.c25k.tts.CueFormatter

class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    private val planDao = database.planDao()
    private val workoutDao = database.workoutDao()

    val languageRepository = LanguageRepository(context)
    val planRepository = PlanRepository(planDao)
    val workoutRepository = WorkoutRepository(workoutDao)
    val planSeeder = PlanSeeder(context, planDao)
    val cueFormatter = CueFormatter(context)
    val locationTracker = LocationTracker(context)
}
