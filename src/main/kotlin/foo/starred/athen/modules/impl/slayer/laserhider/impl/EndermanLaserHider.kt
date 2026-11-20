package foo.starred.athen.modules.impl.slayer.laserhider.impl

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryPlayer
import foo.starred.athen.modules.impl.slayer.carry.resolver.SlayerCarryEntityDataResolver
import foo.starred.kontext.api.entity.extension.get
import net.minecraft.world.entity.monster.Guardian
import kotlin.math.abs

//~ if >= 26.3 'EnderMan' -> 'Enderman'
import net.minecraft.world.entity.monster.EnderMan

@LoadableAthenModule
@AthenModuleScope(islands = [PresetSkyBlockIsland.THE_END])
object EndermanLaserHider : Module(
    "Enderman laser hider",
    "Hides the lasers for voidgloom bosses!",
    SLAYER
) {
    private val carry by config.switch("Show for carries", true)
    //~ if >= 26.3 'EnderMan' -> 'Enderman'
    private val set: MutableSet<EnderMan> = mutableSetOf()

    init {
        on<SlayerEvent.Boss.Spawn>(Int.MAX_VALUE) {
            //~ if >= 26.3 'EnderMan' -> 'Enderman'
            if (entity !is EnderMan) return@on
            if (slayerInfo.owned) return@on
            if (!carry && entity.get<SlayerCarryEntityDataResolver, SlayerCarryPlayer>() != null) return@on

            set.add(entity)
        }

        on<SlayerEvent.Boss.Death> {
            set.remove(entity)
        }

        on<TickEvent.Client.End> {
            if (ticks % 20 != 0) return@on
            set.removeIf { !it.isAlive }
        }

        on<LocationEvent.Server.Connect> {
            set.clear()
        }
    }

    @JvmStatic
    fun Guardian.fn(): Boolean {
        for (s in set) {
            if (abs(x - s.x) > 0.5) continue
            if (abs(z - s.z) > 0.5) continue
            if (abs(y - s.y) > 5) continue

            return true
        }

        return false
    }
}
