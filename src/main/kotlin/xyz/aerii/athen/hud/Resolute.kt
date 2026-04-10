@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.hud

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.library.api.client
import xyz.aerii.library.utils.mouseSX
import xyz.aerii.library.utils.mouseSY

// Defaults to 1080 / 2, intended :eyes:
object Resolute {
    const val height = 540f

    var scale: Float = 1f
        private set

    var width: Float = 960f
        private set

    val mx: Float
        get() = s(mouseSX)

    val my: Float
        get() = s(mouseSY)

    fun s(f: Float): Float {
        return f / scale
    }

    fun push(graphics: GuiGraphics) {
        graphics.pose().pushMatrix()
        graphics.pose().scale(scale, scale)
    }

    fun pop(graphics: GuiGraphics) {
        graphics.pose().popMatrix()
    }

    /**
     * @see xyz.aerii.athen.mixin.mixins.WindowMixin
     */
    @JvmStatic
    fun update() {
        scale = client.window.guiScaledHeight.toFloat() / height
        width = client.window.guiScaledWidth.toFloat() / scale
    }
}