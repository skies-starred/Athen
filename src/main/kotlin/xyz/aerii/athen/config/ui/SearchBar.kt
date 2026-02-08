@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.config.ui

import xyz.aerii.athen.config.ui.elements.base.IInput
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue

class SearchBar(private val onSearch: (String) -> Unit) {
    private val textInput = object : IInput(
        "", "", "",
        { _, newValue -> onSearch(newValue as String) },
        placeholder = "Search...",
        textColor = Mocha.Text.argb,
        placeholderColor = Mocha.Subtext0.argb
    ) {
        private val `anim$bg` = springValue(Mocha.Base.argb, 0.15f)

        init { value = "" }

        override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
            lastX = x
            lastY = y

            val isHovered = isAreaHovered(x + 10f, y + 9f, 330f, 22f)
            `anim$bg`.value = if (isHovered) Mocha.Surface0.argb else Mocha.Base.argb

            NVGRenderer.drawDropShadow(x, y, 350f, 40f, 10f, 0.75f, 9f)
            NVGRenderer.drawRectangle(x, y, 350f, 40f, `anim$bg`.value, 9f)
            NVGRenderer.drawHollowRectangle(x, y, 350f, 40f, 3f, Mocha.Mauve.argb, 9f)

            inputX = x + 10f
            inputY = y + 9f
            inputWidth = 330f
            inputHeight = 22f

            drawInput(mouseX, mouseY)
            return 40f
        }
    }

    fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float) = textInput.draw(x, y, mouseX, mouseY)

    fun mouseClicked(mouseX: Float, mouseY: Float, mouseButton: Int) = textInput.mouseClicked(mouseX, mouseY, mouseButton)

    fun mouseReleased() = textInput.mouseReleased(0)

    fun keyPressed(keyCode: Int) = textInput.keyPressed(keyCode, 0)

    fun keyTyped(typedChar: Char) = textInput.keyTyped(typedChar)
}
