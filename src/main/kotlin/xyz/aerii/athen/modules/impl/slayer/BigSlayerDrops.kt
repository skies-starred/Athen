package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.extentions.getTexture
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.slayers.enums.drop.impl.InfernoDrops
import xyz.aerii.athen.api.slayers.enums.drop.impl.RevenantDrops
import xyz.aerii.athen.api.slayers.enums.drop.impl.SvenDrops
import xyz.aerii.athen.api.slayers.enums.drop.impl.TarantulaDrops
import xyz.aerii.athen.api.slayers.enums.drop.impl.VampireDrops
import xyz.aerii.athen.api.slayers.enums.drop.impl.VoidgloomDrops
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.enchants0
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@Load
@OnlyIn(skyblock = true)
object BigSlayerDrops : Module(
    "Big slayer drops",
    "Renders the items dropped by a slayer boss to be bigger!",
    Category.SLAYER
) {
    private val set = mutableSetOf<Vec3>()

    val scale by config.slider("Scale", 3f, 1f, 10f)
    private val range by config.slider("Range multiplier", 1.0, 0.5, 5.0, "blocks", true)
    private val unscale by config.slider("Unscale after", 15, 5, 60, "seconds")
    private val selected by config.multiCheckbox("Enable for", listOf("Revenant", "Tarantula", "Sven", "Voidgloom", "Riftstalker", "Blaze"), listOf(0, 1, 2, 3, 4, 5))

    private val _filter by config.expandable("Scale filter")
    private val rev by config.multiCheckbox("Revenant", RevenantDrops.entries.map { it.display }, RevenantDrops.entries.map { it.ordinal }).childOf { _filter }
    private val tara by config.multiCheckbox("Tarantula", TarantulaDrops.entries.map { it.display }, TarantulaDrops.entries.map { it.ordinal }).childOf { _filter }
    private val sven by config.multiCheckbox("Sven", SvenDrops.entries.map { it.display }, SvenDrops.entries.map { it.ordinal }).childOf { _filter }
    private val void by config.multiCheckbox("Voidgloom", VoidgloomDrops.entries.map { it.display }, VoidgloomDrops.entries.map { it.ordinal }).childOf { _filter }
    private val blaze by config.multiCheckbox("Blaze", InfernoDrops.entries.map { it.display }, InfernoDrops.entries.map { it.ordinal }).childOf { _filter }
    private val vamp by config.multiCheckbox("Vampire", VampireDrops.entries.map { it.display }, VampireDrops.entries.map { it.ordinal }).childOf { _filter }

    init {
        on<SlayerEvent.Boss.Death> {
            val p = entity.position()
            set.add(p)

            Chronos.schedule(unscale.seconds) {
                set.remove(p)
            }
        }

        on<LocationEvent.Server.Connect> {
            set.clear()
        }
    }

    @JvmStatic
    fun ItemEntity.fn(): Boolean {
        val a = position().x()
        val b = position().y()
        val c = position().z()

        val d = item
        val e = d.getData(DataTypes.ID) ?: return false
        val f = d.item
        val g = if (e == "RUNE") d.getTexture() else null
        val h = if (e == "ENCHANTED_BOOK") d.enchants0().firstOrNull() else null

        val i = fn0(e, f, g, h) || fn1(e, f, g, h) || fn2(e, f, g, h) || fn3(e, f, g, h) || fn4(e, f, g, h) || fn5(e, f, g, h)
        if (!i) return false

        val j = range
        for (s in set) {
            if (abs(s.x - a) > 5 * j) continue
            if (abs(s.y - b) > 3 * j) continue
            if (abs(s.z - c) > 5 * j) continue

            return true
        }

        return false
    }

    private fun fn0(id: String, item: Item, texture: String?, enchants: String?): Boolean {
        if (0 !in selected) return false
        return RevenantDrops.find(item, id, texture, enchants)?.ordinal in rev
    }

    private fun fn1(id: String, item: Item, texture: String?, enchants: String?): Boolean {
        if (1 !in selected) return false
        return TarantulaDrops.find(item, id, texture, enchants)?.ordinal in tara
    }

    private fun fn2(id: String, item: Item, texture: String?, enchants: String?): Boolean {
        if (2 !in selected) return false
        return SvenDrops.find(item, id, texture, enchants)?.ordinal in sven
    }

    private fun fn3(id: String, item: Item, texture: String?, enchants: String?): Boolean {
        if (3 !in selected) return false
        return VoidgloomDrops.find(item, id, texture, enchants)?.ordinal in void
    }

    private fun fn4(id: String, item: Item, texture: String?, enchants: String?): Boolean {
        if (4 !in selected) return false
        return InfernoDrops.find(item, id, texture, enchants)?.ordinal in blaze
    }

    private fun fn5(id: String, item: Item, texture: String?, enchants: String?): Boolean {
        if (5 !in selected) return false
        return VampireDrops.find(item, id, texture, enchants)?.ordinal in vamp
    }
}