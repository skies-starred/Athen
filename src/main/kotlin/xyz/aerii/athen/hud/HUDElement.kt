package xyz.aerii.athen.hud

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.config.ConfigBuilder
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.library.api.ZERO_PAIR

data class HUDElement(
    val id: String,
    val name: String,
    val config: ConfigBuilder,
    var renderer: GuiGraphics.(Boolean) -> Pair<Int, Int>?,
    var defaultX: Float = 20f,
    var defaultY: Float = 20f,
    var defaultScale: Float = 1f,
    var enabled: Boolean = false,
    var renderOutsidePreview: Boolean = true
) {
    var width: Int = 1
        private set
    var height: Int = 1
        private set

    var x: Float = defaultX
    var y: Float = defaultY
    var scale: Float = defaultScale

    val render: Boolean
        get() = config() && enabled

    val render0: Boolean
        get() = (config.module?.enabled ?: config()) && enabled && renderOutsidePreview

    init {
        ConfigManager.observe(id) { enabled = it as? Boolean ?: false }
    }

    fun render(graphics: GuiGraphics, isPreview: Boolean) {
        val (w, h) = graphics.renderer(isPreview) ?: ZERO_PAIR
        width = w
        height = h
    }

    fun isHovered(mx: Float, my: Float) =
        mx >= x - 4f * scale && mx <= x + (width + 4f) * scale && my >= y - 4f * scale && my <= y + (height + 4f) * scale
}