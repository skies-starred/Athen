package xyz.aerii.athen.api.kuudra.enums

import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.core.Event
import xyz.aerii.athen.handlers.Chronos

enum class KuudraPhase(val event: Event, val tiers: IntRange = KuudraTier.BASIC.int..KuudraTier.INFERNAL.int) {
    Supply(KuudraEvent.Phase.Supply),
    Build(KuudraEvent.Phase.Build),
    Fuel(KuudraEvent.Phase.Fuel),
    Stun(KuudraEvent.Phase.Stun, KuudraTier.BURNING.int..KuudraTier.INFERNAL.int),
    DPS(KuudraEvent.Phase.DPS, KuudraTier.BURNING.int..KuudraTier.INFERNAL.int),
    Skip(KuudraEvent.Phase.Skip, KuudraTier.INFERNAL.int..KuudraTier.INFERNAL.int),
    Kill(KuudraEvent.Phase.Kill);

    var startTick: Int = 0
    var startTime: Long = 0

    var endTick: Int = 0
    var endTime: Long = 0

    val durTime: Long
        get() {
            if (startTime == 0L) return 0
            if (endTime == 0L) return System.currentTimeMillis() - startTime
            return (endTime - startTime).coerceAtLeast(0)
        }

    val durTicks: Int
        get() {
            if (startTick == 0) return 0
            if (endTick == 0) return Chronos.ticks.server - startTick
            return (endTick - startTick).coerceAtLeast(0)
        }

    fun str(type: String? = null): String {
        if (type?.lowercase() == "eaten") return "Eaten"
        if (this == Fuel && (KuudraAPI.tier?.int ?: 0) >= KuudraTier.BURNING.int) return "Eaten"
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }

    val active: Boolean
        get() = started && !ended

    val started: Boolean
        get() = startTime != 0L && startTick != 0

    val ended: Boolean
        get() = endTime != 0L && endTick != 0

    fun reset() {
        startTime = 0
        startTick = 0
        endTime = 0
        endTick = 0
    }

    fun start() = apply {
        if (started) return@apply

        startTime = System.currentTimeMillis()
        startTick = Chronos.ticks.server
        entries.filter { it.ordinal < ordinal }.forEach { it.end() }
    }

    fun end() = apply {
        if (ended) return@apply

        endTime = System.currentTimeMillis()
        endTick = Chronos.ticks.server
    }

    companion object {
        fun reset() = entries.forEach { it.reset() }
    }
}