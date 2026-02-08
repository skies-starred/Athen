package xyz.aerii.athen.modules.common.carry

import xyz.aerii.athen.utils.toDuration

interface ITrackedCarry {
    val player: String
    var total: Int
    var completed: Int
    var lastCompletionTime: Long
    var firstCompletionTime: Long

    fun getType(): String
    fun getShortType(): String

    fun str(): String {
        val now = System.currentTimeMillis()

        val timeSince = lastCompletionTime
            .takeIf { it != 0L }
            ?.let { ((now - it) / 1000.0).toDuration() }
            ?: "§7N/A"

        val rate = if (completed > 2 && firstCompletionTime != 0L) {
            val seconds = (now - firstCompletionTime) / 1000
            if (seconds > 0) "${completed * 3600 / seconds}/hr" else "§7N/A"
        } else "§7N/A"

        return "§7> §b$player §8[§7${getShortType()}§8]§f: §b$completed§f/§b$total §7($timeSince | $rate)"
    }
}