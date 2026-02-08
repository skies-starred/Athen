@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.animations.springValue

class ButtonElement(
    text: String,
    private val onClick: () -> Unit
) : IBaseUI(text, "", { _, _ -> }) {

    private val `anim$bg` = springValue(Mocha.Base.argb, 0.2f)
    private val `anim$hover` = springValue(1f, 0.3f)
    private val `anim$click` = springValue(1f, 0.5f)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        val buttonX = x + 6f
        val buttonY = y + 6f
        val buttonW = width - 12f
        val buttonH = 26f

        val isHovered = isAreaHovered(buttonX, buttonY, buttonW, buttonH)

        `anim$bg`.value = if (isHovered) Mocha.Surface2.argb else Mocha.Base.argb
        `anim$hover`.value = if (isHovered) 1.025f else 1f

        val s = `anim$hover`.value * `anim$click`.value
        val rect = drawScaledBox(buttonX + buttonW / 2f, buttonY + buttonH / 2f, buttonW, buttonH, s, `anim$bg`.value, Mocha.Surface0.argb, 5f, 1f)
        val textW = textWidth(name)
        drawText(name, x + width / 2f - textW / 2f, rect.y + (rect.h - 16f) / 2f)

        return 38f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val buttonX = lastX + 6f
        val buttonY = lastY + 6f
        if (!isAreaHovered(buttonX, buttonY, width - 12f, 26f)) return false

        `anim$click`.value = 0.92f
        onClick()
        return true
    }

    override fun mouseReleased(button: Int) {
        if (button == 0) `anim$click`.value = 1f
    }

    override fun getHeight() = 38f
}