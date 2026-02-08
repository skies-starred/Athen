package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer

class FeatureTooltip {
    var currentText = ""
    var visible = false
    var x = 0f
    var y = 0f

    private val maxWidth = 350f
    private val padding = floatArrayOf(8f, 12f, 8f, 12f)
    private val fontSize = 14f

    fun setText(text: String) {
        currentText = text
        visible = text.isNotEmpty()
    }

    fun draw(mouseX: Float, mouseY: Float) {
        if (!visible || currentText.isEmpty()) return

        x = mouseX + 10f
        y = mouseY + 10f
        val max = maxWidth - padding[1] - padding[3]

        val textWidth = NVGRenderer.getWrappedTextWidth(currentText, fontSize, max)
        val textHeight = NVGRenderer.getWrappedTextHeight(currentText, fontSize, max)

        val contentWidth = maxWidth.coerceAtMost(textWidth + padding[1] + padding[3])
        val contentHeight = textHeight + padding[0] + padding[2]

        NVGRenderer.drawRectangle(x, y, contentWidth, contentHeight, Mocha.Base.argb, 6f)
        NVGRenderer.drawHollowRectangle(x, y, contentWidth, contentHeight, 1f, Mocha.Surface0.argb, 6f)
        NVGRenderer.drawTextWrapped(currentText, x + padding[3], y + padding[0], fontSize, max)
    }
}