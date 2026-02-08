package xyz.aerii.athen.utils.render.animations

class TimedValue<T>(
    private val durationMs: Long,
    private val easing: (Float) -> Float,
    private val ops: ValueOps<T>,
    initial: T
) {
    private var from: T = initial
    private var to: T = initial

    private var elapsed = durationMs.toFloat()
    private var lastTime = System.nanoTime()
    var active = false
        private set

    private fun update(): T {
        if (!active) return to

        val now = System.nanoTime()
        val dt = ((now - lastTime) * 1e-6f).coerceIn(0f, 16.67f)
        lastTime = now

        elapsed += dt
        val t = (elapsed / durationMs).coerceIn(0f, 1f)

        if (t >= 1f) {
            active = false
            return to
        }

        return ops.fn(from, to, easing(t))
    }

    var value: T
        get() = update()
        set(v) {
            if (ops.eq(to, v)) return
            from = update()
            to = v
            elapsed = 0f
            lastTime = System.nanoTime()
            active = true
        }

    fun isAnimating() = active
}

inline fun <reified T> timedValue(
    initial: T,
    durationMs: Long,
    noinline easing: (Float) -> Float = ::easeOutQuad
): TimedValue<T> =
    TimedValue(durationMs, easing, selectOps(), initial)