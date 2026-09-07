package foo.starred.athen.hud

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.Priority
import foo.starred.athen.api.rendering.ui.effects.outline.outline
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.extractText
import foo.starred.athen.modules.impl.Dev
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.snowbird.api.client
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.mouseSX
import foo.starred.snowbird.utils.mouseSY
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import kotlin.math.roundToInt

@Priority(-1)
object HUDEditor : Screen("HUD Editor [Athen]".literal()) {
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

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        mx = mouseSX / HUDManager.scale
        my = mouseSY / HUDManager.scale

        dragging?.apply {
            x = mx - x0
            y = my - y0

            if (!snappy) return@apply
            val pad = 4f * scale
            x = (((x - pad) / 8f).roundToInt() * 8f) + pad
            y = (((y - pad) / 8f).roundToInt() * 8f) + pad
        }

        graphics.pose().pushMatrix()
        graphics.pose().scale(HUDManager.scale)
        graphics.fill(0, 0, HUDManager.width.toInt(), HUDManager.height.toInt(), Mocha.Lavender.withAlpha(0.1f))

        if (grid) {
            val color = Mocha.Surface0.withAlpha(0.35f)
            val rw = HUDManager.width.toInt()
            val rh = HUDManager.height.toInt()

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
            graphics.pose().translate(e.x, e.y)
            graphics.pose().scale(e.scale, e.scale)

            graphics.fill(-4, -4, e.width + 4, e.height + 4, Mocha.Base.withAlpha(0.5f))
            graphics.outline(-4, -4, e.width + 8, e.height + 8, 1, Mocha.Text.argb)

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
            graphics.pose().translate(mx + 12, my - textHeight / 2)

            graphics.fill(-6, -6, textWidth + 6, textHeight + 6, Mocha.Base.withAlpha(0.8f))
            graphics.outline(-6, -6, textWidth + 12, textHeight + 12, 1, Mocha.Text.argb)
            graphics.extractText(text, 0, 0, false, Mocha.Text.argb)
            graphics.pose().popMatrix()
        }

        Help.render(graphics)
        graphics.pose().popMatrix()

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (Help.hovered(mx, my)) {
            Help.dragging = true
            Help.x0 = mx - Help.x
            Help.y0 = my - Help.y
            return true
        }

        val hovered = active ?: return super.mouseClicked(event, doubleClick)

        dragging = hovered
        x0 = mx - hovered.x
        y0 = my - hovered.y
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dragging = null
        Help.dragging = false
        return false
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val hovered = active ?: return super.mouseScrolled(x, y, scrollX, scrollY)

        val scaleDelta = if (scrollY > 0) 0.1f else -0.1f
        hovered.scale = (hovered.scale + scaleDelta).coerceIn(0.2f, 5.0f)
        return true
    }

    override fun mouseMoved(x: Double, y: Double) {
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_G) {
            if ((event.modifiers() and InputConstants.MOD_CONTROL) != 0) snappy = !snappy
            else grid = !grid

            return true
        }

        val step = if ((event.modifiers() and InputConstants.MOD_SHIFT) != 0) 8f else 1f

        when (event.key()) {
            InputConstants.KEY_H -> {
                val e = active ?: return false
                e.x = (HUDManager.width - e.width * e.scale) / 2f
                return true
            }

            InputConstants.KEY_V -> {
                val e = active ?: return false
                e.y = (HUDManager.height - e.height * e.scale) / 2f
                return true
            }

            InputConstants.KEY_LEFT -> {
                val e = active ?: return false
                e.x -= step
                return true
            }

            InputConstants.KEY_RIGHT -> {
                val e = active ?: return false
                e.x += step
                return true
            }

            InputConstants.KEY_UP -> {
                val e = active ?: return false
                e.y -= step
                return true
            }

            InputConstants.KEY_DOWN -> {
                val e = active ?: return false
                e.y += step
                return true
            }

            InputConstants.KEY_R -> {
                val ctrl = (event.modifiers() and InputConstants.MOD_CONTROL) != 0
                val shift = (event.modifiers() and InputConstants.MOD_SHIFT) != 0

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

        return super.keyPressed(event)
    }

    override fun onClose() {
        super.onClose()
        HUDManager.set()
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

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

        fun render(graphics: GuiGraphicsExtractor): Unit = with (graphics) {
            if (w == 0 || h == 0) fn()
            val font = client.font ?: return@with

            if (dragging) {
                x = mx - x0
                y = my - y0
            }

            pose().pushMatrix()
            pose().translate(x, y)

            fill(-4, -4, w + 4, h + 4, Mocha.Base.withAlpha(0.6f))
            outline(-4, -4, w + 8, h + 8, 1, Mocha.Lavender.argb)

            var yOff = 0
            for (entry in lines) {
                val (enabled, text) = entry()

                extractText("•", 0, yOff, false, if (enabled) Mocha.Green.argb else Mocha.Red.argb)
                extractText(text, t0, yOff, false, Mocha.Text.argb)

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
