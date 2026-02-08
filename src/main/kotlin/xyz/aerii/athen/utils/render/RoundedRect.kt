package xyz.aerii.athen.utils.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import earth.terrarium.olympus.client.pipelines.RoundedRectangle
import earth.terrarium.olympus.client.pipelines.pips.OlympusPictureInPictureRenderState
import earth.terrarium.olympus.client.pipelines.renderer.PipelineRenderer
import earth.terrarium.olympus.client.pipelines.uniforms.RoundedRectangleUniform
import earth.terrarium.olympus.client.utils.GuiGraphicsHelper
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.util.ARGB
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.joml.Vector4f
import xyz.aerii.athen.handlers.Smoothie.client
import java.util.function.Function

/**
 * Rounded Rectangles!
 *
 * A wrapper-like object for Olympus's RoundedRectangle, adds support for different radius for each corner.
 */
object RoundedRect {
    fun draw(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        bgColor: Int,
        borderColor: Int,
        radiusTL: Float,
        radiusTR: Float,
        radiusBL: Float,
        radiusBR: Float,
        borderWidth: Int
    ) {
        GuiGraphicsHelper.submitPip(graphics, MultiRadiusState(
            graphics, x, y, width, height,
            bgColor, borderColor,
            radiusTR, radiusBR, radiusTL, radiusBL,
            borderWidth
        ))
    }

    data class MultiRadiusState(
        val x0: Int, val y0: Int, val x1: Int, val y1: Int,
        val color: Int, val borderColor: Int,
        val radiusTopLeft: Float, val radiusTopRight: Float,
        val radiusBottomLeft: Float, val radiusBottomRight: Float,
        val borderWidth: Int,
        val pose: Matrix3x2f,
        val scissorArea: ScreenRectangle?,
        val bounds: ScreenRectangle
    ) : OlympusPictureInPictureRenderState<MultiRadiusState> {

        override fun x0() = x0
        override fun y0() = y0
        override fun x1() = x1
        override fun y1() = y1
        override fun pose() = pose
        override fun scissorArea() = scissorArea
        override fun bounds() = bounds

        constructor(
            graphics: GuiGraphics,
            x: Int, y: Int, width: Int, height: Int,
            color: Int, borderColor: Int,
            radiusTopLeft: Float, radiusTopRight: Float,
            radiusBottomLeft: Float, radiusBottomRight: Float,
            borderWidth: Int
        ) : this(
            x, y, x + width, y + height,
            color, borderColor,
            radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight,
            borderWidth,
            Matrix3x2f(graphics.pose()),
            GuiGraphicsHelper.getLastScissor(graphics),
            OlympusPictureInPictureRenderState.getRelativeBounds(
                graphics,
                x,
                y,
                x + width + borderWidth * 2,
                y + height + borderWidth * 2
            )!!
        )

        override fun scale() = 1f

        override fun getFactory(): Function<MultiBufferSource.BufferSource, PictureInPictureRenderer<MultiRadiusState>> {
            return Function { MultiRadiusPIPRenderer(it) }
        }
    }

    class MultiRadiusPIPRenderer(bufferSource: MultiBufferSource.BufferSource) :
        PictureInPictureRenderer<MultiRadiusState>(bufferSource) {

        private var lastState: MultiRadiusState? = null

        override fun getRenderStateClass() = MultiRadiusState::class.java

        override fun textureIsReadyToBlit(state: MultiRadiusState) = lastState?.let { it == state } ?: false

        override fun renderToTexture(state: MultiRadiusState, stack: PoseStack) {
            val bounds = state.bounds
            val scale = client.window.guiScale.toFloat()
            val scaledWidth = (bounds.width() - state.borderWidth * 2) * scale
            val scaledHeight = (bounds.height() - state.borderWidth * 2) * scale

            val buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
            buffer.addVertex(0f, 0f, 0f).setColor(state.color)
            buffer.addVertex(0f, scaledHeight, 0f).setColor(state.color)
            buffer.addVertex(scaledWidth, scaledHeight, 0f).setColor(state.color)
            buffer.addVertex(scaledWidth, 0f, 0f).setColor(state.color)

            PipelineRenderer
                .builder(RoundedRectangle.PIPELINE, buffer.buildOrThrow())
                .uniform(
                    RoundedRectangleUniform.STORAGE,
                    RoundedRectangleUniform.of(
                        Vector4f(
                            ARGB.redFloat(state.borderColor),
                            ARGB.greenFloat(state.borderColor),
                            ARGB.blueFloat(state.borderColor),
                            ARGB.alphaFloat(state.borderColor)
                        ),
                        Vector4f(
                            state.radiusTopLeft,
                            state.radiusTopRight,
                            state.radiusBottomLeft,
                            state.radiusBottomRight
                        ),
                        state.borderWidth.toFloat(),
                        Vector2f(scaledWidth - state.borderWidth * 2, scaledHeight - state.borderWidth * 2),
                        Vector2f(scaledWidth / 2f, scaledHeight / 2f),
                        scale
                    )
                )
                .draw()

            lastState = state
        }

        override fun getTextureLabel() = "starred_rounded_rectangle"
    }
}
