package foo.starred.athen.modules.impl.render.tooltip.custom.renderers.impl

import foo.starred.athen.api.rendering.ui.effects.outline.outline
import foo.starred.athen.api.rendering.ui.shapes.rectangle.rectangle
import foo.starred.athen.modules.impl.render.tooltip.custom.CustomTooltip
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.base.ITooltipRenderer
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.base.TooltipContext
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent

object CombinedTooltip : ITooltipRenderer {
    override fun TooltipContext.render() {
        val w = width + 8
        val bw = CustomTooltip.`border$width`
        val x0 = x - 4
        val y0 = if (height + 8 < screenHeight - 40) (y - 4).coerceIn(20, screenHeight - 20 - (height + 8)) else 20
        val dy = y0 + 4
        val mh = minOf(height + 8, screenHeight - 20 - y0).coerceAtLeast(0)

        val sy = CustomTooltip.scroll(height, mh - 8)
        val h = if (CustomTooltip.`scroll$infinite`) (if (sy > 0) mh - sy else height + sy + 8).coerceIn(0, screenHeight - 20 - y0) else mh

        graphics.box(x0, y0, w, h, bw)
        graphics.component(font, components, x, x0, y0, w, h, dy + minOf(0, sy), width, height)
        graphics.fade(x0, y0, w, h, sy, height)
    }

    private fun GuiGraphicsExtractor.box(x: Int, y: Int, w: Int, h: Int, bw: Int) {
        if (CustomTooltip.background) rectangle(x, y, w, h, CustomTooltip.`background$color`)
        if (CustomTooltip.border && bw > 0) outline(x, y, w, h, bw, if (CustomTooltip.`border$rarity`) CustomTooltip.color else CustomTooltip.`border$color`)
    }

    private fun GuiGraphicsExtractor.component(font: Font, comps: List<ClientTooltipComponent>, tx: Int, boxX: Int, boxY: Int, boxW: Int, boxH: Int, startY: Int, width: Int, totalHeight: Int) {
        enableScissor(boxX, boxY, boxX + boxW, boxY + boxH)
        val l = comps.withIndex()

        var drawY = startY
        for ((i, c) in l) {
            c.extractText(this, font, tx, drawY)
            drawY += c.getHeight(font) + if (i == 0) 2 else 0
        }

        drawY = startY
        for ((i, c) in l) {
            c.extractImage(font, tx, drawY, width, totalHeight, this)
            drawY += c.getHeight(font) + if (i == 0) 2 else 0
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
