@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ui.elements.base.IInput
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue

class TextInputElement(
    name: String,
    initialValue: String,
    private val maxLength: Int,
    configKey: String,
    onUpdate: (String, Any) -> Unit,
    placeholder: String = "",
    textColor: Int = Mocha.Text.argb,
    placeholderColor: Int = Mocha.Subtext0.argb,
    caretColor: Int = Mocha.Text.argb,
    selectionColor: Int = Mocha.Mauve.withAlpha(0.3f)
) : IInput(name, initialValue, configKey, onUpdate, placeholder, textColor, placeholderColor, caretColor, selectionColor) {

    private val `anim$bg` = springValue(Mocha.Base.argb, 0.2f)
    private val `anim$border` = springValue(Mocha.Surface0.argb, 0.25f)
    private val `anim$scale` = springValue(1f, 0.3f)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        drawText(name, x + 6f, y + 6f)

        val inputBoxX = x + 10f
        val inputBoxY = y + 28f
        val inputBoxW = width - 20f
        val inputBoxH = 32f
        val isHovered = isAreaHovered(inputBoxX, inputBoxY, inputBoxW, inputBoxH)

        `anim$bg`.value = when {
            listening -> Mocha.Surface2.argb
            isHovered -> Mocha.Surface0.argb
            else -> Mocha.Base.argb
        }
        `anim$border`.value = if (listening) Mocha.Mauve.argb else Mocha.Surface0.argb
        `anim$scale`.value = if (listening) 1.02f else 1f

        val centerX = inputBoxX + inputBoxW / 2f
        val centerY = inputBoxY + inputBoxH / 2f
        val scaledW = inputBoxW * `anim$scale`.value
        val scaledH = inputBoxH * `anim$scale`.value
        val scaledX = centerX - scaledW / 2f
        val scaledY = centerY - scaledH / 2f

        NVGRenderer.drawRectangle(scaledX, scaledY, scaledW, scaledH, `anim$bg`.value, 5f)
        NVGRenderer.drawHollowRectangle(scaledX, scaledY, scaledW, scaledH, 1f, `anim$border`.value, 5f)

        inputHeight = 18f
        inputX = scaledX + 6f
        inputY = scaledY + (scaledH - inputHeight) / 2f
        inputWidth = scaledW - 12f

        drawInput(mouseX, mouseY)
        return 64f
    }

    override fun keyTyped(typedChar: Char): Boolean {
        if (value.length >= maxLength && selection == caret) return false
        return super.keyTyped(typedChar)
    }

    override fun getHeight() = 64f
}