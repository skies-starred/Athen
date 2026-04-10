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
import xyz.aerii.library.api.name
import java.awt.Color

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object TeammateHighlight : Module(
    "Teammate highlight",
    "Highlights your teammates in kuudra!",
    Category.KUUDRA
) {
    private val lineWidth by config.slider("Line width", 2f, 1f, 10f)
    private val color by config.colorPicker("Color", Color(Catppuccin.Mocha.Green.argb, true))

    init {
        on<WorldRenderEvent.Extract> {
            for (p in KuudraAPI.teammates) {
                if (p.name == name) continue
                val e = p.entity ?: continue

                Render3D.drawBox(e.renderBoundingBox, color, lineWidth)
            }
        }
    }
}