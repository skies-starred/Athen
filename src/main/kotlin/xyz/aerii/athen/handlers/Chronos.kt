@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import kotlinx.atomicfu.atomic
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Smoothie.mainThread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Chronos, eater of ticks and dev mistakes.
 * Does not give a fuck, the greek personification of time itself.
 *
 * ```kotlin
 * Chronos.Tick run { doSomething() }
 * Chronos.Tick after 20 then { doSomething() }
 * Chronos.Tick every 10 times { doSomething() }
 *
 * Chronos.Time after 1.seconds then { doSomething() }
 * Chronos.Time every 500.milliseconds repeat { doSomething() }
 * ```
 */
@Priority(-5)
object Chronos {
    init {
        Athen.LOGGER.debug("Loaded Chronos: {}, and {}", Tick, Server)
    }

    object Ticker {
        var tickServer: Int = 0
        var tickClient: Int = 0
    }

    object Tick : TickScheduler() {
        init {
            on<TickEvent.Client> {
                Ticker.tickClient++
                tick()
            }
        }
    }

    object Server : TickScheduler() {
        init {
            on<TickEvent.Server> {
                Ticker.tickServer++
                tick()
            }
        }
    }

    object Time : TimeScheduler()

    interface Task {
        val active: Boolean
        fun cancel()
    }

    private fun runSafely(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Athen.LOGGER.error("Task execution failed", e)
        }
    }

    abstract class TickScheduler internal constructor() {
        private data class ScheduledTask(val action: () -> Unit, val recurring: Long = 0, val id: String? = null)
        private val pending = ConcurrentHashMap<Long, MutableList<ScheduledTask>>()
        private val recurring = ConcurrentHashMap<String, TaskImpl>()
        private val oneShots = ConcurrentHashMap<String, TaskImpl>()

        private var nextKey = atomic(0L)
        private var currentTick = atomic(0L)

        internal fun tick() {
            val tick = currentTick.incrementAndGet()
            val tasks = pending.remove(tick)?.toTypedArray() ?: return

            for (task in tasks) {
                runSafely(task.action)

                val recurring = task.recurring
                if (recurring > 0 && task.id != null) scheduleTask(tick + recurring, task)
            }
        }

        private fun scheduleTask(tick: Long, task: ScheduledTask) {
            pending.computeIfAbsent(tick) { mutableListOf() }.add(task)
        }

        infix fun run(action: () -> Unit): Task = after(1) then action

        infix fun after(ticks: Int) = ScheduleBuilder(ticks.toLong())

        infix fun every(ticks: Int) = RepeatBuilder(ticks.toLong())

        inner class ScheduleBuilder(private val delay: Long) {
            infix fun then(action: () -> Unit): Task {
                if (delay <= 0) {
                    mainThread { runSafely(action) }
                    return TaskImpl {}
                }

                val id = "tick_once_${nextKey.incrementAndGet()}"
                val task = TaskImpl { oneShots.remove(id) }
                oneShots[id] = task

                scheduleTask(currentTick.value + delay, ScheduledTask(action, id = id))

                return task
            }
        }

        inner class RepeatBuilder(private val interval: Long) {
            private var startDelay = interval

            infix fun after(ticks: Int) = apply { startDelay = ticks.toLong() }

            infix fun repeat(action: () -> Unit): Task {
                val id = "tick_recurring_${nextKey.incrementAndGet()}"
                val task = TaskImpl { recurring.remove(id) }
                recurring[id] = task

                scheduleTask(
                    currentTick.value + startDelay,
                    ScheduledTask(action, interval, id)
                )

                return task
            }

            infix fun times(action: () -> Unit) = repeat(action)
        }
    }

    abstract class TimeScheduler internal constructor() {
        private val executor = Executors.newScheduledThreadPool(2) { Thread(it, "Chronos-Time").apply { isDaemon = true } }
        private val tasks = ConcurrentHashMap<String, Pair<ScheduledFuture<*>, TaskImpl>>()
        private var nextId = atomic(0L)

        infix fun after(duration: Duration) = TimeScheduleBuilder(duration)

        infix fun every(duration: Duration) = TimeRepeatBuilder(duration)

        private fun scheduleOnce(delay: Duration, action: () -> Unit): Task {
            val id = "time_once_${nextId.incrementAndGet()}"
            val task = TaskImpl { tasks.remove(id)?.first?.cancel(false) }

            val t = executor.schedule(
                {
                    runSafely(action)
                    tasks.remove(id)
                },
                delay.inWholeMilliseconds,
                TimeUnit.MILLISECONDS
            )

            tasks[id] = t to task
            return task
        }

        private fun scheduleRepeating(interval: Duration, initialDelay: Duration, action: () -> Unit): Task {
            val id = "time_recurring_${nextId.incrementAndGet()}"
            val task = TaskImpl { tasks.remove(id)?.first?.cancel(false) }

            val future = executor.scheduleAtFixedRate(
                { runSafely(action) },
                initialDelay.inWholeMilliseconds,
                interval.inWholeMilliseconds,
                TimeUnit.MILLISECONDS
            )

            tasks[id] = future to task
            return task
        }

        inner class TimeScheduleBuilder(private val delay: Duration) {
            infix fun then(action: () -> Unit) = scheduleOnce(delay, action)
        }

        inner class TimeRepeatBuilder(private val interval: Duration) {
            private var startDelay = interval

            infix fun after(duration: Duration) = apply { startDelay = duration }

            infix fun repeat(action: () -> Unit) = scheduleRepeating(interval, startDelay, action)
        }
    }

    private class TaskImpl(private val onCancel: () -> Unit) : Task {
        private val cancelled = atomic(false)

        override val active get() = !cancelled.value

        override fun cancel() = if (cancelled.compareAndSet(expect = false, update = true)) onCancel() else {}
    }
}