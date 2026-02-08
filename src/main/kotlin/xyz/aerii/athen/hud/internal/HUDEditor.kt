package xyz.aerii.athen.hud.internal

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.platform.translate
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Scurry
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.RoundedRect

@Priority(-1)
object HUDEditor : Scram("HUD Editor [Athen]") {
    private var dragging: HUDElement? = null
    private var offsetX = 0f
    private var offsetY = 0f

    private val active: HUDElement?
        get() = dragging ?: HUDManager.elements.values.filter { it.renderEditor }.firstOrNull { it.isHovered(Scurry.rawX, Scurry.rawY) }

    override fun onScramClose() {
        HUDManager.saveLayouts()
    }

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        dragging?.let {
            it.x = Scurry.rawX - offsetX
            it.y = Scurry.rawY - offsetY
        }

        guiGraphics.fill(0, 0, width, height, Mocha.Crust.withAlpha(0.9f))

        for (element in HUDManager.elements.values) {
            if (!element.renderEditor) continue

            val hovered = element.isHovered(Scurry.rawX, Scurry.rawY)
            val borderColor = if (hovered) Mocha.Green.argb else Mocha.Surface2.argb
            val bgColor = Mocha.Base.withAlpha(if (hovered) 0.7f else 0.4f)

            val scale = element.scaledScale
            val padding = 4f
            val headerHeight = 16f

            val contentWidth = element.width * scale + padding * 2
            val contentHeight = element.height * scale + padding * 2

            val x = element.scaledX - padding
            val y = element.scaledY - padding - headerHeight

            val xi = x.toInt()
            val yi = y.toInt()

            val font = client.font
            val name = element.name
            val nameWidth = font.width(name) + 6

            RoundedRect.draw(guiGraphics, xi, yi, nameWidth, headerHeight.toInt(), bgColor, borderColor, 4f, 4f, 0f, 0f, 1)
            guiGraphics.drawString(font, name, xi + 4, yi + 4, Mocha.Text.argb, false)
            RoundedRect.draw(guiGraphics, xi, (y + headerHeight - 1).toInt(), contentWidth.toInt(), contentHeight.toInt(), bgColor, borderColor, 0f, 4f, 4f, 4f, 1)

            guiGraphics.pushPop {
                guiGraphics.translate(element.scaledX, element.scaledY)
                guiGraphics.scale(scale, scale)
                guiGraphics.pushPop { element.render(guiGraphics, true) }
            }
        }

        guiGraphics.drawString(client.font, "Drag elements. Scroll to scale. Press ESC to save and exit.", 10, 10, Mocha.Subtext0.argb, true)
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        val hovered = active ?: return super.onScramMouseClick(mouseX, mouseY, button)

        dragging = hovered
        offsetX = Scurry.rawX - hovered.x
        offsetY = Scurry.rawY - hovered.y
        return true
    }

    override fun onScramMouseRelease(mouseX: Int, mouseY: Int, button: Int): Boolean {
        dragging = null
        return super.onScramMouseRelease(mouseX, mouseY, button)
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        val hovered = active ?: return false

        val scaleDelta = if (vertical > 0) 0.1f else -0.1f
        hovered.scale = (hovered.scale + scaleDelta).coerceIn(0.2f, 5.0f)
        return true
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val element = active ?: return false

        val step = when {
            OmniKeyboard.isCtrlKeyPressed -> 10f
            OmniKeyboard.isShiftKeyPressed -> 5f
            else -> 1f
        }

        when (keyCode) {
            GLFW.GLFW_KEY_C -> {
                val centerX = client.window.width / 2f
                element.x = centerX - element.width * element.rawScale / 2f
                return true
            }

            GLFW.GLFW_KEY_LEFT -> element.x -= step
            GLFW.GLFW_KEY_RIGHT -> element.x += step
            GLFW.GLFW_KEY_UP -> element.y -= step
            GLFW.GLFW_KEY_DOWN -> element.y += step

            else -> return false
        }

        return super.onScramKeyPress(keyCode, scanCode, modifiers)
    }

    override fun isPauseScreen(): Boolean = false
}
