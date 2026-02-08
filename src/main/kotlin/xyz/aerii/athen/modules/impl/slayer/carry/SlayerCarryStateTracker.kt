package xyz.aerii.athen.modules.impl.slayer.carry

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerType
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.modules.common.carry.ICarryStateTracker
import xyz.aerii.athen.modules.common.carry.ICarryStateTracker.HistoryEntry
import xyz.aerii.athen.modules.common.carry.ITrackedCarry
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryTracker.shortName
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryTracker.slayerTypeMap
import java.util.*

private val historyCodec: Codec<HistoryEntry> = RecordCodecBuilder.create { instance ->
    instance.group(
        Codec.STRING.fieldOf("player").forGetter { it.player },
        Codec.INT.fieldOf("amount").forGetter { it.amount },
        Codec.STRING.fieldOf("type").forGetter { it.type },
        Codec.LONG.fieldOf("timestamp").forGetter { it.timestamp }
    ).apply(instance, ::HistoryEntry)
}

@Priority
object SlayerCarryStateTracker : ICarryStateTracker<SlayerCarryStateTracker.TrackedCarry>("features/slayer/carryTracker", historyCodec) {
    override val tracked = mutableMapOf<String, TrackedCarry>()
    val bossToPlayer = WeakHashMap<Entity, String>()

    init {
        for ((p, e) in data.entrySet()) {
            if (e.isJsonObject) d(p, e.asJsonObject)?.let { tracked[p] = it}
        }
    }

    data class TrackedCarry(
        override val player: String,
        override var total: Int,
        val slayerType: SlayerType,
        val tier: Int,
        override var completed: Int = 0,
        override var lastCompletionTime: Long = 0L,
        override var firstCompletionTime: Long = 0L,
        var entity: Entity? = null
    ) : ITrackedCarry {

        data class KillResult(
            val killTime: Double,
            val completed: Boolean,
            val current: Int,
            val total: Int,
            val amount: Int,
            val totalTime: Double
        )

        override fun getType() = "${slayerType.shortName}${if (tier == -1) " Any" else " T$tier"}"
        override fun getShortType() = getType()

        fun onSpawn(boss: Entity): Boolean = (entity == null).also { if (it) entity = boss }

        fun onKill(): KillResult? {
            val ent = entity ?: return null
            val now = System.currentTimeMillis()

            if (firstCompletionTime == 0L) firstCompletionTime = now
            lastCompletionTime = now
            entity = null
            completed++

            return KillResult(
                killTime = ent.tickCount / 20.0,
                completed = completed >= total,
                current = completed,
                total = total,
                amount = completed,
                totalTime = (lastCompletionTime - firstCompletionTime) / 1000.0
            )
        }

        fun reset() {
            lastCompletionTime = System.currentTimeMillis()
            entity = null
        }

        fun clean() {
            if (entity?.run { isRemoved || !isAlive } == true) entity = null
        }
    }

    override fun d(player: String, obj: JsonObject): TrackedCarry? {
        val slayerTypeName = obj.get("slayerType")?.asString ?: return null
        val slayerType = slayerTypeMap.values.find { it.name == slayerTypeName } ?: return null

        return TrackedCarry(
            player = player,
            total = obj.get("total")?.asInt ?: 1,
            slayerType = slayerType,
            tier = obj.get("tier")?.asInt ?: 1,
            completed = obj.get("completed")?.asInt ?: 0,
            lastCompletionTime = obj.get("lastKill")?.asLong ?: 0L,
            firstCompletionTime = obj.get("firstKill")?.asLong ?: 0L
        )
    }

    override fun s(carry: TrackedCarry): JsonObject {
        return JsonObject().apply {
            addProperty("total", carry.total)
            addProperty("slayerType", carry.slayerType.name)
            addProperty("tier", carry.tier)
            addProperty("completed", carry.completed)
            addProperty("lastKill", carry.lastCompletionTime)
            addProperty("firstKill", carry.firstCompletionTime)
        }
    }

    override fun create(player: String, total: Int, vararg params: Any): TrackedCarry? {
        if (params.size < 2) return null
        val slayerType = params[0] as? SlayerType ?: return null
        val tier = params[1] as? Int ?: return null
        return TrackedCarry(player, total, slayerType, tier)
    }

    override fun valid(existing: TrackedCarry, vararg params: Any): Boolean {
        if (params.size < 2) return false
        val slayerType = params[0] as? SlayerType ?: return false
        val tier = params[1] as? Int ?: return false
        return existing.slayerType == slayerType && (existing.tier == -1 || tier == -1 || existing.tier == tier)
    }
}