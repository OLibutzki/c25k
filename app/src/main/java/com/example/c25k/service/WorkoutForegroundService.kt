package com.example.c25k.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.c25k.C25kApplication
import com.example.c25k.MainActivity
import com.example.c25k.R
import com.example.c25k.domain.PlanSessionModel
import com.example.c25k.domain.SegmentStats
import com.example.c25k.domain.SegmentType
import com.example.c25k.domain.TrackPointCapture
import com.example.c25k.domain.isTrackedSegment
import com.example.c25k.domain.trackedSegmentIndex
import com.example.c25k.domain.WorkoutMath
import com.example.c25k.domain.WorkoutDebugMode
import com.example.c25k.domain.WorkoutPersistRequest
import com.example.c25k.domain.withDurationsDividedBy
import com.example.c25k.domain.withWarmupCooldownDuration
import com.example.c25k.tts.TtsCoach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channelId = "workout_guidance_silent"
    private val notificationId = 101

    private lateinit var ttsCoach: TtsCoach

    private var timerJob: Job? = null
    private var locationJob: Job? = null

    private var planSession: PlanSessionModel? = null
    private var startedAtEpochMs: Long = 0L
    private var elapsedSec: Long = 0L
    private var currentSegmentOrder: Int = 0
    private var remainingSec: Int = 0
    private var paused: Boolean = false

    private var totalDistanceMeters = 0.0
    private var runDistanceMeters = 0.0
    private var walkDistanceMeters = 0.0
    private var runDurationSec = 0L
    private var walkDurationSec = 0L
    private var lastLocation: Location? = null

    private val pointCaptures = mutableListOf<TrackPointCapture>()
    private val segmentDistances = mutableMapOf<Int, Double>()

    override fun onCreate() {
        super.onCreate()
        ttsCoach = TtsCoach(this)
        createNotificationChannel()
        startForeground(notificationId, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            WorkoutRuntime.ACTION_START -> {
                val sessionId = intent.getLongExtra(WorkoutRuntime.EXTRA_SESSION_ID, -1L)
                if (sessionId > 0) startWorkout(sessionId)
            }
            WorkoutRuntime.ACTION_PAUSE -> pauseWorkout()
            WorkoutRuntime.ACTION_RESUME -> resumeWorkout()
            WorkoutRuntime.ACTION_STOP -> stopWorkout(markComplete = false)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWorkout(sessionId: Long) {
        if (WorkoutRuntime.state.value.phase == WorkoutPhase.RUNNING || WorkoutRuntime.state.value.phase == WorkoutPhase.PAUSED) {
            return
        }
        serviceScope.launch {
            val app = application as C25kApplication
            val session = app.container.planRepository.getSession(sessionId)
            if (session == null) {
                WorkoutRuntime.updateState(WorkoutState(errorMessage = "Session not found"))
                return@launch
            }

            val language = app.container.languageRepository.getLanguage()
            app.applyLocale(language)
            ttsCoach.setLanguage(language)

            val debugMode = if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                app.container.workoutDebugRepository.getMode()
            } else {
                WorkoutDebugMode.OFF
            }
            val warmupCooldownDurationSec = app.container.warmupCooldownRepository.getDurationSec()
            resetRuntime(
                session.withWarmupCooldownDuration(warmupCooldownDurationSec)
                    .withDurationsDividedBy(debugMode.durationDivisor)
            )
            speakTransitionCue()
            startTimerLoop()
            startLocationLoop()
        }
    }

    private fun resetRuntime(session: PlanSessionModel) {
        planSession = session
        startedAtEpochMs = System.currentTimeMillis()
        elapsedSec = 0L
        currentSegmentOrder = 0
        remainingSec = session.segments.firstOrNull()?.durationSec ?: 0
        paused = false
        totalDistanceMeters = 0.0
        runDistanceMeters = 0.0
        walkDistanceMeters = 0.0
        runDurationSec = 0L
        walkDurationSec = 0L
        lastLocation = null
        pointCaptures.clear()
        segmentDistances.clear()
        publishState(WorkoutPhase.RUNNING)
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                if (paused) {
                    delay(200L)
                    continue
                }

                delay(1000L)
                elapsedSec += 1
                if (currentSegmentTracked()) {
                    if (currentType() == SegmentType.RUN) runDurationSec += 1 else walkDurationSec += 1
                }

                if (remainingSec == 10 && hasNextSegment()) {
                    announcePrepareCue()
                }

                remainingSec -= 1
                if (remainingSec <= 0) {
                    if (hasNextSegment()) {
                        currentSegmentOrder += 1
                        remainingSec = currentDuration()
                        speakTransitionCue()
                    } else {
                        finishWorkout()
                        return@launch
                    }
                }
                publishState(WorkoutPhase.RUNNING)
            }
        }
    }

    private fun startLocationLoop() {
        locationJob?.cancel()
        val app = application as C25kApplication
        locationJob = serviceScope.launch {
            app.container.locationTracker.observeLocation().collectLatest { location ->
                if (paused) return@collectLatest
                handleLocation(location)
            }
        }
    }

    private fun handleLocation(location: Location) {
        if (location.accuracy > 50f) return
        if (!currentSegmentTracked()) {
            lastLocation = null
            publishState(if (paused) WorkoutPhase.PAUSED else WorkoutPhase.RUNNING)
            return
        }

        val prev = lastLocation
        if (prev != null) {
            val delta = prev.distanceTo(location).toDouble()
            if (delta in 0.0..40.0) {
                totalDistanceMeters += delta
                val current = currentType()
                if (current == SegmentType.RUN) runDistanceMeters += delta else walkDistanceMeters += delta
                segmentDistances[currentSegmentOrder] = (segmentDistances[currentSegmentOrder] ?: 0.0) + delta
            }
        }
        lastLocation = location
        val trackedSegmentOrder = planSession?.trackedSegmentIndex(currentSegmentOrder) ?: return

        pointCaptures += TrackPointCapture(
            segmentOrder = trackedSegmentOrder,
            latitude = location.latitude,
            longitude = location.longitude,
            timestampEpochMs = location.time,
            accuracyMeters = location.accuracy,
            speedMps = location.speed,
            segmentType = currentType()
        )

        publishState(if (paused) WorkoutPhase.PAUSED else WorkoutPhase.RUNNING)
    }

    private fun pauseWorkout() {
        if (WorkoutRuntime.state.value.phase != WorkoutPhase.RUNNING) return
        paused = true
        publishState(WorkoutPhase.PAUSED)
    }

    private fun resumeWorkout() {
        if (WorkoutRuntime.state.value.phase != WorkoutPhase.PAUSED) return
        paused = false
        publishState(WorkoutPhase.RUNNING)
    }

    private fun finishWorkout() {
        serviceScope.launch {
            val app = application as C25kApplication
            val session = planSession ?: return@launch
            ttsCoach.speak(app.container.cueFormatter.completeCue())

            val segments = session.segments
                .filter { session.isTrackedSegment(it.segmentOrder) }
                .map { segment ->
                val startSec = session.segments.take(segment.segmentOrder).sumOf { it.durationSec }.toLong()
                val endSec = startSec + segment.durationSec
                val distance = segmentDistances[segment.segmentOrder] ?: 0.0
                SegmentStats(
                    type = segment.type,
                    startEpochMs = startedAtEpochMs + startSec * 1000L,
                    endEpochMs = startedAtEpochMs + endSec * 1000L,
                    durationSec = segment.durationSec.toLong(),
                    distanceMeters = distance,
                    paceSecPerKm = WorkoutMath.paceSecPerKm(distance, segment.durationSec.toLong())
                )
            }
            val trackedElapsedSec = runDurationSec + walkDurationSec

            val request = WorkoutPersistRequest(
                sessionId = session.id,
                startedAtEpochMs = startedAtEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                distanceMeters = totalDistanceMeters,
                avgPaceSecPerKm = WorkoutMath.paceSecPerKm(totalDistanceMeters, trackedElapsedSec),
                segments = segments,
                points = pointCaptures.toList()
            )
            val workoutId = app.container.workoutRepository.persistCompletedWorkout(request)
            stopWorkout(markComplete = true, completedWorkoutId = workoutId)
        }
    }

    private fun stopWorkout(markComplete: Boolean, completedWorkoutId: Long? = null) {
        timerJob?.cancel()
        locationJob?.cancel()
        WorkoutRuntime.updateState(
            if (markComplete) {
                WorkoutState(
                    phase = WorkoutPhase.COMPLETED,
                    completedWorkoutId = completedWorkoutId
                )
            } else {
                WorkoutState(phase = WorkoutPhase.IDLE)
            }
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishState(phase: WorkoutPhase) {
        val runPace = WorkoutMath.paceSecPerKm(runDistanceMeters, runDurationSec)
        val walkPace = WorkoutMath.paceSecPerKm(walkDistanceMeters, walkDurationSec)
        val currentPace = if (currentSegmentTracked()) {
            if (currentType() == SegmentType.RUN) runPace else walkPace
        } else {
            null
        }

        WorkoutRuntime.updateState(
            WorkoutState(
                phase = phase,
                sessionId = planSession?.id,
                week = planSession?.week,
                day = planSession?.day,
                segments = planSession?.segments.orEmpty(),
                currentSegmentType = currentType(),
                currentSegmentOrder = currentSegmentOrder,
                segmentRemainingSec = remainingSec.coerceAtLeast(0),
                elapsedSec = elapsedSec,
                totalDistanceMeters = totalDistanceMeters,
                currentPaceSecPerKm = currentPace,
                runPaceSecPerKm = runPace,
                walkPaceSecPerKm = walkPace
            )
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, buildNotification())
    }

    private suspend fun announcePrepareCue() {
        val nextType = planSession?.segments?.getOrNull(currentSegmentOrder + 1)?.type ?: return
        val app = application as C25kApplication
        val language = app.container.languageRepository.getLanguage()
        app.applyLocale(language)
        ttsCoach.setLanguage(language)
        ttsCoach.speak(app.container.cueFormatter.prepareCue(nextType))
    }

    private suspend fun speakTransitionCue() {
        val app = application as C25kApplication
        val language = app.container.languageRepository.getLanguage()
        app.applyLocale(language)
        ttsCoach.setLanguage(language)
        ttsCoach.playPhaseStartBeep()
        delay(150L)
        ttsCoach.speak(app.container.cueFormatter.transitionCue(currentType(), remainingSec))
    }

    private fun buildNotification(): Notification {
        val pauseResumeAction = if (paused) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.resume),
                servicePendingIntent(WorkoutRuntime.ACTION_RESUME)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.pause),
                servicePendingIntent(WorkoutRuntime.ACTION_PAUSE)
            )
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.stop),
            servicePendingIntent(WorkoutRuntime.ACTION_STOP)
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(activityPendingIntent())
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, WorkoutForegroundService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            enableVibration(false)
            setSound(null, null)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun currentType(): SegmentType {
        return planSession?.segments?.getOrNull(currentSegmentOrder)?.type ?: SegmentType.WALK
    }

    private fun currentDuration(): Int {
        return planSession?.segments?.getOrNull(currentSegmentOrder)?.durationSec ?: 0
    }

    private fun currentSegmentTracked(): Boolean {
        return planSession?.isTrackedSegment(currentSegmentOrder) == true
    }

    private fun hasNextSegment(): Boolean {
        val session = planSession ?: return false
        return currentSegmentOrder < session.segments.lastIndex
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        locationJob?.cancel()
        ttsCoach.shutdown()
        serviceScope.cancel()
    }
}
