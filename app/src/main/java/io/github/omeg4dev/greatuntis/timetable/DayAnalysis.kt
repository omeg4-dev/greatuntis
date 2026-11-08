package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.TimeUnit
import java.time.LocalDate
import java.time.LocalTime

data class PlacedLesson(
    val view: LessonView,
    val lane: Int,
    val laneCount: Int,
    /** 1-based grid units covered (inclusive). */
    val firstUnit: Int,
    val lastUnit: Int,
    /** True when the lesson overlaps no grid unit and was clamped to the nearest one. */
    val offGrid: Boolean,
)

enum class FreeReason { CANCELLED, GAP }

data class FreeSlot(
    val start: LocalTime,
    val end: LocalTime,
    val firstUnit: Int,
    val lastUnit: Int,
    val reason: FreeReason,
)

data class GridBreak(val afterUnit: Int, val start: LocalTime, val end: LocalTime) {
    val minutes: Int get() = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
}

enum class DayStatus { FREE, SCHOOL, WHOLE_DAY_FREE }

data class DayPlan(
    val date: LocalDate,
    val status: io.github.omeg4dev.greatuntis.model.DayStatus,
    val placed: List<PlacedLesson>,
    val freeSlots: List<FreeSlot>,
    /** Plan without changes: from regular entries. Null = no regular entries. */
    val regularStart: LocalTime?,
    val regularEnd: LocalTime?,
    /** What really happens: from effective entries. Null = nothing happens. */
    val effectiveStart: LocalTime?,
    val effectiveEnd: LocalTime?,
    val startsLater: Boolean,
    val endsEarlier: Boolean,
    /** Student has no school this day although entries exist. */
    val wholeDayFree: Boolean,
    /** German reason when wholeDayFree: "Alles entfällt" or the free event title. */
    val wholeDayFreeReason: String?,
    /** Number of grid units freed by cancellations (FreeReason.CANCELLED units). */
    val gainedUnits: Int,
    /** True if at least one effective entry exists. */
    val hasSchool: Boolean,
    val predicted: Boolean,
)

