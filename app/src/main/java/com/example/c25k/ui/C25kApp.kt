package com.example.c25k.ui

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.c25k.AppContainer
import com.example.c25k.C25kApplication
import com.example.c25k.R
import com.example.c25k.domain.AppLanguage
import com.example.c25k.domain.PlanSessionModel
import com.example.c25k.domain.PlanSessionStatus
import com.example.c25k.domain.SegmentType
import com.example.c25k.domain.TrackPointModel
import com.example.c25k.domain.WorkoutDetail
import com.example.c25k.domain.WorkoutSummary
import com.example.c25k.domain.latestCompletedSession
import com.example.c25k.domain.nextSuggestedSession
import com.example.c25k.service.WorkoutPhase
import com.example.c25k.service.WorkoutRuntime
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

@Composable
fun C25kApp(container: AppContainer, application: C25kApplication) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                container = container,
                onOpenHistory = { navController.navigate("history") },
                onOpenSettings = { navController.navigate("settings") },
                onStartWorkout = { sessionId ->
                    WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_START, sessionId)
                    navController.navigate("live")
                }
            )
        }
        composable("live") {
            LiveWorkoutScreen(onDone = { navController.navigate("home") })
        }
        composable("history") {
            HistoryScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onWorkoutClick = { workoutId -> navController.navigate("detail/$workoutId") }
            )
        }
        composable(
            route = "detail/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { entry ->
            val workoutId = entry.arguments?.getLong("workoutId") ?: return@composable
            WorkoutDetailScreen(container = container, workoutId = workoutId, onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onLanguageChanged = { language ->
                    scope.launch {
                        container.languageRepository.setLanguage(language)
                        application.applyLocale(language)
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    container: AppContainer,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (Long) -> Unit
) {
    val sessions by container.planRepository.observeAllSessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val nextSession = sessions.nextSuggestedSession()
    val latestCompleted = sessions.latestCompletedSession()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.next_workout), style = MaterialTheme.typography.titleLarge)
            }
            item {
                HomeSummaryCard(nextSession = nextSession, latestCompleted = latestCompleted)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenHistory) { Text(stringResource(R.string.history)) }
                    Button(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
                }
            }
            item {
                Text(stringResource(R.string.training_plan), style = MaterialTheme.typography.titleLarge)
            }
            items(items = sessions, key = { it.id }) { session ->
                PlanSessionCard(
                    session = session,
                    isNext = nextSession?.id == session.id,
                    onStartWorkout = { onStartWorkout(session.id) },
                    onSkip = {
                        scope.launch {
                            container.planRepository.markSkipped(session.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeSummaryCard(nextSession: PlanSessionModel?, latestCompleted: PlanSessionModel?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.next_workout), style = MaterialTheme.typography.titleMedium)
                if (nextSession == null) {
                    Text(stringResource(R.string.workout_complete))
                } else {
                    Text(stringResource(R.string.week_day_format, nextSession.week, nextSession.day))
                    Text(planStatusLabel(session = nextSession, isNext = true), color = planStatusColor(nextSession.status, true))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.latest_finished_run), style = MaterialTheme.typography.titleMedium)
                if (latestCompleted == null || latestCompleted.latestCompletedAtEpochMs == null) {
                    Text(stringResource(R.string.no_workouts_yet))
                } else {
                    Text(stringResource(R.string.week_day_format, latestCompleted.week, latestCompleted.day))
                    Text(
                        stringResource(
                            R.string.completed_on,
                            formatDate(latestCompleted.latestCompletedAtEpochMs)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanSessionCard(
    session: PlanSessionModel,
    isNext: Boolean,
    onStartWorkout: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.week_day_format, session.week, session.day),
                style = MaterialTheme.typography.titleMedium
            )
            Text(planStatusLabel(session = session, isNext = isNext), color = planStatusColor(session.status, isNext))
            Text(
                stringResource(
                    R.string.plan_session_summary,
                    formatDurationWords(context, session.segments.filter { it.type == SegmentType.RUN }.sumOf { it.durationSec }),
                    formatDurationWords(context, session.segments.sumOf { it.durationSec })
                )
            )
            if (session.status == PlanSessionStatus.COMPLETED && session.latestCompletedAtEpochMs != null) {
                Text(stringResource(R.string.completed_on, formatDate(session.latestCompletedAtEpochMs)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartWorkout) {
                    Text(
                        stringResource(
                            if (session.status == PlanSessionStatus.COMPLETED) R.string.start_again
                            else R.string.start_workout
                        )
                    )
                }
                if (session.status == PlanSessionStatus.PENDING) {
                    Button(onClick = onSkip) { Text(stringResource(R.string.skip_run)) }
                }
            }
        }
    }
}

@Composable
private fun LiveWorkoutScreen(onDone: () -> Unit) {
    val state by WorkoutRuntime.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.live_workout), style = MaterialTheme.typography.titleLarge)
            }
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            when (state.currentSegmentType) {
                                SegmentType.RUN -> stringResource(R.string.running)
                                SegmentType.WALK -> stringResource(R.string.walking)
                                null -> "-"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                        state.week?.let { week ->
                            state.day?.let { day ->
                                Text(
                                    stringResource(R.string.week_day_format, week, day),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("${stringResource(R.string.remaining)}: ${formatClock(state.segmentRemainingSec.toLong())}")
                        Text("${stringResource(R.string.elapsed)}: ${formatClock(state.elapsedSec)}")
                        Text("${stringResource(R.string.distance)}: ${"%.2f".format(state.totalDistanceMeters / 1000.0)} km")
                        Text("${stringResource(R.string.pace)}: ${formatPace(state.currentPaceSecPerKm)}")
                        Text("${stringResource(R.string.run_pace_label)} ${formatPace(state.runPaceSecPerKm)}")
                        Text("${stringResource(R.string.walk_pace_label)} ${formatPace(state.walkPaceSecPerKm)}")
                    }
                }
            }
            if (state.segments.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.phase_timeline), style = MaterialTheme.typography.titleMedium)
                }
                items(state.segments, key = { it.segmentOrder }) { segment ->
                    val timelineStatus = when {
                        segment.segmentOrder < state.currentSegmentOrder -> PhaseTimelineStatus.COMPLETED
                        segment.segmentOrder == state.currentSegmentOrder -> PhaseTimelineStatus.CURRENT
                        else -> PhaseTimelineStatus.UPCOMING
                    }
                    val progress = if (timelineStatus == PhaseTimelineStatus.CURRENT && segment.durationSec > 0) {
                        ((segment.durationSec - state.segmentRemainingSec).toFloat() / segment.durationSec.toFloat())
                            .coerceIn(0f, 1f)
                    } else if (timelineStatus == PhaseTimelineStatus.COMPLETED) {
                        1f
                    } else {
                        0f
                    }
                    PhaseTimelineItem(
                        segmentNumber = segment.segmentOrder + 1,
                        segmentType = segment.type,
                        durationSec = segment.durationSec,
                        status = timelineStatus,
                        progress = progress
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.phase == WorkoutPhase.RUNNING) {
                        Button(onClick = { WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_PAUSE) }) {
                            Text(stringResource(R.string.pause))
                        }
                    }
                    if (state.phase == WorkoutPhase.PAUSED) {
                        Button(onClick = { WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_RESUME) }) {
                            Text(stringResource(R.string.resume))
                        }
                    }
                    Button(onClick = {
                        WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_STOP)
                        onDone()
                    }) {
                        Text(stringResource(R.string.stop))
                    }
                }
            }
            if (state.phase == WorkoutPhase.COMPLETED || state.phase == WorkoutPhase.IDLE) {
                item {
                    Button(onClick = onDone) { Text(stringResource(R.string.history)) }
                }
            }
        }
    }
}

