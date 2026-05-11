package de.libutzki.c25k.domain

import de.libutzki.c25k.data.PlanDao
import de.libutzki.c25k.data.PlanSegmentEntity
import de.libutzki.c25k.data.PlanSessionEntity
import de.libutzki.c25k.data.TrackPointEntity
import de.libutzki.c25k.data.WorkoutDao
import de.libutzki.c25k.data.WorkoutEntity
import de.libutzki.c25k.data.WorkoutSegmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkoutRepositoryTest {
    @Test
    fun `persistCompletedWorkout stores out of order sessions in history and marks each completed`() = runTest {
        val planDao = FakePlanDao(
            listOf(
                PlanSessionEntity(id = 1, week = 1, day = 1, orderInPlan = 1),
                PlanSessionEntity(id = 7, week = 3, day = 1, orderInPlan = 7)
            )
        )
        val workoutDao = FakeWorkoutDao()
        val repository = WorkoutRepository(
            transactionRunner = TransactionRunner { block -> block() },
            workoutDao = workoutDao,
            planDao = planDao
        )

        repository.persistCompletedWorkout(request(sessionId = 1, startedAt = 1_000L, completedAt = 2_000L))
        repository.persistCompletedWorkout(request(sessionId = 7, startedAt = 3_000L, completedAt = 4_000L))

        val history = repository.observeHistory().first()
        assertEquals(2, history.size)
        assertEquals(7L, history[0].sessionId)
        assertEquals(3, history[0].week)
        assertEquals(1, history[0].day)
        assertEquals(300.0, history[0].runPaceSecPerKm)
        assertEquals(480.0, history[0].walkPaceSecPerKm)
        assertEquals(1L, history[1].sessionId)
        assertEquals(WorkoutSummary::class, history[0]::class)

        val week1 = planDao.getSession(1)!!
        val week3 = planDao.getSession(7)!!
        assertEquals(PlanSessionStatus.COMPLETED, week1.status)
        assertEquals(PlanSessionStatus.COMPLETED, week3.status)
        assertNotNull(week1.latestCompletedWorkoutId)
        assertNotNull(week3.latestCompletedWorkoutId)
    }

    @Test
    fun `withDurationsDividedBy never reduces a segment below one second`() {
        val session = PlanSessionModel(
            id = 1,
            week = 1,
            day = 1,
            orderInPlan = 1,
            segments = listOf(
                PlanSegmentModel(segmentOrder = 0, type = SegmentType.WALK, durationSec = 5),
                PlanSegmentModel(segmentOrder = 1, type = SegmentType.RUN, durationSec = 60)
            ),
            status = PlanSessionStatus.PENDING,
            latestCompletedWorkoutId = null,
            latestCompletedAtEpochMs = null
        )

        val accelerated = session.withDurationsDividedBy(60)

        assertEquals(1, accelerated.segments[0].durationSec)
        assertEquals(1, accelerated.segments[1].durationSec)
    }

    private fun request(sessionId: Long, startedAt: Long, completedAt: Long) = WorkoutPersistRequest(
        sessionId = sessionId,
        startedAtEpochMs = startedAt,
        completedAtEpochMs = completedAt,
        distanceMeters = 1_000.0,
        avgPaceSecPerKm = 360.0,
        runPaceSecPerKm = 300.0,
        walkPaceSecPerKm = 480.0,
        segments = listOf(
            SegmentStats(
                segmentOrder = 0,
                type = SegmentType.RUN,
                startEpochMs = startedAt,
                endEpochMs = completedAt,
                durationSec = (completedAt - startedAt) / 1000L,
                distanceMeters = 1_000.0,
                paceSecPerKm = 360.0
            )
        ),
        points = emptyList()
    )
}

private class FakePlanDao(
    sessions: List<PlanSessionEntity>
) : PlanDao {
    private val sessionMap = linkedMapOf<Long, PlanSessionEntity>().apply {
        sessions.forEach { put(it.id, it) }
    }
    private val sessionFlow = MutableStateFlow(sessionMap.values.sortedBy { it.orderInPlan })

    override suspend fun countSessions(): Int = sessionMap.size

    override suspend fun insertSession(session: PlanSessionEntity): Long {
        val nextId = (sessionMap.keys.maxOrNull() ?: 0L) + 1L
        sessionMap[nextId] = session.copy(id = nextId)
        publish()
        return nextId
    }

    override suspend fun insertSegments(segments: List<PlanSegmentEntity>) = Unit

    override fun observeSessions(): Flow<List<PlanSessionEntity>> = sessionFlow

    override suspend fun getSession(id: Long): PlanSessionEntity? = sessionMap[id]

    override suspend fun getSegmentsForSession(sessionId: Long): List<PlanSegmentEntity> = emptyList()

    override suspend fun updateCompletion(
        sessionId: Long,
        status: PlanSessionStatus,
        workoutId: Long,
        completedAtEpochMs: Long
    ) {
        val current = sessionMap.getValue(sessionId)
        sessionMap[sessionId] = current.copy(
            status = status,
            latestCompletedWorkoutId = workoutId,
            latestCompletedAtEpochMs = completedAtEpochMs
        )
        publish()
    }

    private fun publish() {
        sessionFlow.value = sessionMap.values.sortedBy { it.orderInPlan }
    }
}

private class FakeWorkoutDao : WorkoutDao {
    private val workouts = mutableListOf<WorkoutEntity>()
    private val workoutFlow = MutableStateFlow<List<WorkoutEntity>>(emptyList())
    private val segmentsByWorkout = mutableMapOf<Long, List<WorkoutSegmentEntity>>()
    private val pointsByWorkout = mutableMapOf<Long, List<TrackPointEntity>>()

    override suspend fun insertWorkout(workout: WorkoutEntity): Long {
        val id = (workouts.maxOfOrNull { it.id } ?: 0L) + 1L
        workouts += workout.copy(id = id)
        publish()
        return id
    }

    override suspend fun insertWorkoutSegments(segments: List<WorkoutSegmentEntity>) {
        if (segments.isEmpty()) return
        segmentsByWorkout[segments.first().workoutId] = segments
    }

    override suspend fun insertTrackPoints(points: List<TrackPointEntity>) {
        if (points.isEmpty()) return
        pointsByWorkout[points.first().workoutId] = points
    }

    override fun observeWorkoutHistory(): Flow<List<WorkoutEntity>> = workoutFlow

    override suspend fun getWorkout(id: Long): WorkoutEntity? = workouts.firstOrNull { it.id == id }

    override suspend fun getWorkoutSegments(workoutId: Long): List<WorkoutSegmentEntity> =
        segmentsByWorkout[workoutId].orEmpty()

    override suspend fun getTrackPoints(workoutId: Long): List<TrackPointEntity> =
        pointsByWorkout[workoutId].orEmpty()

    private fun publish() {
        workoutFlow.value = workouts.sortedByDescending { it.startedAtEpochMs }
    }
}
