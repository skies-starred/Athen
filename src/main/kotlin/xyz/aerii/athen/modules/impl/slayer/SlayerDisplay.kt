package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.helpers.getAttachedLines
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.skyblock.SlayerAPI.SLAYER_NAMES
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText

@Load
@OnlyIn(skyblock = true)
object SlayerDisplay : Module(
    "Slayer display",
    "Displays the slayer boss's nametags on your screen.",
    Category.SLAYER
) {
    private var displayComponents: List<Component>? = null
    private var slayerEntity: Entity? = null

    init {
        config.hud("Display HUD") {
            if (it) return@hud sizedText("§c02:46\n§c☠ §bRevenant Horror I §a500§c❤", center = listOf(0))
            val components = displayComponents ?: return@hud null
            sizedText(components, center = listOf(0))
        }

        on<TickEvent.Client> {
            if (Chronos.Ticker.tickClient % 2 != 0) return@on
            val entity = slayerEntity ?: return@on

            val lines = entity.getAttachedLines()
            val c = ArrayList<Component>(lines.size)
            val cc = ArrayList<Component>(lines.size)

            for (i in lines.indices) {
                val line = lines[i]
                val name = line.stripped()
                if (name.contains("Spawned by:")) continue

                var ok = name.indexOf(':') != -1
                if (!ok) {
                    val it = SLAYER_NAMES.iterator()
                    while (it.hasNext()) {
                        if (name.contains(it.next())) {
                            ok = true
                            break
                        }
                    }
                }

                if (!ok) continue
                if (line.string.indexOf(':') != -1) c.add(line) else cc.add(line)
            }

            c.addAll(cc)
            displayComponents = c
        }

        on<SlayerEvent.Boss.Spawn> {
            if (slayerInfo.isOwnedByPlayer) slayerEntity = entity
        }

        on<SlayerEvent.Boss.Death> {
            if (slayerInfo.isOwnedByPlayer) reset()
        }

        on<SlayerEvent.Cleanup> {
            reset()
        }

        on<LocationEvent.ServerConnect> {
            reset()
        }
    }

    private fun reset() {
        slayerEntity = null
        displayComponents = null
    }
}