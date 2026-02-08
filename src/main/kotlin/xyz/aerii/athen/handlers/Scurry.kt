package xyz.aerii.athen.handlers

import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.max

/**
 * Scurry is a tiny mouse goblin that knows where your cursor is at all times.
 */
object Scurry {
    val rawX: Float
        get() = McClient.self.mouseHandler.xpos().toFloat()

    val rawY: Float
        get() = McClient.self.mouseHandler.ypos().toFloat()

    val scaledX: Float
        get() = rawX * McClient.window.guiScaledWidth / max(1, McClient.window.width)

    val scaledY: Float
        get() = rawY * McClient.window.guiScaledHeight / max(1, McClient.window.height)

    @JvmStatic
    @JvmOverloads
    fun isAreaHovered(x: Float, y: Float, w: Float, h: Float, scaled: Boolean = false): Boolean {
        return if (scaled) scaledX in x..(x + w) && scaledY in y..(y + h) else rawX in x..(x + w) && rawY in y..(y + h)
    }

    @JvmStatic
    @JvmOverloads
    fun isAreaHovered(x: Float, y: Float, w: Float, scaled: Boolean = false): Boolean {
        return if (scaled) scaledX in x..(x + w) && scaledY >= y else rawX in x..(x + w) && rawY >= y
    }
}