private enum class PhaseTimelineStatus {
    COMPLETED,
    CURRENT,
    UPCOMING
}

@Composable
private fun PhaseTimelineItem(
    segmentNumber: Int,
    segmentType: SegmentType,
    durationSec: Int,
    status: PhaseTimelineStatus,
    progress: Float
) {
    val context = LocalContext.current
    val accentColor = when (status) {
        PhaseTimelineStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        PhaseTimelineStatus.CURRENT -> MaterialTheme.colorScheme.primary
        PhaseTimelineStatus.UPCOMING -> MaterialTheme.colorScheme.outline
    }
    val statusLabel = when (status) {
        PhaseTimelineStatus.COMPLETED -> stringResource(R.string.phase_completed)
        PhaseTimelineStatus.CURRENT -> stringResource(R.string.phase_current)
        PhaseTimelineStatus.UPCOMING -> stringResource(R.string.phase_up_next)
    }
    val phaseLabel = stringResource(R.string.phase_label, segmentNumber)

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(72.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(72.dp)
                        .padding(top = 12.dp, bottom = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(999.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height((48f * progress).dp)
                        .padding(top = 12.dp)
                        .background(
                            color = accentColor,
                            shape = RoundedCornerShape(999.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .padding(top = 26.dp)
                        .size(14.dp)
                        .background(accentColor, CircleShape)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "$phaseLabel • ${segmentTypeLabel(segmentType)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    statusLabel,
                    color = accentColor,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    formatDurationWords(context, durationSec),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (status == PhaseTimelineStatus.CURRENT) {
                    Text(
                        "${stringResource(R.string.phase_progress_label)} ${formatPercent(progress)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onWorkoutClick: (Long) -> Unit
) {
    val items by container.workoutRepository.observeHistory().collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(onClick = onBack) { Text(stringResource(R.string.back)) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(stringResource(R.string.no_workouts_yet))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWorkoutClick(item.id) }
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(formatDate(item.startedAtEpochMs))
                                Text("${"%.2f".format(item.distanceMeters / 1000.0)} km")
                                Text("${stringResource(R.string.avg_pace_label)} ${formatPace(item.avgPaceSecPerKm)}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutDetailScreen(container: AppContainer, workoutId: Long, onBack: () -> Unit) {
    val detail by produceState<WorkoutDetail?>(initialValue = null, workoutId) {
        value = container.workoutRepository.getWorkoutDetail(workoutId)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onBack) { Text(stringResource(R.string.back)) }
            if (detail == null) {
                Text(stringResource(R.string.loading))
            } else {
                Text(formatDate(detail!!.workout.startedAtEpochMs), style = MaterialTheme.typography.titleLarge)
                Text("${"%.2f".format(detail!!.workout.distanceMeters / 1000.0)} km")
                Text("${stringResource(R.string.avg_pace_label)} ${formatPace(detail!!.workout.avgPaceSecPerKm)}")
                Spacer(modifier = Modifier.height(8.dp))
                RouteMap(points = detail!!.points)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(detail!!.segments) { seg ->
                        Text("${seg.type.name}: ${formatPace(seg.paceSecPerKm)} | ${"%.2f".format(seg.distanceMeters / 1000.0)} km")
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteMap(points: List<TrackPointModel>) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { mapView },
        update = { view ->
            view.overlays.clear()
            val runPoints = points.filter { it.segmentType == SegmentType.RUN }
                .map { GeoPoint(it.latitude, it.longitude) }
            val walkPoints = points.filter { it.segmentType == SegmentType.WALK }
                .map { GeoPoint(it.latitude, it.longitude) }

            if (runPoints.size >= 2) {
                val runLine = Polyline().apply {
                    setPoints(runPoints)
                    outlinePaint.color = 0xFFE53935.toInt()
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(runLine)
            }
            if (walkPoints.size >= 2) {
                val walkLine = Polyline().apply {
                    setPoints(walkPoints)
                    outlinePaint.color = 0xFF1E88E5.toInt()
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(walkLine)
            }
            points.firstOrNull()?.let { first ->
                view.controller.setCenter(GeoPoint(first.latitude, first.longitude))
            }
            view.invalidate()
        }
    )

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }
}

@Composable
private fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val language by container.languageRepository.observeLanguage().collectAsStateWithLifecycle(initialValue = AppLanguage.SYSTEM)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.language))
            Button(onClick = { onLanguageChanged(AppLanguage.SYSTEM) }) {
                Text(
                    if (language == AppLanguage.SYSTEM) "${stringResource(R.string.system)} ✓"
                    else stringResource(R.string.system)
                )
            }
            Button(onClick = { onLanguageChanged(AppLanguage.EN) }) {
                Text(
                    if (language == AppLanguage.EN) "${stringResource(R.string.english)} ✓"
                    else stringResource(R.string.english)
                )
            }
            Button(onClick = { onLanguageChanged(AppLanguage.DE) }) {
                Text(
                    if (language == AppLanguage.DE) "${stringResource(R.string.german)} ✓"
                    else stringResource(R.string.german)
                )
            }
        }
    }
}

@Composable
private fun planStatusLabel(session: PlanSessionModel, isNext: Boolean): String {
    return when {
        isNext && session.status == PlanSessionStatus.PENDING -> stringResource(R.string.status_up_next)
        session.status == PlanSessionStatus.COMPLETED -> stringResource(R.string.status_completed)
        session.status == PlanSessionStatus.SKIPPED -> stringResource(R.string.status_skipped)
        else -> stringResource(R.string.status_pending)
    }
}

@Composable
private fun planStatusColor(status: PlanSessionStatus, isNext: Boolean): Color {
    return when {
        isNext && status == PlanSessionStatus.PENDING -> MaterialTheme.colorScheme.primary
        status == PlanSessionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        status == PlanSessionStatus.SKIPPED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatPace(secPerKm: Double?): String {
    if (secPerKm == null || secPerKm <= 0.0) return "--"
    val total = secPerKm.roundToInt()
    val min = total / 60
    val sec = total % 60
    return "%d:%02d /km".format(min, sec)
}

@Composable
private fun segmentTypeLabel(type: SegmentType): String {
    return when (type) {
        SegmentType.RUN -> stringResource(R.string.running)
        SegmentType.WALK -> stringResource(R.string.walking)
    }
}

private fun formatClock(totalSec: Long): String {
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun formatPercent(value: Float): String = "${(value * 100).roundToInt()}%"

private fun formatDate(epochMs: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm")
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
    return fmt.format(calendar)
}

private fun formatDurationWords(context: Context, totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return when {
        minutes > 0 && seconds > 0 -> {
            val minutePart = context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
            val secondPart = context.resources.getQuantityString(R.plurals.duration_seconds, seconds, seconds)
            "$minutePart $secondPart"
        }
        minutes > 0 -> context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
        else -> context.resources.getQuantityString(R.plurals.duration_seconds, seconds, seconds)
    }
}
