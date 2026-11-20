package com.example.notes.data

import com.example.notes.domain.Category
import com.example.notes.domain.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.ZoneId

data class DayCompletion(val epochDay: Long, val due: Int, val done: Int) {
    val rate: Float get() = if (due == 0) 0f else done.toFloat() / due
}

data class CategoryPlan(val category: Category, val plannedMs: Long, val actualMs: Long)

class InsightsRepository(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) {
    /** Per-day tasks due vs done for the 7 days starting at [weekStartDay] (a Monday). */
    fun observeWeeklyCompletion(weekStartDay: Long): Flow<List<DayCompletion>> {
        val z = zone()
        val from = TimeUtils.dayStart(weekStartDay, z)
        val to = TimeUtils.dayStart(weekStartDay + 7, z)
        return taskDao.observeDueBetween(from, to).map { tasks ->
            val byDay = tasks.groupBy { TimeUtils.epochDay(it.deadline!!, z) }
            (0L until 7L).map { offset ->
                val day = weekStartDay + offset
                val dayTasks = byDay[day].orEmpty()
                DayCompletion(day, dayTasks.size, dayTasks.count { it.isDone })
            }
        }
    }

    /** Activity per day (tasks finished + habit check-ins) for a GitHub-style heatmap. */
    fun observeHeatmap(fromDay: Long, toDay: Long): Flow<Map<Long, Int>> {
        val z = zone()
        return combine(
            taskDao.observeCompletedAtBetween(TimeUtils.dayStart(fromDay, z), TimeUtils.dayEnd(toDay, z)),
            habitDao.observeLogCountsBetween(fromDay, toDay)
        ) { completedAt, habitCounts ->
            val result = mutableMapOf<Long, Int>()
            completedAt.forEach { result.merge(TimeUtils.epochDay(it, z), 1, Int::plus) }
            habitCounts.forEach { result.merge(it.epochDay, it.count, Int::plus) }
            result
        }
    }

    /** Time of scheduled tasks that are already done, per category, for blocks overlapping [from, to). */
    fun observeTimeByCategory(from: Long, to: Long): Flow<List<CategoryTotal>> =
        taskDao.observeScheduledBetween(from, to).map { tasks ->
            sumByCategory(tasks.filter { it.isDone }).map { (category, ms) -> CategoryTotal(category, ms) }
        }

    /** Planned (all scheduled tasks) vs done (scheduled tasks that are finished) time per category in [from, to). */
    fun observePlannedVsActual(from: Long, to: Long): Flow<List<CategoryPlan>> =
        taskDao.observeScheduledBetween(from, to).map { tasks ->
            val planned = sumByCategory(tasks)
            val done = sumByCategory(tasks.filter { it.isDone })

            Category.entries.map { category ->
                CategoryPlan(
                    category = category,
                    plannedMs = planned[category.name] ?: 0L,
                    actualMs = done[category.name] ?: 0L
                )
            }
        }

    /** Total scheduled duration per category name. Tasks without a complete time-block count as 0. */
    private fun sumByCategory(tasks: List<TaskEntity>): Map<String, Long> =
        tasks.groupBy { it.category }.mapValues { (_, list) ->
            list.sumOf { (it.scheduledEnd ?: 0L) - (it.scheduledStart ?: 0L) }
        }
}
