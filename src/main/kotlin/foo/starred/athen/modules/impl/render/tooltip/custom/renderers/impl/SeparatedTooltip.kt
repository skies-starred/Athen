package foo.starred.athen.modules.impl.render.tooltip.custom.renderers.impl

import foo.starred.athen.api.rendering.ui.effects.outline.outline
import foo.starred.athen.api.rendering.ui.shapes.rectangle.rectangle
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.extractText
import foo.starred.athen.modules.impl.render.tooltip.custom.CustomTooltip
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.base.ITooltipRenderer
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.base.TooltipContext
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.util.FormattedCharSequence

object SeparatedTooltip : ITooltipRenderer {
    override fun TooltipContext.render() {
        val w = width + 8
        val bw = CustomTooltip.`border$width`

        val header = components[0]
        val headerH = header.getHeight(font) + 6

        val body = components.drop(1).let { if ((it.firstOrNull() as? ClientTextTooltip)?.text?.equals(FormattedCharSequence.EMPTY) == true) it.drop(1) else it }
        val bh0 = body.sumOf { it.getHeight(font) }
        val th = headerH + 4 + bh0 + 8

        val hy = if (th < screenHeight - 40) (y - 4).coerceIn(20, screenHeight - 20 - th) else 20
        val hx = x - 4
        val dy = hy + 4

        graphics.box(hx, hy, w, headerH, bw)

        val text = (header as? ClientTextTooltip)?.text
        if (text != null) {
            val textX = if (CustomTooltip.`header$centered`) (x + (w - 8) / 2) - font.width(text) / 2 else x
            graphics.extractText(text, textX, dy, CustomTooltip.`text$shadow`)
        }

        header.extractImage(font, x, dy, width, height, graphics)

        val bx = x - 4
        val by = hy + headerH + 4
        val mbh = minOf(bh0 + 6, screenHeight - 20 - by).coerceAtLeast(0)

        val sy = CustomTooltip.scroll(bh0, mbh - 6)
        val bh = if (CustomTooltip.`scroll$infinite`) (if (sy > 0) mbh - sy else bh0 + sy + 6).coerceIn(0, screenHeight - 20 - by) else mbh

        graphics.box(bx, by, w, bh, bw)
        graphics.components(font, body, x, bx, by, w, bh, by + 4 + sy.coerceAtMost(0), width, bh0)
        graphics.fade(bx, by, w, bh, sy, bh0)
    }

    private fun GuiGraphicsExtractor.box(x: Int, y: Int, w: Int, h: Int, bw: Int) {
        if (CustomTooltip.background) rectangle(x, y, w, h, CustomTooltip.`background$color`)
        if (CustomTooltip.border && bw > 0) outline(x, y, w, h, bw, if (CustomTooltip.`border$rarity`) CustomTooltip.color else CustomTooltip.`border$color`)
    }

    private fun GuiGraphicsExtractor.components(font: Font, comps: List<ClientTooltipComponent>, tx: Int, boxX: Int, boxY: Int, boxW: Int, boxH: Int, startY: Int, width: Int, totalHeight: Int) {
        enableScissor(boxX, boxY, boxX + boxW, boxY + boxH)

        var drawY = startY
        for (c in comps) {
            c.extractText(this, font, tx, drawY)
            drawY += c.getHeight(font)
        }

        drawY = startY
        for (c in comps) {
            c.extractImage(font, tx, drawY, width, totalHeight, this)
            drawY += c.getHeight(font)
        }

        disableScissor()
    }

    private fun GuiGraphicsExtractor.fade(x: Int, y: Int, w: Int, h: Int, scrollY: Int, contentHeight: Int) {
        val bg = CustomTooltip.`background$color` or 0xFF000000.toInt()
        val bgT = bg and 0x00FFFFFF

        enableScissor(x, y, x + w, y + h)
        if (scrollY < 0) fillGradient(x, y, x + w, y + 18, bg, bgT)
        if (scrollY > 0 || contentHeight + scrollY > h) fillGradient(x, y + h - 18, x + w, y + h, bgT, bg)
        disableScissor()
    }
}
