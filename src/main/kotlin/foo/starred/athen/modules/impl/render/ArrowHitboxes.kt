package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.config.Category
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.render.renderBoundingBox
import net.minecraft.client.renderer.entity.state.ArrowRenderState

@Load
object ArrowHitboxes : Module(
    "Arrow hitboxes",
    "Shows the hitboxes for arrows",
    Category.RENDER
) {
    private val color by config.colorPicker("Color", Catppuccin.Mocha.Green.argb)
    private val thickness by config.slider("Thickness", 2f, 1f, 10f)

    init {
        on<WorldRenderEvent.Entity> {
            if (renderState !is ArrowRenderState) return@on
            val entity = entity ?: return@on

            extractFrameBox(entity.renderBoundingBox, color, thickness)
        }
    }
}
