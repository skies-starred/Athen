package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import xyz.aerii.athen.accessors.attachedNames
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.skyblock.SlayerAPI.slayerNames
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Ticking
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.fcs
import xyz.aerii.library.utils.stripped

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

        val lines = entity.attachedNames
        var colon: Component? = null
        var name: Component? = null

        for (l in lines) {
            val s = l.stripped()
            if ("Spawned by:" in s) continue

            colon = colon ?: l.takeIf { ":" in s }
            name = name ?: l.takeIf { slayerNames.any { it in s } }

            if (colon != null && name != null) break
        }

        listOfNotNull(colon, name)
    }

    init {
        config.hud("Display HUD") {
            if (it) return@hud sizedText(ex0, center = listOf(0))
            sizedText(display.value ?: return@hud null, center = listOf(0))
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