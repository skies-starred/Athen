package xyz.aerii.athen.utils.render

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import xyz.aerii.athen.annotations.Load
import xyz.aerii.library.api.client
import java.awt.Color
import kotlin.math.*

@Load
object Render2D {
    // <editor-fold desc = "Text rendering">
    @JvmStatic
    @JvmOverloads
    @JvmName("text_string")
    fun GuiGraphics.text(text: String, x: Int, y: Int, shadow: Boolean = true, color: Int = -1, center: Boolean = false) {
        val xx = if (center) x - client.font.width(text) / 2 else x
        drawString(client.font, text, xx, y, color, shadow)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("text_component")
    fun GuiGraphics.text(text: Component, x: Int, y: Int, shadow: Boolean = true, color: Int = -1, center: Boolean = false) {
        val xx = if (center) x - client.font.width(text) / 2 else x
        drawString(client.font, text, xx, y, color, shadow)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("text_fcs")
    fun GuiGraphics.text(text: FormattedCharSequence, x: Int, y: Int, shadow: Boolean = true, color: Int = -1, center: Boolean = false) {
        val xx = if (center) x - client.font.width(text) / 2 else x
        drawString(client.font, text, xx, y, color, shadow)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("text_string_multi")
    fun GuiGraphics.text(texts: List<String>, x: Int, y: Int, shadow: Boolean = true, color: Int = -1, spacing: Int = 2, center: List<Int> = emptyList()) {
        text(texts.map { Language.getInstance().getVisualOrder(FormattedText.of(it)) }, x, y, shadow, color, spacing, center)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("text_component_multi")
    fun GuiGraphics.text(texts: List<Component>, x: Int, y: Int, shadow: Boolean = true, color: Int = -1, spacing: Int = 2, center: List<Int> = emptyList()) {
        text(texts.map { it.visualOrderText }, x, y, shadow, color, spacing, center)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("text_fcs_multi")
    fun GuiGraphics.text(texts: List<FormattedCharSequence>, x: Int, y: Int, shadow: Boolean = true, color: Int = -1, spacing: Int = 2, center: List<Int> = emptyList()) {
        val widths = texts.map { client.font.width(it) }
        drawLines(texts, widths, x, y, color, shadow, spacing, center)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("sizedText_string")
    fun GuiGraphics.sizedText(text: String, shadow: Boolean = true, color: Int = -1, center: Boolean = false): Pair<Int, Int> {
        text(text, 0, 0, shadow, color, center)
        return client.font.width(text) to client.font.lineHeight
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("sizedText_component")
    fun GuiGraphics.sizedText(text: Component, shadow: Boolean = true, color: Int = -1, center: Boolean = false): Pair<Int, Int> {
        text(text, 0, 0, shadow, color, center)
        return client.font.width(text) to client.font.lineHeight
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("sizedText_fcs")
    fun GuiGraphics.sizedText(text: FormattedCharSequence, shadow: Boolean = true, color: Int = -1, centered: Boolean = false): Pair<Int, Int> {
        text(text, 0, 0, shadow, color, centered)
        return client.font.width(text) to client.font.lineHeight
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("sizedText_string_multi")
    fun GuiGraphics.sizedText(texts: List<String>, shadow: Boolean = true, color: Int = -1, spacing: Int = 2, center: List<Int> = emptyList()): Pair<Int, Int> {
        return sizedText(texts.map { Language.getInstance().getVisualOrder(FormattedText.of(it)) }, shadow, color, spacing, center)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("sizedText_component_multi")
    fun GuiGraphics.sizedText(texts: List<Component>, shadow: Boolean = true, color: Int = -1, spacing: Int = 2, center: List<Int> = emptyList()): Pair<Int, Int> {
        return sizedText(texts.map { it.visualOrderText }, shadow, color, spacing, center)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("sizedText_fcs_multi")
    fun GuiGraphics.sizedText(texts: List<FormattedCharSequence>, shadow: Boolean = true, color: Int = -1, spacing: Int = 2, center: List<Int> = emptyList()): Pair<Int, Int> {
        val widths = texts.map { client.font.width(it) }

        drawLines(texts, widths, 0, 0, color, shadow, spacing, center)

        val h = texts.size * client.font.lineHeight + (texts.size - 1) * spacing
        return (widths.maxOrNull() ?: 0) to h
    }
    // </editor-fold>

    @JvmStatic
    @JvmOverloads
    @JvmName("drawRectangle_color")
    fun GuiGraphics.drawRectangle(x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        fill(x, y, x + width, y + height, color.rgb)
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("drawRectangle_int")
    fun GuiGraphics.drawRectangle(x: Int, y: Int, width: Int, height: Int, color: Int = -1) {
        fill(x, y, x + width, y + height, color)
    }

    @JvmStatic
    @JvmOverloads
    fun GuiGraphics.drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Int, thickness: Int = 1) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt((dx * dx + dy * dy).toDouble())
        if (length <= 0f) return

        val pose = pose()
        pose.pushMatrix()

        pose.translate(x1.toFloat(), y1.toFloat())
        pose.rotate(atan2(dy.toFloat(), dx.toFloat()))

        if (thickness == 1) {
            fill(0, 0, length.toInt(), 1, color)
            pose.popMatrix()
            return
        }

        val half = thickness / 2f
        fill(0, floor(-half).toInt(), length.toInt(), ceil(half).toInt(), color)

        pose.popMatrix()
    }

    @JvmStatic
    @JvmOverloads
    fun GuiGraphics.drawOutline(x: Int, y: Int, width: Int, height: Int, border: Int, color: Int = -1, inset: Boolean = false) {
        val border = if (inset) -border else border
        fill(x - border, y - border, x + width + border, y, color)
        fill(x - border, y + height, x + width + border, y + height + border, color)
        fill(x - border, y, x, y + height, color)
        fill(x + width, y, x + width + border, y + height, color)
    }

    private fun GuiGraphics.drawLines(lines: List<FormattedCharSequence>, widths: List<Int>, x: Int, y: Int, color: Int, shadow: Boolean, spacing: Int, center: List<Int>) {
        val max = widths.maxOrNull() ?: 0
        for (i in lines.indices) {
            val xx = if (i in center) x + (max - widths[i]) / 2 else x
            val yy = y + i * (client.font.lineHeight + spacing)
            drawString(client.font, lines[i], xx, yy, color, shadow)
        }
    }
}