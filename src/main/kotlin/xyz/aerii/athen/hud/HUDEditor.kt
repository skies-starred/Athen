package xyz.aerii.athen.hud

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.platform.drawOutline
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.platform.translate
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.client
import kotlin.math.roundToInt

@Priority(-1)
object HUDEditor : Scram("HUD Editor [Athen]") {
    private var grid = false
    private var snappy = false
    private var dragging: HUDElement? = null
    private var mx = 0f
    private var my = 0f
    private var x0 = 0f
    private var y0 = 0f

    private val _act: List<HUDElement> by lazy {
        HUDManager.elements.values.sortedBy { it.name }
    }

    private val active: HUDElement?
        get() = dragging ?: _act.filter { it.render }.asReversed().firstOrNull { it.isHovered(mx, my) }

    override fun onScramRender(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        mx = Resolute.mx
        my = Resolute.my

        dragging?.apply {
            x = mx - x0
            y = my - y0

            if (!snappy) return@apply

            val pad = 4f * scale

            x = (((x - pad) / 8f).roundToInt() * 8f) + pad
            y = (((y - pad) / 8f).roundToInt() * 8f) + pad
        }

        Resolute.push(graphics)
        graphics.fill(0, 0, Resolute.width.toInt(), Resolute.height.toInt(), Mocha.Mauve.withAlpha(0.1f))

        if (grid) {
            val color = Mocha.Surface0.withAlpha(0.35f)
            val rw = Resolute.width.toInt()
            val rh = Resolute.height.toInt()

            var gx = 0
            while (gx <= rw) {
                graphics.fill(gx, 0, gx + 1, rh, color)
                gx += 8
            }

            var gy = 0
            while (gy <= rh) {
                graphics.fill(0, gy, rw, gy + 1, color)
                gy += 8
            }
        }

        for (e in _act.filter { it.render }) {
            graphics.pose().pushMatrix()
            graphics.translate(e.x, e.y)
            graphics.scale(e.scale, e.scale)

            graphics.fill(-4, -4, e.width + 4, e.height + 4, Mocha.Base.withAlpha(0.5f))
            graphics.drawOutline(-4, -4, e.width + 8, e.height + 8, Mocha.Text.argb)

            graphics.pose().pushMatrix()
            e.render(graphics, true)
            graphics.pose().popMatrix()

            graphics.pose().popMatrix()
        }

        active?.let { e ->
            val text = e.name + if (Dev.debug) " | ${e.x} | ${e.y}" else ""
            val textWidth = client.font.width(text)
            val textHeight = client.font.lineHeight

            graphics.pose().pushMatrix()
            graphics.translate(mx + 12, my - textHeight / 2)

            graphics.fill(-6, -6, textWidth + 6, textHeight + 6, Mocha.Base.withAlpha(0.8f))
            graphics.drawOutline(-6, -6, textWidth + 12, textHeight + 12, Mocha.Text.argb)
            graphics.text(text, 0, 0, false, Mocha.Text.argb)
            graphics.pose().popMatrix()
        }

        Help.render(graphics)
        Resolute.pop(graphics)
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (Help.hovered(mx, my)) {
            Help.dragging = true
            Help.x0 = mx - Help.x
            Help.y0 = my - Help.y
            return true
        }

        val hovered = active ?: return false

        dragging = hovered
        x0 = mx - hovered.x
        y0 = my - hovered.y
        return true
    }

    override fun onScramMouseRelease(mouseX: Int, mouseY: Int, button: Int): Boolean {
        dragging = null
        Help.dragging = false
        return false
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        val hovered = active ?: return false

        val scaleDelta = if (vertical > 0) 0.1f else -0.1f
        hovered.scale = (hovered.scale + scaleDelta).coerceIn(0.2f, 5.0f)
        return true
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_G) {
            if ((modifiers and GLFW.GLFW_MOD_CONTROL) != 0) snappy = !snappy
            else grid = !grid

            return true
        }

        val step = if ((modifiers and GLFW.GLFW_MOD_SHIFT) != 0) 8f else 1f

        when (keyCode) {
            GLFW.GLFW_KEY_H -> {
                val e = active ?: return false
                e.x = (Resolute.width - e.width * e.scale) / 2f
                return true
            }

            GLFW.GLFW_KEY_V -> {
                val e = active ?: return false
                e.y = (Resolute.height - e.height * e.scale) / 2f
                return true
            }

            GLFW.GLFW_KEY_LEFT -> {
                val e = active ?: return false
                e.x -= step
                return true
            }

            GLFW.GLFW_KEY_RIGHT -> {
                val e = active ?: return false
                e.x += step
                return true
            }

            GLFW.GLFW_KEY_UP -> {
                val e = active ?: return false
                e.y -= step
                return true
            }

            GLFW.GLFW_KEY_DOWN -> {
                val e = active ?: return false
                e.y += step
                return true
            }

            GLFW.GLFW_KEY_R -> {
                val ctrl = (modifiers and GLFW.GLFW_MOD_CONTROL) != 0
                val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0

                if (ctrl && shift) {
                    for (e in HUDManager.elements.values) {
                        e.x = e.defaultX
                        e.y = e.defaultY
                        e.scale = e.defaultScale
                    }

                    return true
                }

                val e = active ?: return false
                e.x = e.defaultX
                e.y = e.defaultY
                e.scale = e.defaultScale
                return true
            }
        }

        return false
    }

    override fun onScramClose() = HUDManager.set()

    override fun isPauseScreen(): Boolean = false

    private object Help {
        private var w = 0
        private var h = 0

        var t0 = 0

        var x = 400f
        var y = 400f

        var dragging = false
        var x0 = 0f
        var y0 = 0f

        private val lines = listOf(
            { false to "Arrow keys to move, shift maybe" },
            { false to "H = center horizontally" },
            { false to "V = center vertically" },
            { false to "R = reset" },
            { false to "Ctrl + Shift + R = reset all" },
            { grid to "G = toggle grid" },
            { snappy to "Ctrl + G = toggle snap to grid" },
        )

        init {
            fn()
        }

        fun hovered(mx: Float, my: Float): Boolean =
            mx >= x - 4f && mx <= x + w + 4f && my >= y - 4f && my <= y + h + 4f

        fun render(graphics: GuiGraphics): Unit = with (graphics) {
            if (w == 0 || h == 0) fn()
            val font = client.font ?: return@with

            if (dragging) {
                x = mx - x0
                y = my - y0
            }

            pose().pushMatrix()
            pose().translate(x, y)

            fill(-4, -4, w + 4, h + 4, Mocha.Base.withAlpha(0.6f))
            drawOutline(-4, -4, w + 8, h + 8, Mocha.Mauve.argb)

            var yOff = 0
            for (entry in lines) {
                val (enabled, text) = entry()

                text("•", 0, yOff, false, if (enabled) Mocha.Green.argb else Mocha.Red.argb)
                text(text, t0, yOff, false, Mocha.Text.argb)

                yOff += font.lineHeight
            }

            pose().popMatrix()
        }

        private fun fn() {
            val font = client.font ?: return
            var y = 0
            var maxWidth = 0

            t0 = font.width("• ")

            for (line in lines) {
                val w = font.width(line().second) + t0
                maxWidth = maxWidth.coerceAtLeast(w)
                y += font.lineHeight
            }

            w = maxWidth
            h = y
        }
    }
}