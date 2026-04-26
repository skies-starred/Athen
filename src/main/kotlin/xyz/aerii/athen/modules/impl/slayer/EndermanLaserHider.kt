package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Guardian
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryStateTracker
import kotlin.math.abs

@Load
@OnlyIn(islands = [SkyBlockIsland.THE_END])
object EndermanLaserHider : Module(
    "Enderman laser hider",
    "Hides the lasers for voidgloom bosses!",
    Category.SLAYER
) {
    private val carry by config.switch("Show for carries", true)
    private val set0: MutableSet<EnderMan> = mutableSetOf()
    private val set1: MutableSet<Guardian> = mutableSetOf()

    init {
        on<SlayerEvent.Boss.Spawn> {
            if (entity !is EnderMan) return@on
            if (slayerInfo.isOwnedByPlayer) return@on
            if (!carry && slayerInfo.owner in SlayerCarryStateTracker.tracked.keys) return@on

            set0.add(entity)
        }

        on<SlayerEvent.Boss.Death> {
            set0.remove(entity)
        }

        on<EntityEvent.Unload> {
            set1.remove(entity)
        }

        on<TickEvent.Client.End> {
            if (ticks % 20 != 0) return@on

            set0.removeIf { !it.isAlive }
            set1.removeIf { !it.isAlive }
        }

        on<LocationEvent.Server.Connect> {
            set0.clear()
            set1.clear()
        }
    }

    @JvmStatic
    fun Guardian.fn(): Boolean {
        if (this in set1) return true

        for (s in set0) {
            if (abs(x - s.x) > 0.5) continue
            if (abs(z - s.z) > 0.5) continue
            if (abs(y - s.y) > 5) continue

            set1.add(this)
            return true
        }

        return false
    }
}