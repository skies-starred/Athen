package xyz.aerii.athen.events.core

import xyz.aerii.athen.handlers.React

class Node<T : Event>(
    @JvmField val eventClass: Class<T>,
    @JvmField var handler: (T) -> Unit,
    @JvmField val priority: Int
) {
    @Volatile var registered = true
    internal var overridden = false
    internal val conditions = mutableListOf<React<Boolean>>()

    fun once() = apply {
        val original = handler
        handler = {
            original(it)
            EventBus.all[eventClass]?.remove(this)
            EventBus.cache(eventClass)
        }
    }

    fun register() = toggle(true)

    fun unregister() = toggle(false)

    fun toggle(value: Boolean): Boolean {
        if (registered == value) return false
        registered = value
        EventBus.cache(eventClass)
        return true
    }

    internal fun evaluate() = if (conditions.all { it.value }) register() else unregister()
}