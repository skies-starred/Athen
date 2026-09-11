package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.config.Category
import foo.starred.athen.ducks.entity.EntityDuck.Companion.attachedStripped
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.snowbird.utils.withAlpha
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.world.entity.Entity

@Load
@OnlyIn(islands = [SkyBlockIsland.THE_END])
object EndermanPhaseColor : Module(
    "Enderman phase color",
    "Changes the color of the boss based on it's current phase.",
    Category.SLAYER,
) {
    private val all by config.switch("Change for all bosses")
    private val normal by config.colorPicker("Normal", Catppuccin.Mocha.Text.argb)
    private val hits by config.colorPicker("Hit phase", Catppuccin.Mocha.Lavender.argb.withAlpha(0.5f))

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
