package com.example.c25k.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.c25k.AppContainer
import com.example.c25k.C25kApplication
import com.example.c25k.R
import com.example.c25k.domain.AppLanguage
import com.example.c25k.domain.DEFAULT_WARMUP_COOLDOWN_DURATION_SEC
import com.example.c25k.domain.MAX_WARMUP_COOLDOWN_DURATION_SEC
import com.example.c25k.domain.PlanSessionModel
import com.example.c25k.domain.PlanSessionStatus
import com.example.c25k.domain.SegmentType
import com.example.c25k.domain.TrackPointModel
import com.example.c25k.domain.WorkoutDetail
import com.example.c25k.domain.WorkoutDebugMode
import com.example.c25k.domain.WorkoutSummary
import com.example.c25k.domain.latestCompletedSession
import com.example.c25k.domain.nextSuggestedSession
import com.example.c25k.domain.withWarmupCooldownDuration
import com.example.c25k.service.WorkoutPhase
import com.example.c25k.service.WorkoutRuntime
import com.example.c25k.ui.theme.C25kTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

@Composable
fun C25kApp(container: AppContainer, application: C25kApplication) {
    C25kTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val workoutState by WorkoutRuntime.state.collectAsStateWithLifecycle()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val hasActiveWorkout = workoutState.phase == WorkoutPhase.RUNNING || workoutState.phase == WorkoutPhase.PAUSED

            LaunchedEffect(hasActiveWorkout, currentRoute) {
                if (hasActiveWorkout && currentRoute != "live") {
                    navController.navigate("live") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                }
            }

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
                    LiveWorkoutScreen(
                        onDone = {
                            navController.navigate("home") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )
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
                    WorkoutDetailScreen(
                        container = container,
                        workoutId = workoutId,
                        onBack = { navController.popBackStack() }
                    )
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
                        },
                        onWarmupCooldownDurationChanged = { durationSec ->
                            scope.launch {
                                container.warmupCooldownRepository.setDurationSec(durationSec)
                            }
                        },
                        onDebugModeChanged = { mode ->
                            scope.launch {
                                container.workoutDebugRepository.setMode(mode)
                            }
                        }
                    )
                }
            }
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
    val sessions by container.planRepository.observeAllSessions()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val warmupCooldownDurationSec by container.warmupCooldownRepository.observeDurationSec()
        .collectAsStateWithLifecycle(initialValue = DEFAULT_WARMUP_COOLDOWN_DURATION_SEC)
    val scope = rememberCoroutineScope()
    val displaySessions = remember(sessions, warmupCooldownDurationSec) {
        sessions.map { it.withWarmupCooldownDuration(warmupCooldownDurationSec) }
    }
    val nextSession = displaySessions.nextSuggestedSession()
    val latestCompleted = displaySessions.latestCompletedSession()
    val completedCount = displaySessions.count { it.status == PlanSessionStatus.COMPLETED }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HomeHeroCard(
                    nextSession = nextSession,
                    latestCompleted = latestCompleted,
                    completedCount = completedCount,
                    totalCount = displaySessions.size,
                    onOpenHistory = onOpenHistory,
                    onOpenSettings = onOpenSettings,
                    onStartWorkout = nextSession?.let { { onStartWorkout(it.id) } }
                )
            }
            item {
                PlanOverviewCard(
                    sessions = displaySessions,
                    latestCompleted = latestCompleted
                )
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.training_plan),
                    subtitle = stringResource(R.string.training_plan_subtitle)
                )
            }
            items(items = displaySessions, key = { it.id }) { session ->
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
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun HomeHeroCard(
    nextSession: PlanSessionModel?,
    latestCompleted: PlanSessionModel?,
    completedCount: Int,
    totalCount: Int,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (() -> Unit)?
) {
    val headline = if (nextSession == null) {
        stringResource(R.string.workout_complete)
    } else {
        stringResource(R.string.week_day_format, nextSession.week, nextSession.day)
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        label = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.sessions_completed_count,
                            completedCount,
                            totalCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroMetric(
                        label = stringResource(R.string.next_workout),
                        value = if (nextSession == null) {
                            stringResource(R.string.status_completed)
                        } else {
                            planStatusLabel(session = nextSession, isNext = true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetric(
                        label = stringResource(R.string.latest_finished_run),
                        value = latestCompleted?.latestCompletedAtEpochMs?.let(::formatDate)
                            ?: stringResource(R.string.no_workouts_yet),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (onStartWorkout != null) {
                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_workout))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.history))
                    }
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.13f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlanOverviewCard(
    sessions: List<PlanSessionModel>,
    latestCompleted: PlanSessionModel?
) {
    val totalRunMinutes = sessions.sumOf { session ->
        session.segments.filter { it.type == SegmentType.RUN }.sumOf { it.durationSec }
    } / 60
    val completedCount = sessions.count { it.status == PlanSessionStatus.COMPLETED }
    val progress = if (sessions.isEmpty()) 0f else completedCount.toFloat() / sessions.size.toFloat()

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = stringResource(R.string.plan_overview),
                subtitle = stringResource(R.string.plan_overview_subtitle)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetric(
                    label = stringResource(R.string.training_plan),
                    value = stringResource(R.string.sessions_completed_count, completedCount, sessions.size),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = stringResource(R.string.running),
                    value = stringResource(R.string.total_run_minutes, totalRunMinutes),
                    modifier = Modifier.weight(1f)
                )
            }
            latestCompleted?.latestCompletedAtEpochMs?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(R.string.completed_on, formatDate(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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
    val statusColor = planStatusColor(session.status, isNext)
    val runDuration = session.segments.filter { it.type == SegmentType.RUN }.sumOf { it.durationSec }
    val totalDuration = session.segments.sumOf { it.durationSec }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.week_day_format, session.week, session.day),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.plan_session_summary,
                            formatDurationWords(context, runDuration),
                            formatDurationWords(context, totalDuration)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    label = planStatusLabel(session = session, isNext = isNext),
                    color = statusColor.copy(alpha = 0.14f),
                    contentColor = statusColor
                )
            }

            SessionSegmentPreview(session = session)

            if (session.status == PlanSessionStatus.COMPLETED && session.latestCompletedAtEpochMs != null) {
                Text(
                    text = stringResource(R.string.completed_on, formatDate(session.latestCompletedAtEpochMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStartWorkout, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            if (session.status == PlanSessionStatus.COMPLETED) R.string.start_again
                            else R.string.start_workout
                        )
                    )
                }
                if (session.status == PlanSessionStatus.PENDING) {
                    OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.skip_run))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSegmentPreview(session: PlanSessionModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        session.segments.take(4).forEach { segment ->
            Surface(
                modifier = Modifier.weight(1f),
                color = if (segment.type == SegmentType.RUN) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (segment.type == SegmentType.RUN) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                    )
                    Text(
                        text = segmentTypeLabel(segment.type),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatShortDuration(segment.durationSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveWorkoutScreen(onDone: () -> Unit) {
    val state by WorkoutRuntime.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingConfirmation by remember { mutableStateOf<WorkoutActionConfirmation?>(null) }
    val hasActiveWorkout = state.phase == WorkoutPhase.RUNNING || state.phase == WorkoutPhase.PAUSED

    BackHandler(enabled = hasActiveWorkout) {
        pendingConfirmation = WorkoutActionConfirmation.STOP
    }

    pendingConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = {
                Text(
                    text = stringResource(
                        if (confirmation == WorkoutActionConfirmation.PAUSE) {
                            R.string.pause_workout_title
                        } else {
                            R.string.stop_workout_title
                        }
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(
                        if (confirmation == WorkoutActionConfirmation.PAUSE) {
                            R.string.pause_workout_message
                        } else {
                            R.string.stop_workout_message
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingConfirmation = null
                        if (confirmation == WorkoutActionConfirmation.PAUSE) {
                            WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_PAUSE)
                        } else {
                            WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_STOP)
                            onDone()
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (confirmation == WorkoutActionConfirmation.PAUSE) {
                                R.string.pause
                            } else {
                                R.string.stop
                            }
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AppScaffold(title = stringResource(R.string.live_workout)) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = when (state.currentSegmentType) {
                                        SegmentType.RUN -> stringResource(R.string.running)
                                        SegmentType.WALK -> stringResource(R.string.walking)
                                        SegmentType.WARMUP -> stringResource(R.string.warmup)
                                        SegmentType.COOLDOWN -> stringResource(R.string.cooldown)
                                        null -> "-"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                state.week?.let { week ->
                                    state.day?.let { day ->
                                        Text(
                                            text = stringResource(R.string.week_day_format, week, day),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            StatusBadge(
                                label = state.phase.name,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = formatClock(state.segmentRemainingSec.toLong()),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.remaining),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricTile(
                                label = stringResource(R.string.elapsed),
                                value = formatClock(state.elapsedSec),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = stringResource(R.string.distance),
                                value = "${"%.2f".format(state.totalDistanceMeters / 1000.0)} km",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricTile(
                                label = stringResource(R.string.run_pace_label),
                                value = formatPace(state.runPaceSecPerKm),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = stringResource(R.string.walk_pace_label),
                                value = formatPace(state.walkPaceSecPerKm),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            if (state.segments.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.phase_timeline),
                        subtitle = stringResource(R.string.workout_metrics_subtitle)
                    )
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.phase == WorkoutPhase.RUNNING) {
                        Button(
                            onClick = { pendingConfirmation = WorkoutActionConfirmation.PAUSE },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.pause))
                        }
                    }
                    if (state.phase == WorkoutPhase.PAUSED) {
                        FilledTonalButton(
                            onClick = { WorkoutRuntime.startService(context, WorkoutRuntime.ACTION_RESUME) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.resume))
                        }
                    }
                    OutlinedButton(
                        onClick = { pendingConfirmation = WorkoutActionConfirmation.STOP },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.stop))
                    }
                }
            }
            if (state.phase == WorkoutPhase.COMPLETED || state.phase == WorkoutPhase.IDLE) {
                item {
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.history))
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

private enum class PhaseTimelineStatus {
    COMPLETED,
    CURRENT,
    UPCOMING
}

private enum class WorkoutActionConfirmation {
    PAUSE,
    STOP
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

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(78.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height((78f * progress).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                        .align(Alignment.BottomStart)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.phase_label, segmentNumber),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = segmentTypeLabel(segmentType),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDurationWords(context, durationSec),
                    style = MaterialTheme.typography.bodyMedium
                )
                StatusBadge(
                    label = statusLabel,
                    color = accentColor.copy(alpha = 0.14f),
                    contentColor = accentColor
                )
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

    AppScaffold(title = stringResource(R.string.history), onBack = onBack) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                title = stringResource(R.string.history),
                message = stringResource(R.string.no_workouts_yet)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    WorkoutHistoryCard(item = item, onClick = { onWorkoutClick(item.id) })
                }
                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(item: WorkoutSummary, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = formatDate(item.startedAtEpochMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (item.week != null && item.day != null) {
                Text(
                    text = stringResource(R.string.week_day_format, item.week, item.day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric(
                    label = stringResource(R.string.distance),
                    value = "${"%.2f".format(item.distanceMeters / 1000.0)} km",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = stringResource(R.string.avg_pace_label),
                    value = formatPace(item.avgPaceSecPerKm),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WorkoutDetailScreen(
    container: AppContainer,
    workoutId: Long,
    onBack: () -> Unit
) {
    val detail by produceState<WorkoutDetail?>(initialValue = null, workoutId) {
        value = container.workoutRepository.getWorkoutDetail(workoutId)
    }

    AppScaffold(title = stringResource(R.string.history), onBack = onBack) { padding ->
        if (detail == null) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                title = stringResource(R.string.loading),
                message = stringResource(R.string.workout_metrics_subtitle)
            )
        } else {
            val current = detail!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = formatDate(current.workout.startedAtEpochMs),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (current.workout.week != null && current.workout.day != null) {
                                Text(
                                    text = stringResource(
                                        R.string.week_day_format,
                                        current.workout.week,
                                        current.workout.day
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SummaryMetric(
                                    label = stringResource(R.string.distance),
                                    value = "${"%.2f".format(current.workout.distanceMeters / 1000.0)} km",
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetric(
                                    label = stringResource(R.string.avg_pace_label),
                                    value = formatPace(current.workout.avgPaceSecPerKm),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = stringResource(R.string.route_map),
                                subtitle = stringResource(R.string.route_map_subtitle)
                            )
                            RouteMap(points = current.points)
                        }
                    }
                }
                item {
                    SectionHeader(
                        title = stringResource(R.string.session_breakdown),
                        subtitle = stringResource(R.string.session_breakdown_subtitle)
                    )
                }
                items(current.segments) { seg ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = segmentTypeLabel(seg.type),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatPace(seg.paceSecPerKm),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                label = "${"%.2f".format(seg.distanceMeters / 1000.0)} km",
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun RouteMap(points: List<TrackPointModel>) {
    val context = LocalContext.current
    val runColor = MaterialTheme.colorScheme.primary.toArgb()
    val walkColor = MaterialTheme.colorScheme.secondary.toArgb()
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
            .height(260.dp)
            .clip(MaterialTheme.shapes.medium),
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
                    outlinePaint.color = runColor
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(runLine)
            }
            if (walkPoints.size >= 2) {
                val walkLine = Polyline().apply {
                    setPoints(walkPoints)
                    outlinePaint.color = walkColor
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
    onLanguageChanged: (AppLanguage) -> Unit,
    onWarmupCooldownDurationChanged: (Int) -> Unit,
    onDebugModeChanged: (WorkoutDebugMode) -> Unit
) {
    val language by container.languageRepository.observeLanguage()
        .collectAsStateWithLifecycle(initialValue = AppLanguage.SYSTEM)
    val warmupCooldownDurationSec by container.warmupCooldownRepository.observeDurationSec()
        .collectAsStateWithLifecycle(initialValue = DEFAULT_WARMUP_COOLDOWN_DURATION_SEC)
    val debugMode by container.workoutDebugRepository.observeMode()
        .collectAsStateWithLifecycle(initialValue = WorkoutDebugMode.OFF)
    val context = LocalContext.current

    AppScaffold(title = stringResource(R.string.settings), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.language),
                            subtitle = stringResource(R.string.language_subtitle)
                        )
                        LanguageOption(
                            title = stringResource(R.string.system),
                            selected = language == AppLanguage.SYSTEM,
                            onClick = { onLanguageChanged(AppLanguage.SYSTEM) }
                        )
                        LanguageOption(
                            title = stringResource(R.string.english),
                            selected = language == AppLanguage.EN,
                            onClick = { onLanguageChanged(AppLanguage.EN) }
                        )
                        LanguageOption(
                            title = stringResource(R.string.german),
                            selected = language == AppLanguage.DE,
                            onClick = { onLanguageChanged(AppLanguage.DE) }
                        )
                    }
                }
            }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.warmup_cooldown),
                            subtitle = stringResource(R.string.warmup_cooldown_subtitle)
                        )
                        MetricTile(
                            label = stringResource(R.string.warmup_cooldown_duration),
                            value = if (warmupCooldownDurationSec == 0) {
                                stringResource(R.string.warmup_cooldown_disabled)
                            } else {
                                formatDurationWords(context, warmupCooldownDurationSec)
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onWarmupCooldownDurationChanged(
                                        (warmupCooldownDurationSec - 60).coerceAtLeast(0)
                                    )
                                },
                                enabled = warmupCooldownDurationSec > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.decrease_one_minute))
                            }
                            FilledTonalButton(
                                onClick = {
                                    onWarmupCooldownDurationChanged(
                                        (warmupCooldownDurationSec + 60)
                                            .coerceAtMost(MAX_WARMUP_COOLDOWN_DURATION_SEC)
                                    )
                                },
                                enabled = warmupCooldownDurationSec < MAX_WARMUP_COOLDOWN_DURATION_SEC,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.increase_one_minute))
                            }
                        }
                    }
                }
            }
            if (context.isDebuggableApp()) {
                item {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SectionHeader(
                                title = stringResource(R.string.workout_debug_mode),
                                subtitle = stringResource(R.string.workout_debug_mode_subtitle)
                            )
                            LanguageOption(
                                title = stringResource(R.string.workout_debug_mode_off),
                                selected = debugMode == WorkoutDebugMode.OFF,
                                onClick = { onDebugModeChanged(WorkoutDebugMode.OFF) }
                            )
                            LanguageOption(
                                title = stringResource(R.string.workout_debug_mode_x10),
                                selected = debugMode == WorkoutDebugMode.X10,
                                onClick = { onDebugModeChanged(WorkoutDebugMode.X10) }
                            )
                            LanguageOption(
                                title = stringResource(R.string.workout_debug_mode_x60),
                                selected = debugMode == WorkoutDebugMode.X60,
                                onClick = { onDebugModeChanged(WorkoutDebugMode.X60) }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            StatusBadge(
                label = if (selected) stringResource(R.string.selected) else stringResource(R.string.available),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    message: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color,
    contentColor: Color
) {
    Surface(
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }
            )
        },
        content = content
    )
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
        SegmentType.WARMUP -> stringResource(R.string.warmup)
        SegmentType.COOLDOWN -> stringResource(R.string.cooldown)
    }
}

private fun formatClock(totalSec: Long): String {
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun formatDate(epochMs: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm")
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
    return fmt.format(calendar)
}

private fun Context.isDebuggableApp(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

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

private fun formatShortDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (minutes > 0) {
        "${minutes}m"
    } else {
        "${seconds}s"
    }
}
