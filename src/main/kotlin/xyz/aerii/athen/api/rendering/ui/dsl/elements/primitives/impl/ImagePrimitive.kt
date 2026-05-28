package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

open class ImagePrimitive : IPrimitiveElement<ImagePrimitive>() {
    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0
    override var color: Int = -1

    var sprite: Boolean = false

    var location: ResourceLocation? = null
    var pipeline: RenderPipeline = RenderPipelines.GUI_TEXTURED

    var u0: Float = 0f
    var v0: Float = 0f

    var u1: Int? = null
    var v1: Int? = null

    var textureWidth: Int = 256
    var textureHeight: Int = 256

    override fun render(graphics: GuiGraphics) {
        if (!visible) return
        val location = location ?: return

        if (sprite) {
            graphics.blitSprite(pipeline, location, x, y, width, height, color)
            super.render(graphics)

            return
        }

        graphics.blit(pipeline, location, x, y, u0, v0, width, height, u1 ?: width, v1 ?: height, textureWidth, textureHeight, color)
        super.render(graphics)
    }
}