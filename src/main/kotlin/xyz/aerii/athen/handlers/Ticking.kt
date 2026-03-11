package xyz.aerii.athen.handlers

class Ticking<T>(
    private val ticks: Int = 1,
    private val block: () -> T
) {
    var init = false
        private set

    var last = -1
        private set

    var value: T? = null
        private set

    operator fun invoke(): T? {
        val now = Chronos.Ticker.tickServer
        val v0 = value

        if (init && now - last < ticks) return v0

        value = block()
        last = now
        init = true
        return value
    }

    fun reset() {
        if (!init) return

        init = false
        value = null
        last = -1
    }
}