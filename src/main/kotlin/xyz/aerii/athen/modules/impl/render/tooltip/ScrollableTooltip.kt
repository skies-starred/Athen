@file:Suppress("ObjectPrivatePropertyName", "ObjectPropertyName", "Unused")

package xyz.aerii.athen.modules.impl.render.tooltip

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.modules.Module
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.pressed

@Load
object ScrollableTooltip : Module(
    "Scrollable tooltip",
    "Allows you to scroll tooltips. Does nothing if \"CustomTooltip\" is enabled!",
    Category.RENDER
) {
    private val horizontal by config.switch("Horizontal", true)
    private val `horizontal$key` by config.keybind("Horizontal keybind", GLFW.GLFW_KEY_LEFT_SHIFT).dependsOn { horizontal }
    private val `horizontal$speed` by config.slider("Horizontal speed", 8, 1, 20, "pixels").dependsOn { horizontal }

    private val vertical by config.switch("Vertical", true)
    private val `vertical$place` by config.switch("Vertical in place", true).dependsOn { vertical }
    private val `vertical$speed` by config.slider("Vertical speed", 8, 1, 20, "pixels").dependsOn { vertical }

    private val scale by config.switch("Scale tooltip")
    private val `scale$key` by config.keybind("Scale keybind", GLFW.GLFW_KEY_LEFT_CONTROL).dependsOn { scale }
    private val `scale$dynamic` by config.switch("Dynamic scale", true).dependsOn { scale }
    private val `scale$dynamic$text` by config.textParagraph("Dynamic scale automatically scales the tooltip to fit on your screen!").dependsOn { scale }

    private val reset by config.switch("Reset on hover")

    private val background = ResourceLocation.withDefaultNamespace("tooltip/background")
    private val frame = ResourceLocation.withDefaultNamespace("tooltip/frame")

    private var last: Int = 0

    private var xo: Double = 0.0
    private var yo: Double = 0.0
    private var mss: Double = 0.0

    private var sc: Double = 1.0

    init {
        on<GuiEvent.Slots.Hover> {
            if (reset) reset()
        }

        on<GuiEvent.Close.Any> {
            reset()
        }

        on<GuiEvent.Input.Mouse.Scroll> {
            if (last != Chronos.ticks.client) return@on

            if (scale && `scale$key`.bound && `scale$key`.pressed) {
                sc = (sc + amount * 0.1).coerceIn(0.5, 3.0)
                return@on
            }

            if (horizontal && `horizontal$key`.bound && `horizontal$key`.pressed) {
                xo += amount * `horizontal$speed`
                return@on
            }

            if (vertical) {
                val n = (yo + amount * `vertical$speed`).coerceAtLeast(mss)
                yo = if (yo != 0.0 && (n > 0) != (yo > 0)) 0.0 else n
            }
        }
    }

    @JvmStatic
    fun GuiGraphics.fn(font: Font, components: List<ClientTooltipComponent>, x: Int, y: Int, positioner: ClientTooltipPositioner, background: ResourceLocation?) {
        var i = 0
        var j = if (components.size == 1) -2 else 0

        last = Chronos.ticks.client

        for (c in components) {
            val k = c.getWidth(font)
            if (k > i) i = k
            j += c.getHeight(font)
        }

        val v = positioner.positionTooltip(guiWidth(), guiHeight(), x, y, i, j)
        val n = v.x()
        val o = if (j + 8 < guiHeight() - 40) v.y().coerceIn(20, guiHeight() - 20 - j - 8) else 20

        val mh = minOf(j + 8, guiHeight() - 20 - o).coerceAtLeast(0)
        mss = if (`vertical$place`) -j.toDouble() else Double.NEGATIVE_INFINITY
        yo = yo.coerceIn(mss, if (`vertical$place`) j.toDouble() else Double.POSITIVE_INFINITY)

        val pose = pose()

        pose.pushMatrix()

        val s = if (scale) (if (`scale$dynamic` && j + 8 > mh) (mh.toFloat() / (j + 8)).coerceIn(0.5f, 1f) else sc.toFloat()) else 1f
        pose.translate((n - 4).toFloat(), (o - 4).toFloat())
        pose.scale(s, s)
        pose.translate(-(n - 4).toFloat(), -(o - 4).toFloat())
        pose.translate(xo.toFloat(), if (!`vertical$place`) yo.toFloat() else 0f)

        val bh = (if (yo > 0) mh - yo.toInt() else j + yo.toInt() + 8).coerceIn(0, guiHeight() - 20 - o)
        blitSprite(RenderPipelines.GUI_TEXTURED, fn0(background), n - 12, o - 12, i + 24, (if (`vertical$place`) bh - 8 else j) + 24)

        val b = `vertical$place`
        val l = components.withIndex()
        if (b) enableScissor(n - 4, o - 4, n - 4 + i + 8, o - 4 + bh)

        val a = if (b) o + yo.toInt().coerceAtMost(0) else o
        var p = a
        for ((i, c) in l) {
            c.renderText(this, font, n, p)
            p += c.getHeight(font) + if (i == 0) 2 else 0
        }

        p = a
        for ((io, c) in l) {
            c.renderImage(font, n, p, i, j, this)
            p += c.getHeight(font) + if (io == 0) 2 else 0
        }

        if (b) disableScissor()

        blitSprite(RenderPipelines.GUI_TEXTURED, fn1(background), n - 12, o - 12, i + 24, (if (`vertical$place`) bh - 8 else j) + 24)
        pose.popMatrix()
    }

    private fun reset() {
        xo = 0.0
        yo = 0.0
        sc = 1.0
        mss = 0.0
    }

    private fun fn0(r: ResourceLocation?): ResourceLocation {
        return r?.withPath { "tooltip/${it}_background" } ?: background
    }

    private fun fn1(r: ResourceLocation?): ResourceLocation {
        return r?.withPath { "tooltip/${it}_frame" } ?: frame
    }
}