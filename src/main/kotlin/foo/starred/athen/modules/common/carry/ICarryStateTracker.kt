package foo.starred.athen.modules.common.carry

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.storage.JsonStore
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.api.storage.impl.generic.extension.jsonObject
import foo.starred.kontext.api.storage.impl.generic.extension.mutableList
import java.text.SimpleDateFormat
import java.util.*

abstract class ICarryStateTracker<T : ITrackedCarry>(storagePath: String, historyCodec: Codec<HistoryEntry>) {
    protected val storage = JsonStore(storagePath)
    protected var data by storage.jsonObject("active")

    private var history = storage.mutableList("history", historyCodec)

    abstract val tracked: MutableMap<String, T>

    data class HistoryEntry(
        val player: String,
        val amount: Int,
        val type: String,
        val timestamp: Long
    )

    abstract fun load(player: String, obj: JsonObject): T?
    abstract fun save(carry: T): JsonObject

    abstract fun create(player: String, total: Int, vararg params: Any): T?
    abstract fun valid(existing: T, vararg params: Any): Boolean

    fun addCarry(player: String, total: Int, vararg params: Any) {
        val existing = tracked[player]
        if (existing != null && !valid(existing, *params)) return "§b$player§f is already being tracked for §b${existing.short}§f. Remove first.".mod()

        val carry = existing ?: create(player, 0, *params)?.also { tracked[player] = it } ?: return "Failed to create carry tracker.".mod()
        carry.total += total

        persist()
        "Now tracking §b$player§f for §b${carry.total}§f §b${carry.short}§f carries.".mod()
    }

    fun removeCarry(player: String) {
        tracked.remove(player) ?: return "§b$player§f is not being tracked.".mod()
        persist()
        "Removed §b$player§f from tracking.".mod()
    }

    fun listCarries() {
        val l = tracked.values
        if (tracked.values.isEmpty()) return "<red>No carries being tracked.".mod()

        "Currently tracking:".mod()
        for (a in l) " ${a.str()}".mod()
    }

    fun clearCarries() {
        "Cleared §b${tracked.size}§f tracked carries.".mod()
        tracked.clear()
        persist()
    }

    fun displayHistory(page: Int) {
        if (history.value.isEmpty()) return "No carry history found.".mod()

        val sorted = history.value.sortedByDescending { it.timestamp }
        val totalPages = (sorted.size + 9) / 10
        val currentPage = page.coerceIn(1, totalPages)

        val rangeStart = (currentPage - 1) * 10
        val rangeEnd = minOf(rangeStart + 10, sorted.size)

        val divider = "§7" + MinecraftChat.repeat("-")
        val dateFormat = SimpleDateFormat("MM/dd HH:mm")

        divider.mod()
        "Carry History - Page §b$currentPage§f/§b$totalPages".mod()

        for (e in sorted.subList(rangeStart, rangeEnd)) " <gray>• <aqua>${e.player} <dark_gray>[<gray>${e.type}<dark_gray>] <gray>- <red>${dateFormat.format(Date(e.timestamp))} <gray>- <aqua>${e.amount}<r> carries".mod()

        "Total: §b${sorted.sumOf { it.amount }}§f carries".mod()
        divider.mod()
    }

    fun persist() {
        data = JsonObject().apply { for ((p, c) in tracked) add(p, save(c)) }
    }

    fun add(player: String, amount: Int, type: String) =
        history.update { add(HistoryEntry(player, amount, type, System.currentTimeMillis())) }
}
