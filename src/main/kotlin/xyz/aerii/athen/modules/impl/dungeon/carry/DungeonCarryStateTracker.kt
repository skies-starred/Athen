package xyz.aerii.athen.modules.impl.dungeon.carry

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.modules.common.carry.ICarryStateTracker
import xyz.aerii.athen.modules.common.carry.ICarryStateTracker.HistoryEntry
import xyz.aerii.athen.modules.common.carry.ITrackedCarry

private val historyCodec: Codec<HistoryEntry> = RecordCodecBuilder.create { instance ->
    instance.group(
        Codec.STRING.fieldOf("player").forGetter { it.player },
        Codec.INT.fieldOf("amount").forGetter { it.amount },
        Codec.STRING.fieldOf("type").forGetter { it.type },
        Codec.LONG.fieldOf("timestamp").forGetter { it.timestamp }
    ).apply(instance, ::HistoryEntry)
}

@Priority
object DungeonCarryStateTracker : ICarryStateTracker<DungeonCarryStateTracker.TrackedCarry>("features/dungeon/carryTracker", historyCodec) {
    override val tracked = mutableMapOf<String, TrackedCarry>()

    init {
        data.entrySet().forEach { (player, element) ->
            if (!element.isJsonObject) return@forEach
            d(player, element.asJsonObject)?.let { tracked[player] = it }
        }
    }

    data class TrackedCarry(
        override val player: String,
        override var total: Int,
        val floor: DungeonFloor,
        override var completed: Int = 0,
        override var lastCompletionTime: Long = 0L,
        override var firstCompletionTime: Long = 0L
    ) : ITrackedCarry {

        data class CompletionResult(
            val completed: Boolean,
            val current: Int,
            val total: Int,
            val amount: Int,
            val totalTime: Double
        )

        override fun getType() = floor.name
        override fun getShortType() = floor.name

        fun onCompletion(): CompletionResult {
            val now = System.currentTimeMillis()

            if (firstCompletionTime == 0L) firstCompletionTime = now
            lastCompletionTime = now
            completed++

            return CompletionResult(
                completed = completed >= total,
                current = completed,
                total = total,
                amount = completed,
                totalTime = (lastCompletionTime - firstCompletionTime) / 1000.0
            )
        }
    }

    override fun d(player: String, obj: JsonObject): TrackedCarry? {
        val floorName = obj.get("floor")?.asString ?: return null
        val floor = DungeonFloor.getByName(floorName) ?: return null

        return TrackedCarry(
            player = player,
            total = obj.get("total")?.asInt ?: 1,
            floor = floor,
            completed = obj.get("completed")?.asInt ?: 0,
            lastCompletionTime = obj.get("lastCompletion")?.asLong ?: 0L,
            firstCompletionTime = obj.get("firstCompletion")?.asLong ?: 0L
        )
    }

    override fun s(carry: TrackedCarry): JsonObject {
        return JsonObject().apply {
            addProperty("total", carry.total)
            addProperty("floor", carry.floor.name)
            addProperty("completed", carry.completed)
            addProperty("lastCompletion", carry.lastCompletionTime)
            addProperty("firstCompletion", carry.firstCompletionTime)
        }
    }

    override fun create(player: String, total: Int, vararg params: Any): TrackedCarry? {
        if (params.isEmpty()) return null
        val floor = params[0] as? DungeonFloor ?: return null
        return TrackedCarry(player, total, floor)
    }

    override fun valid(existing: TrackedCarry, vararg params: Any): Boolean {
        if (params.isEmpty()) return false
        val floor = params[0] as? DungeonFloor ?: return false
        return existing.floor == floor
    }
}