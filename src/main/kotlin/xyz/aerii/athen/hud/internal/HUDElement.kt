package xyz.aerii.athen.hud.internal

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.config.ConfigBuilder
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.utils.render.toScaleMC

data class HUDElement(
    val id: String,
    val name: String,
    val config: ConfigBuilder,
    var renderer: GuiGraphics.(Boolean) -> Pair<Int, Int>?,
    var enabled: Boolean = false,
    var renderOutsidePreview: Boolean = true
) {
    var width: Int = 1
        private set
    var height: Int = 1
        private set

    var rawX: Float = 20f
        private set
    var rawY: Float = 20f
        private set
    var rawScale: Float = 1f
        private set

    var scaledX: Float = rawX
        private set
    var scaledY: Float = rawY
        private set
    var scaledScale: Float = rawScale
        private set

    var x: Float
        get() = rawX
        set(value) {
            rawX = value
            scaledX = value.toScaleMC()
        }

    var y: Float
        get() = rawY
        set(value) {
            rawY = value
            scaledY = value.toScaleMC()
        }

    var scale: Float
        get() = rawScale
        set(value) {
            rawScale = value
            scaledScale = value.toScaleMC()
        }

    val renderEditor: Boolean
        get() = config() && enabled

    val renderNormal: Boolean
        get() = (config.module?.react?.value ?: config()) && enabled

    init {
        ConfigManager.observe(id) { enabled = it as? Boolean ?: false }
    }

    fun render(graphics: GuiGraphics, isPreview: Boolean) {
        val (w, h) = graphics.renderer(isPreview) ?: (0 to 0)
        width = w
        height = h
    }

    /**
     * Called when the minecraft gui scale changes.
     * @see xyz.aerii.athen.mixin.mixins.WindowMixin
     */
    fun refreshScale() {
        x = x
        y = y
        scale = scale
    }

    fun isHovered(rawX: Float, rawY: Float): Boolean =
        rawX in this@HUDElement.rawX..(this@HUDElement.rawX + (width * rawScale)) && rawY in this@HUDElement.rawY..(this@HUDElement.rawY + (height * rawScale))
}
