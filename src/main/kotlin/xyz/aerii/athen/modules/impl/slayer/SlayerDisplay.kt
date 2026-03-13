package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.helpers.getAttachedLines
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.skyblock.SlayerAPI.slayerNames
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Ticking
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.fcs

@Load
@OnlyIn(skyblock = true)
object SlayerDisplay : Module(
    "Slayer display",
    "Displays the slayer boss's nametags on your screen.",
    Category.SLAYER
) {
    private val ex0 = listOf("§c02:46", "§c☠ §bRevenant Horror I §a500§c❤").fcs

    private var displayComponents: List<Component>? = null
    private var slayerEntity: Entity? = null

    private val display = Ticking(2) {
        val entity = slayerEntity ?: return@Ticking null

        val lines = entity.getAttachedLines()
        val c = ArrayList<Component>(lines.size)
        val cc = ArrayList<Component>(lines.size)

        for (l in lines) {
            val name = l.stripped()
            if ("Spawned by:" in name) continue

            val co = ":" in name
            if (!co && slayerNames.none { it in name }) continue

            if (co) c.add(l) else cc.add(l)
        }

        c += cc
        c
    }

    init {
        config.hud("Display HUD") {
            if (it) return@hud sizedText(ex0, center = listOf(0))
            sizedText(display() ?: return@hud null, center = listOf(0))
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

        on<LocationEvent.Server.Connect> {
            reset()
        }
    }

    private fun reset() {
        slayerEntity = null
        displayComponents = null
    }
}