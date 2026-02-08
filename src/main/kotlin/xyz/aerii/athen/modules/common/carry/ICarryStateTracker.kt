package xyz.aerii.athen.modules.common.carry

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import java.text.SimpleDateFormat
import java.util.*

abstract class ICarryStateTracker<T : ITrackedCarry>(storagePath: String, historyCodec: Codec<HistoryEntry>) {
    protected val storage = Scribble(storagePath)
    protected var data by storage.jsonObject("active")

    private var history = storage.mutableList("history", historyCodec)

    abstract val tracked: MutableMap<String, T>

    data class HistoryEntry(
        val player: String,
        val amount: Int,
        val type: String,
        val timestamp: Long
    )

    abstract fun d(player: String, obj: JsonObject): T?
    abstract fun s(carry: T): JsonObject

    abstract fun create(player: String, total: Int, vararg params: Any): T?
    abstract fun valid(existing: T, vararg params: Any): Boolean

    fun addCarry(player: String, total: Int, vararg params: Any) {
        val existing = tracked[player]
        if (existing != null && !valid(existing, *params)) return "§b$player§f is already being tracked for §b${existing.getShortType()}§f. Remove first.".modMessage()
        
        val carry = existing ?: create(player, 0, *params)?.also { tracked[player] = it } ?: return "Failed to create carry tracker.".modMessage()
        carry.total += total

        persist()
        "Now tracking §b$player§f for §b${carry.total}§f §b${carry.getShortType()}§f carries.".modMessage()
    }

    fun removeCarry(player: String) {
        tracked.remove(player) ?: return "§b$player§f is not being tracked.".modMessage()
        persist()
        "Removed §b$player§f from tracking.".modMessage()
    }

    fun listCarries() {
        tracked.values
            .takeIf { it.isNotEmpty() }
            ?.run {
                "Currently tracking:".modMessage()
                forEach { " §7• ${it.str()}".modMessage() }
            } ?: "No carries being tracked.".modMessage()
    }

    fun clearCarries() {
        "Cleared §b${tracked.size}§f tracked carries.".modMessage()
        tracked.clear()
        persist()
    }

    fun displayHistory(page: Int) {
        if (history.value.isEmpty()) return "No carry history found.".modMessage()
        
        val sorted = history.value.sortedByDescending { it.timestamp }
        val totalPages = (sorted.size + 9) / 10
        val currentPage = page.coerceIn(1, totalPages)
        
        val rangeStart = (currentPage - 1) * 10
        val rangeEnd = minOf(rangeStart + 10, sorted.size)
        
        val divider = "§7" + "-".repeatBreak()
        val dateFormat = SimpleDateFormat("MM/dd HH:mm")
        
        divider.modMessage()
        "Carry History - Page §b$currentPage§f/§b$totalPages".modMessage()

        for (e in sorted.subList(rangeStart, rangeEnd))
            " §7• §b${e.player} §8[§7${e.type}§8] §7- §c${dateFormat.format(Date(e.timestamp))} §7- §b${e.amount}§f carries".modMessage()

        "Total: §b${sorted.sumOf { it.amount }}§f carries".modMessage()
        divider.modMessage()
    }

    fun persist() {
        data = JsonObject().apply { for ((p, c) in tracked) add(p, s(c)) }
    }

    fun add(player: String, amount: Int, type: String) =
        history.update { add(HistoryEntry(player, amount, type, System.currentTimeMillis())) }
}