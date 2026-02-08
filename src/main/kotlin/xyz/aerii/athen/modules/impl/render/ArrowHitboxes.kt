package xyz.aerii.athen.modules.impl.render

import net.minecraft.client.renderer.entity.state.ArrowRenderState
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import java.awt.Color

@Load
object ArrowHitboxes : Module(
    "Arrow hitboxes",
    "Shows the hitboxes for arrows",
    Category.RENDER
) {
    private val color by config.colorPicker("Color", Color(Mocha.Green.rgba))
    private val thickness by config.slider("Thickness", 2f, 1f, 10f)

    init {
        on<WorldRenderEvent.Entity.Post> {
            if (renderState !is ArrowRenderState) return@on
            val entity = entity ?: return@on

            Render3D.drawBox(entity.renderBoundingBox, color, thickness)
        }
    }
}