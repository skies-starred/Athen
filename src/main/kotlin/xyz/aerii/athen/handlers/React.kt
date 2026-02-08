@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import kotlinx.atomicfu.atomic
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A modified implementation of Kotlin's StateFlow.
 *
 * @see kotlinx.coroutines.flow.StateFlow
 */
class React<T>(initial: T) {
    private val state = atomic(initial)
    private val immutable = atomic(false)
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()

    var value: T
        get() = state.value
        set(new) {
            if (immutable.value) throw UnsupportedOperationException("Reactive value is set as immutable.")

            val old = state.getAndSet(new)
            if (old != new) {
                val snapshot = listeners.toTypedArray()
                for (i in snapshot) i(new)
            }
        }

    fun onChange(callback: (T) -> Unit) = apply {
        listeners.add(callback)
    }

    fun immutable() = apply {
        immutable.value = true
    }

    fun <R> map(transform: (T) -> R): React<R> {
        val mapped = React(transform(value))
        onChange { mapped.value = transform(it) }
        return mapped
    }

    fun <O, R> combine(other: React<O>, transform: (T, O) -> R): React<R> {
        val combined = React(transform(value, other.value))
        val update = { combined.value = transform(this.value, other.value) }
        onChange { update() }
        other.onChange { update() }
        return combined
    }

    fun filter(predicate: (T) -> Boolean): React<T> {
        val filtered = React(value)
        onChange { if (predicate(it)) filtered.value = it }
        return filtered
    }

    fun scan(initial: T, operation: (acc: T, value: T) -> T): React<T> {
        val scanned = React(initial)
        onChange { scanned.value = operation(scanned.value, it) }
        return scanned
    }

    companion object {
        infix fun React<Boolean>.and(other: React<Boolean>) = combine(other) { a, b -> a && b }
        infix fun React<Boolean>.or(other: React<Boolean>) = combine(other) { a, b -> a || b }
    }
}