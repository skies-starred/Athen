package xyz.aerii.athen.modules.impl.kuudra

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import java.awt.Color

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object KuudraHighlight : Module(
    "Kuudra highlight",
    "Highlights kuudra nicely",
    Category.KUUDRA
) {
    private val lineWidth by config.slider("Line width", 2f, 1f, 10f)
    private val color by config.colorPicker("Color", Color(Catppuccin.Mocha.Peach.rgba))

    init {
        on<WorldRenderEvent.Extract> {
            render()
        }
    }

    private fun render() {
        if (!KuudraAPI.inRun) return
        val k = KuudraAPI.kuudra ?: return
        Render3D.drawBox(k.renderBoundingBox, color, lineWidth)
    }
}