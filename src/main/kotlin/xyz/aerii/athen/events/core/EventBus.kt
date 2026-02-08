package xyz.aerii.athen.events.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object EventBus {
    val all = ConcurrentHashMap<Class<out Event>, CopyOnWriteArrayList<Node<out Event>>>()
    val cached = ConcurrentHashMap<Class<out Event>, Array<Node<out Event>>>()

    inline fun <reified T : Event> on(
        priority: Int = 0,
        noinline handler: T.() -> Unit
    ) = Node(T::class.java, handler, priority).handle()

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> post(event: T) {
        val nodes = cached[event.javaClass] as? Array<Node<T>> ?: return
        for (i in nodes) i.handler(event)
    }

    fun Node<*>.handle() = apply {
        all.computeIfAbsent(eventClass) { CopyOnWriteArrayList() }.add(this)
        cache(eventClass)
    }

    fun cache(eventClass: Class<out Event>) {
        val list = all[eventClass] ?: return

        val tmp = ArrayList<Node<*>>(list.size)
        for (n in list) if (n.registered) tmp.add(n)

        tmp.sortBy { it.priority }
        cached[eventClass] = tmp.toTypedArray()
    }
}