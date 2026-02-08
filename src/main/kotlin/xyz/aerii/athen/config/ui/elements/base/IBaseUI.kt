@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements.base

import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer

abstract class IBaseUI(
    protected val name: String,
    protected val configKey: String,
    protected val onUpdate: (String, Any) -> Unit
) {
    protected data class ScaledRect(val x: Float, val y: Float, val w: Float, val h: Float)

    protected var lastX = 0f
    protected var lastY = 0f
    protected val width = 240f

    abstract fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float

    open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean = false

    open fun mouseReleased(button: Int) {}

    open fun keyTyped(typedChar: Char): Boolean = false

    open fun keyPressed(keyCode: Int, scanCode: Int): Boolean = false

    abstract fun getHeight(): Float

    protected fun drawText(text: String, x: Float, y: Float, size: Float = 16f, color: Int = Mocha.Text.argb) =
        NVGRenderer.drawText(text, x, y, size, color, NVGRenderer.defaultFont)

    protected fun textWidth(text: String, size: Float = 16f): Float =
        NVGRenderer.getTextWidth(text, size, NVGRenderer.defaultFont)

    protected fun drawBox(x: Float, y: Float, w: Float, h: Float, bgColor: Int, borderColor: Int, radius: Float = 5f) {
        NVGRenderer.drawRectangle(x, y, w, h, bgColor, radius)
        NVGRenderer.drawHollowRectangle(x, y, w, h, 1f, borderColor, radius)
    }

    protected fun drawScaledBox(centerX: Float, centerY: Float, w: Float, h: Float, scale: Float, bgColor: Int, borderColor: Int, radius: Float = 5f, borderWidth: Float = 1.5f): ScaledRect {
        val scaledW = w * scale
        val scaledH = h * scale
        val scaledX = centerX - scaledW / 2f
        val scaledY = centerY - scaledH / 2f
        NVGRenderer.drawRectangle(scaledX, scaledY, scaledW, scaledH, bgColor, radius)
        NVGRenderer.drawHollowRectangle(scaledX, scaledY, scaledW, scaledH, borderWidth, borderColor, radius)
        return ScaledRect(scaledX, scaledY, scaledW, scaledH)
    }

    protected fun drawCaret(x: Float, y: Float, h: Float, blinkTime: Long, color: Int = Mocha.Text.argb): Long {
        val now = System.currentTimeMillis()
        if (now - blinkTime < 500) NVGRenderer.drawLine(x, y, x, y + h, 1.5f, color)
        return if (now - blinkTime > 1000) now else blinkTime
    }

    protected fun radii(index: Int, total: Int, r: Float = 5f): FloatArray {
        val first = index == 0
        val last = index == total - 1
        return floatArrayOf(if (first) r else 0f, if (first) r else 0f, if (last) r else 0f, if (last) r else 0f)
    }
}
