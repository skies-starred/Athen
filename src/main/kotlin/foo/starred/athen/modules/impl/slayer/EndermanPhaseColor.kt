package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.skyblock.entity.duck.EntityDuck.Companion.attachedStripped
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.kontext.api.format.extension.color.alpha
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.world.entity.Entity

@LoadableAthenModule
@AthenModuleScope(islands = [PresetSkyBlockIsland.THE_END])
object EndermanPhaseColor : Module(
    "Enderman phase color",
    "Changes the color of the boss based on it's current phase.",
    ConfigCategory.SLAYER,
) {
    private val all by config.switch("Change for all bosses")
    private val normal by config.colorPicker("Normal", MochaColorScheme.Text.argb)
    private val hits by config.colorPicker("Hit phase", MochaColorScheme.Lavender.argb.alpha(0.5f))

    private val map: Object2IntOpenHashMap<Entity> = Object2IntOpenHashMap<Entity>().apply {
        defaultReturnValue(-1)
    }

    init {
        on<SlayerEvent.Boss.Spawn> {
            if (slayerInfo.type != SlayerBoss.Voidgloom) return@on
            if (!slayerInfo.owned && !all) return@on

            map[entity] = normal
        }

        on<SlayerEvent.Boss.Death> {
            if (slayerInfo.type != SlayerBoss.Voidgloom) return@on
            if (!slayerInfo.owned && !all) return@on

            map -= entity
        }

        on<TickEvent.Client.End> {
            if (ticks % 5 != 0) return@on

            val it = map.iterator()
            while (it.hasNext()) {
                val (k, _) = it.next()

                if (!k.isAlive) {
                    it.remove()
                    continue
                }

                map[k] = if (k.attachedStripped.any { " Hits" in it }) hits else normal
            }
        }
    }

    @JvmStatic
    fun Entity.get(): Int? {
        return map.getInt(this).takeIf { it != -1 }
    }
}
