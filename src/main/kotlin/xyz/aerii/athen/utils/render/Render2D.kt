package xyz.aerii.athen.utils.render

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Smoothie.client
import java.awt.Color

@Load
object Render2D {
    @JvmStatic
    @JvmOverloads
    fun GuiGraphics.sizedText(
        text: String,
        color: Int = 0xFFFFFFFF.toInt(),
        shadow: Boolean = true,
        lineSpacing: Int = 2,
        center: List<Int> = emptyList()
    ): Pair<Int, Int> {
        val lines = text.split("\n")
        val widths = lines.map { client.font.width(it) }
        val maxWidth = widths.maxOrNull() ?: 0

        for ((i, l) in lines.withIndex()) {
            val y = i * (client.font.lineHeight + lineSpacing)
            val x = if (i in center) (maxWidth - widths[i]) / 2 else 0
            drawString(client.font, l, x, y, color, shadow)
        }

        val totalHeight = lines.size * client.font.lineHeight + (lines.size - 1) * lineSpacing
        return maxWidth to totalHeight
    }

    @JvmStatic
    @JvmOverloads
    fun GuiGraphics.sizedText(
        components: List<Component>,
        color: Int = 0xFFFFFFFF.toInt(),
        shadow: Boolean = true,
        lineSpacing: Int = 2,
        center: List<Int> = emptyList()
    ): Pair<Int, Int> {
        val widths = components.map { client.font.width(it) }
        val maxWidth = widths.maxOrNull() ?: 0

        for ((i, comp) in components.withIndex()) {
            val y = i * (client.font.lineHeight + lineSpacing)
            val x = if (i in center) (maxWidth - widths[i]) / 2 else 0
            drawString(client.font, comp, x, y, color, shadow)
        }

        val totalHeight = components.size * client.font.lineHeight + (components.size - 1) * lineSpacing
        return maxWidth to totalHeight
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("drawRectangleColor")
    fun GuiGraphics.drawRectangle(x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        fill(x, y, x + width, y + height, color.rgb)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("drawRectangleInt")
    fun GuiGraphics.drawRectangle(x: Int, y: Int, width: Int, height: Int, color: Int = TextColor.WHITE) {
        fill(x, y, x + width, y + height, color)
    }
}