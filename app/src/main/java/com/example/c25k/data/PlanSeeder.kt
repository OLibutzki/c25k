package com.example.c25k.data

import android.content.Context
import com.example.c25k.domain.SegmentType
import org.json.JSONArray
import org.json.JSONObject

class PlanSeeder(
    private val context: Context,
    private val planDao: PlanDao
) {
    suspend fun seedIfNeeded() {
        if (planDao.countSessions() > 0) return

        val raw = context.assets.open("nhs_c25k_plan.json").bufferedReader().use { it.readText() }
        val root = JSONArray(raw)
        val knownSegments = mutableMapOf<String, List<Pair<SegmentType, Int>>>()

        for (i in 0 until root.length()) {
            val session = root.getJSONObject(i)
            val week = session.getInt("week")
            val day = session.getInt("day")
            val key = "w${week}d$day"
            val segments = parseSegments(session, knownSegments)
            knownSegments[key] = segments

            val sessionId = planDao.insertSession(
                PlanSessionEntity(
                    week = week,
                    day = day,
                    orderInPlan = i + 1
                )
            )
            val segmentEntities = segments.mapIndexed { index, seg ->
                PlanSegmentEntity(
                    sessionId = sessionId,
                    segmentOrder = index,
                    type = seg.first,
                    durationSec = seg.second
                )
            }
            planDao.insertSegments(segmentEntities)
        }
    }

    private fun parseSegments(
        session: JSONObject,
        known: Map<String, List<Pair<SegmentType, Int>>>
    ): List<Pair<SegmentType, Int>> {
        val rawSegments = session.get("segments")
        if (rawSegments is JSONArray) {
            return buildList {
                for (j in 0 until rawSegments.length()) {
                    val seg = rawSegments.getJSONObject(j)
                    add(
                        SegmentType.valueOf(seg.getString("type")) to seg.getInt("durationSec")
                    )
                }
            }
        }

        val ref = rawSegments.toString() // e.g. same_as_w1d1
        val match = Regex("""same_as_w(\d+)d(\d+)""").matchEntire(ref)
            ?: error("Unknown segment reference: $ref")
        val key = "w${match.groupValues[1]}d${match.groupValues[2]}"
        return known[key] ?: error("Missing referenced segments for key=$key")
    }
}
