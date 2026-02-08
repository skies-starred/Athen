package xyz.aerii.athen.utils.render.animations

import kotlin.math.pow

class SpringValue<T>(
    initial: T,
    private val smoothness: Float,
    private val ops: ValueOps<T>
) {
    private var current: T = initial
    private var target: T = initial
    private var lastTime = System.nanoTime()

    private fun update(): T {
        val now = System.nanoTime()
        val dt = ((now - lastTime) * 1e-9f).coerceIn(0f, 0.1f)
        lastTime = now

        val s = 1f - smoothness.coerceIn(0.01f, 0.99f)
        val f = 1f - s.pow(dt * 60f)

        current = ops.fn(current, target, f)

        if (f >= 0.9999f) current = target
        return current
    }

    var value: T
        get() = update()
        set(v) { target = v }
}

inline fun <reified T> springValue(
    initial: T,
    smoothness: Float = 0.2f
): SpringValue<T> =
    SpringValue(initial, smoothness, selectOps())