object DayAnalysis {
    /**
     * Analyse [day] using [grid] for unit assignment.  [foyerPrefix] is forwarded to [LessonViews.of].
     */
    fun analyze(day: Day, grid: List<TimeUnit>, foyerPrefix: String): DayPlan {
        val sortedGrid = grid.sortedBy { it.index }

        // Build LessonViews for all lessons
        val views = day.lessons.map { lesson -> LessonViews.of(lesson, foyerPrefix) }

        // --- Unit assignment ---
        // For each view find the grid units it overlaps: u.start < lesson.end && u.end > lesson.start
        data class ViewWithUnits(
            val view: LessonView,
            val firstUnit: Int,
            val lastUnit: Int,
            val offGrid: Boolean,
        )

        fun assignUnits(view: LessonView): ViewWithUnits {
            val lesson = view.lesson
            if (sortedGrid.isEmpty()) {
                return ViewWithUnits(view, 1, 1, false)
            }
            val overlapping = sortedGrid.filter { u ->
                u.start < lesson.end && u.end > lesson.start
            }
            return if (overlapping.isNotEmpty()) {
                ViewWithUnits(
                    view,
                    firstUnit = overlapping.first().index,
                    lastUnit = overlapping.last().index,
                    offGrid = false,
                )
            } else {
                // Clamp to nearest unit by start time
                val nearest = sortedGrid.minByOrNull { u ->
                    minOf(
                        Math.abs(u.start.toSecondOfDay() - lesson.start.toSecondOfDay()).toLong(),
                        Math.abs(u.start.toSecondOfDay() - lesson.end.toSecondOfDay()).toLong(),
                    )
                }!!
                ViewWithUnits(view, nearest.index, nearest.index, true)
            }
        }

        val withUnits = views.map { assignUnits(it) }

        // --- Lanes ---
        // Sort non-cancelled first, then by (start, end desc, key)
        val sorted = withUnits.sortedWith(
            compareBy<ViewWithUnits> { it.view.lesson.status == LessonStatus.CANCELLED || it.view.kind == LessonKind.CANCELLED }
                .thenBy { it.view.lesson.start }
                .thenByDescending { it.view.lesson.end }
                .thenBy { it.view.lesson.key }
        )

        // Greedy interval colouring by unit range
        // Two lessons overlap if their [firstUnit..lastUnit] ranges intersect
        fun rangesOverlap(a: ViewWithUnits, b: ViewWithUnits): Boolean =
            a.firstUnit <= b.lastUnit && b.firstUnit <= a.lastUnit

        // Assign lanes greedily: reuse lowest free lane by unit range
        data class LaneEntry(val wu: ViewWithUnits, val lane: Int)

        val laneEntries = mutableListOf<LaneEntry>()
        for (wu in sorted) {
            val overlappingLanes = laneEntries
                .filter { rangesOverlap(it.wu, wu) }
                .map { it.lane }
                .toSet()
            var lane = 0
            while (lane in overlappingLanes) {
                lane++
            }
            laneEntries.add(LaneEntry(wu, lane))
        }

        // Compute connected components (clusters) of overlapping lessons
        val n = sorted.size
        val clusterOf = IntArray(n) { it }
        fun find(x: Int): Int {
            var r = x
            while (clusterOf[r] != r) r = clusterOf[r]
            var c = x
            while (clusterOf[c] != r) { val next = clusterOf[c]; clusterOf[c] = r; c = next }
            return r
        }
        fun union(a: Int, b: Int) { clusterOf[find(a)] = find(b) }
        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                if (rangesOverlap(sorted[i], sorted[j])) union(i, j)
            }
        }
        val maxLaneByCluster = mutableMapOf<Int, Int>()
        for (i in sorted.indices) {
            val root = find(i)
            val lane = laneEntries[i].lane
            maxLaneByCluster[root] = maxOf(maxLaneByCluster[root] ?: 0, lane)
        }

        // Build the final laneCount by using max lanes used in cluster + 1
        val placedList: List<PlacedLesson> = laneEntries.mapIndexed { idx, (wu, lane) ->
            val root = find(idx)
            val clusterLaneCount = (maxLaneByCluster[root] ?: 0) + 1
            PlacedLesson(
                view = wu.view,
                lane = lane,
                laneCount = clusterLaneCount,
                firstUnit = wu.firstUnit,
                lastUnit = wu.lastUnit,
                offGrid = wu.offGrid,
            )
        }.sortedWith(
            compareBy<PlacedLesson> { it.view.lesson.start }
                .thenBy { it.lane }
                .thenBy { it.view.lesson.key }
        )

        // --- Effective and Regular entries ---
        // Effective: status != CANCELLED and not isFreeEvent
        // Regular: type == "NORMAL_TEACHING_PERIOD" and status != ADDITIONAL (cancelled ones count)
        val effectiveViews = views.filter { v ->
            v.lesson.status != LessonStatus.CANCELLED && !v.isFreeEvent
        }
        val regularViews = views.filter { v ->
            v.lesson.type == "NORMAL_TEACHING_PERIOD" && v.lesson.status != LessonStatus.ADDITIONAL
        }

        val regularStart = regularViews.minOfOrNull { it.lesson.start }
        val regularEnd = regularViews.maxOfOrNull { it.lesson.end }
        val effectiveStart = effectiveViews.minOfOrNull { it.lesson.start }
        val effectiveEnd = effectiveViews.maxOfOrNull { it.lesson.end }

        val startsLater = regularStart != null && effectiveStart != null && effectiveStart > regularStart
        val endsEarlier = regularEnd != null && effectiveEnd != null && effectiveEnd < regularEnd

        // --- Free slots ---
        // Work within [firstUnit..lastUnit] touched by any entry
        val freeSlots: List<FreeSlot>
        val gainedUnits: Int

        if (sortedGrid.isEmpty() || withUnits.isEmpty()) {
            freeSlots = emptyList()
            gainedUnits = 0
        } else {
            val firstTouched = withUnits.minOf { it.firstUnit }
            val lastTouched = withUnits.maxOf { it.lastUnit }

            // For each unit in range, determine free reason (or null = not free)
            val unitReasons = mutableMapOf<Int, FreeReason>()
            for (unitIdx in firstTouched..lastTouched) {
                val unit = sortedGrid.firstOrNull { it.index == unitIdx } ?: continue

                val coveredByEffective = effectiveViews.any { v ->
                    val wu = withUnits.first { it.view == v }
                    unitIdx in wu.firstUnit..wu.lastUnit
                }
                val cancelledOrFreeEvent = views.filter { v ->
                    v.lesson.status == LessonStatus.CANCELLED || v.isFreeEvent
                }
                val coveredByCancelledOrFreeEvent = cancelledOrFreeEvent.any { v ->
                    val wu = withUnits.first { it.view == v }
                    unitIdx in wu.firstUnit..wu.lastUnit
                }

                if (!coveredByEffective && coveredByCancelledOrFreeEvent) {
                    unitReasons[unitIdx] = FreeReason.CANCELLED
                } else if (!coveredByEffective && !coveredByCancelledOrFreeEvent) {
                    // GAP: only if strictly between effectiveStart and effectiveEnd
                    if (effectiveStart != null && effectiveEnd != null) {
                        val unitStart = unit.start
                        val unitEnd = unit.end
                        // unit lies strictly between effectiveStart and effectiveEnd
                        if (unitStart >= effectiveStart && unitEnd <= effectiveEnd) {
                            unitReasons[unitIdx] = FreeReason.GAP
                        }
                    }
                }
            }

            // Merge consecutive units with same reason
            val mergedSlots = mutableListOf<FreeSlot>()
            var i = firstTouched
            while (i <= lastTouched) {
                val reason = unitReasons[i]
                if (reason != null) {
                    val unit = sortedGrid.first { it.index == i }
                    var j = i + 1
                    while (j <= lastTouched && unitReasons[j] == reason) j++
                    val lastUnit = sortedGrid.first { it.index == j - 1 }
                    mergedSlots.add(
                        FreeSlot(
                            start = unit.start,
                            end = lastUnit.end,
                            firstUnit = i,
                            lastUnit = j - 1,
                            reason = reason,
                        )
                    )
                    i = j
                } else {
                    i++
                }
            }
            freeSlots = mergedSlots
            gainedUnits = unitReasons.values.count { it == FreeReason.CANCELLED }
        }

        // --- Whole day free ---
        val wholeDayFree = views.isNotEmpty() && effectiveViews.isEmpty()
        val wholeDayFreeReason: String? = if (wholeDayFree) {
            val freeEvent = views.firstOrNull { it.isFreeEvent }
            freeEvent?.title ?: "Alles entfällt"
        } else null

        // --- Predicted ---
        val predicted = views.any { it.lesson.predicted }

        // --- hasSchool ---
        val hasSchool = effectiveViews.isNotEmpty()

        return DayPlan(
            date = day.date,
            status = day.status,
            placed = placedList,
            freeSlots = freeSlots,
            regularStart = regularStart,
            regularEnd = regularEnd,
            effectiveStart = effectiveStart,
            effectiveEnd = effectiveEnd,
            startsLater = startsLater,
            endsEarlier = endsEarlier,
            wholeDayFree = wholeDayFree,
            wholeDayFreeReason = wholeDayFreeReason,
            gainedUnits = gainedUnits,
            hasSchool = hasSchool,
            predicted = predicted,
        )
    }

    /**
     * Returns [GridBreak]s between consecutive grid units where next.start > prev.end.
     */
    fun breaks(grid: List<TimeUnit>): List<GridBreak> {
        val sorted = grid.sortedBy { it.index }
        val result = mutableListOf<GridBreak>()
        for (i in 0 until sorted.size - 1) {
            val prev = sorted[i]
            val next = sorted[i + 1]
            if (next.start > prev.end) {
                result.add(GridBreak(afterUnit = prev.index, start = prev.end, end = next.start))
            }
        }
        return result
    }
}
