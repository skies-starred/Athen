package xyz.aerii.athen.modules.impl.kuudra

import net.minecraft.world.entity.LivingEntity
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.override
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.abbreviate
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import java.awt.Color

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object KuudraInfo : Module(
    "Kuudra info",
    "Displays information about kuudra and highlights him, nicely :3",
    Category.KUUDRA
) {
    private val highlight = config.switch("Highlight", true).custom("highlight")
    private val lineWidth by config.slider("Line width", 2f, 1f, 10f).dependsOn { highlight.value }
    private val color by config.colorPicker("Color", Color(Catppuccin.Mocha.Peach.argb, true)).dependsOn { highlight.value }

    private val hud = config.hud("Kuudra HP") {
        if (it) return@hud sizedText("§a46.5m§7/§4240m §c❤")
        if (!KuudraAPI.inRun) return@hud null

        sizedText(display ?: return@hud null)
    }

    private var display: String? = null

    init {
        on<LocationEvent.ServerConnect> {
            display = null
        }.override()

        on<EntityEvent.Update.Health> {
            if (!hud.enabled) return@on
            if (!KuudraAPI.inRun) return@on

            val e = KuudraAPI.kuudra?.takeIf { it == entity } ?: return@on
            display = e.str()
        }

        on<WorldRenderEvent.Extract> {
            render()
        }.runWhen(highlight.state)
    }

    private fun LivingEntity.str(): String {
        val s = health
        val bool = KuudraAPI.tier == KuudraTier.INFERNAL && s <= 25_000
        val m = if (bool) 240_000_000f else 100_000f
        val h = if (bool) s * 9_600 else s // 240_000_000 / 25_000 = 9_600
        val m0 = if (bool) "240M" else "100K"

        val color = when (h / m) {
            in 0.7..1.0 -> "§4"
            in 0.5..0.7 -> "§c"
            in 0.3..0.5 -> "§a"
            else -> "§2"
        }

        return "$color${h.abbreviate()}§7/§4$m0 §c❤"
    }

    private fun render() {
        if (!KuudraAPI.inRun) return
        val k = KuudraAPI.kuudra ?: return
        Render3D.drawBox(k.renderBoundingBox, color, lineWidth)
    }
}