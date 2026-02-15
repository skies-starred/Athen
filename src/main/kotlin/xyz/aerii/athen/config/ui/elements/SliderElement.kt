@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue

class SliderElement(
    name: String,
    private var value: Double,
    private val min: Double,
    private val max: Double,
    private val showDouble: Boolean,
    private val unit: String,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {

    private var dragging = false
    private var editing = false
    private var editText = ""
    private var caretBlink = System.currentTimeMillis()
    private val `anim$tooltip` = springValue(0f, 0.25f)

    private val tooltipText = "Click value to edit"
    private val tooltipW = textWidth(tooltipText, 12f) + 8f

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        if (dragging) {
            val percent = ((mouseX - (x + 6f)) / (width - 12f)).coerceIn(0f, 1f)
            value = min + percent * (max - min)
            onUpdate(configKey, value)
        }

        val rawText = if (editing) editText else str()
        val valueText = if (unit.isNotEmpty()) "$rawText $unit" else rawText
        val rawW = textWidth(rawText)

        drawText(name, x + 6f, y + 8f)

        val textW = textWidth(valueText)
        val textX = x + width - textW - 6f
        val textY = y + 8f
        val isHovered = isAreaHovered(lastX, lastY + 20f, width, 16f)

        `anim$tooltip`.value = if (isHovered && !editing) 1f else 0f
        drawText(rawText, textX, textY, color = if (editing) Mocha.Text.argb else Mocha.Subtext0.argb)
        if (unit.isNotEmpty()) drawText(" $unit", textX + rawW, textY, color = Mocha.Subtext0.argb)

        if (editing) caretBlink = drawCaret(textX + rawW + 2f, textY, 16f, caretBlink)
        if (`anim$tooltip`.value > 0f) drawTooltip(x, y)

        val sliderY = y + 28f
        NVGRenderer.drawRectangle(x + 6f, sliderY, width - 12f, 8f, Mocha.Crust.argb, 3f)

        val percentage = ((value - min) / (max - min)).toFloat()
        if (percentage > 0f) NVGRenderer.drawRectangle(x + 6f, sliderY, percentage * (width - 12f), 8f, Mocha.Mauve.argb, 3f)

        val thumbX = x + 6f + percentage * (width - 12f)
        NVGRenderer.drawCircle(thumbX, sliderY + 4f, 7.5f, Mocha.Text.argb)

        return 44f
    }

    private fun str() = if (showDouble) "%.1f".format(value) else value.toInt().toString()

    private fun drawTooltip(x: Float, y: Float) {
        val tooltipX = (x + width / 2f) - tooltipW / 2f
        NVGRenderer.globalAlpha(`anim$tooltip`.value)
        NVGRenderer.drawRectangle(tooltipX, y + 6f, tooltipW, 18f, Mocha.Base.argb, 3f)
        NVGRenderer.drawText(tooltipText, tooltipX + 4f, y + 6f + 3f, 12f, Mocha.Text.argb)
        NVGRenderer.globalAlpha(1f)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        val str = str()
        val textW = textWidth(if (str.isEmpty()) "" else if (unit.isNotEmpty()) "$str $unit" else str)
        val textX = lastX + width - textW - 6f
        val textY = lastY + 8f

        if (isAreaHovered(textX - 4f, textY - 2f, textW + 8f, 20f)) {
            editing = true
            editText = str
            caretBlink = System.currentTimeMillis()
            return true
        }

        editing = false
        if (isAreaHovered(lastX, lastY + 20f, width, 16f)) {
            dragging = true
            return true
        }

        return false
    }

    override fun mouseReleased(button: Int) {
        if (button == 0) dragging = false
    }

    override fun keyTyped(typedChar: Char): Boolean {
        if (!editing) return false
        if (typedChar.isDigit() || (typedChar == '.' && showDouble && !editText.contains('.'))) {
            editText += typedChar
            caretBlink = System.currentTimeMillis()
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int): Boolean {
        if (!editing) return false

        when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (editText.isEmpty()) return true
                editText = editText.dropLast(1)
                caretBlink = System.currentTimeMillis()
                return true
            }

            GLFW.GLFW_KEY_ENTER -> {
                editText.toDoubleOrNull()?.let {
                    value = it.coerceIn(min, max)
                    onUpdate(configKey, value)
                }

                editing = false
                return true
            }

            GLFW.GLFW_KEY_ESCAPE -> {
                editing = false
                return true
            }

            GLFW.GLFW_KEY_V -> {
                if (!OmniKeyboard.isCtrlKeyPressed) return true
                val clipboard = client.keyboardHandler?.clipboard ?: ""
                val clean = clipboard.filter { it.isDigit() || (it == '.' && showDouble) }
                if (clean.isNotEmpty()) {
                    editText = clean
                    caretBlink = System.currentTimeMillis()
                }
                return true
            }
        }
        return false
    }

    override fun getHeight() = 44f
}
