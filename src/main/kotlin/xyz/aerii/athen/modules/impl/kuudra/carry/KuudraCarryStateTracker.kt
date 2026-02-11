package xyz.aerii.athen.modules.impl.kuudra.carry

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
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
object KuudraCarryStateTracker : ICarryStateTracker<KuudraCarryStateTracker.TrackedCarry>("features/kuudra/carryTracker", historyCodec) {
    override val tracked = mutableMapOf<String, TrackedCarry>()

    init {
        for ((p, e) in data.entrySet()) {
            if (e.isJsonObject) d(p, e.asJsonObject)?.let { tracked[p] = it }
        }
    }

    data class TrackedCarry(
        override val player: String,
        override var total: Int,
        val tier: KuudraTier,
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

        override fun getType() = tier.str
        override fun getShortType() = tier.str

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
        val tierInt = obj.get("tier")?.asInt ?: return null
        val tier = KuudraTier.get(tierInt) ?: return null

        return TrackedCarry(
            player = player,
            total = obj.get("total")?.asInt ?: 1,
            tier = tier,
            completed = obj.get("completed")?.asInt ?: 0,
            lastCompletionTime = obj.get("lastCompletion")?.asLong ?: 0L,
            firstCompletionTime = obj.get("firstCompletion")?.asLong ?: 0L
        )
    }

    override fun s(carry: TrackedCarry): JsonObject {
        return JsonObject().apply {
            addProperty("total", carry.total)
            addProperty("tier", carry.tier.tier)
            addProperty("completed", carry.completed)
            addProperty("lastCompletion", carry.lastCompletionTime)
            addProperty("firstCompletion", carry.firstCompletionTime)
        }
    }

    override fun create(player: String, total: Int, vararg params: Any): TrackedCarry? {
        if (params.isEmpty()) return null
        val tier = params[0] as? KuudraTier ?: return null
        return TrackedCarry(player, total, tier)
    }

    override fun valid(existing: TrackedCarry, vararg params: Any): Boolean {
        if (params.isEmpty()) return false
        val tier = params[0] as? KuudraTier ?: return false
        return existing.tier == tier
    }
}