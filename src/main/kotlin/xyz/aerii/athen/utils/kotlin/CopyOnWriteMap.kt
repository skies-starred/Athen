package xyz.aerii.athen.utils.kotlin

import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

class CopyOnWriteMap<K, V>(
    initial: Map<K, V> = emptyMap()
) : MutableMap<K, V> {

    private val ref = AtomicReference(Collections.unmodifiableMap(HashMap(initial)))

    private inline val map: Map<K, V>
        get() = ref.get()

    override val size: Int
        get() = map.size

    override val keys: MutableSet<K>
        get() = map.keys as MutableSet<K>

    override val values: MutableCollection<V>
        get() = map.values as MutableCollection<V>

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries as MutableSet<MutableMap.MutableEntry<K, V>>

    override fun isEmpty() = map.isEmpty()

    override fun containsKey(key: K) = map.containsKey(key)

    override fun containsValue(value: V) = map.containsValue(value)

    override fun get(key: K) = map[key]

    override fun put(key: K, value: V): V? {
        while (true) {
            val old = map
            val new = HashMap(old)
            val prev = new.put(key, value)
            if (ref.compareAndSet(old, Collections.unmodifiableMap(new))) return prev
        }
    }

    override fun remove(key: K): V? {
        while (true) {
            val old = map
            if (!old.containsKey(key)) return null
            val new = HashMap(old)
            val prev = new.remove(key)
            if (ref.compareAndSet(old, Collections.unmodifiableMap(new))) return prev
        }
    }

    override fun putAll(from: Map<out K, V>) {
        if (from.isEmpty()) return
        while (true) {
            val old = map
            val new = HashMap(old)
            new.putAll(from)
            if (ref.compareAndSet(old, Collections.unmodifiableMap(new))) return
        }
    }

    override fun clear() {
        ref.set(emptyMap())
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    override fun toString() = map.toString()
}