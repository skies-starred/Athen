package xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.impl

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import xyz.aerii.athen.modules.impl.render.tooltip.custom.CustomTooltip
import xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.base.ITooltipRenderer
import xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.base.TooltipContext
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle

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

    private fun GuiGraphics.box(x: Int, y: Int, w: Int, h: Int, bw: Int) {
        if (CustomTooltip.background) drawRectangle(x, y, w, h, CustomTooltip.`background$color`.rgb)
        if (CustomTooltip.border && bw > 0) drawOutline(x, y, w, h, bw, if (CustomTooltip.`border$rarity`) CustomTooltip.color else CustomTooltip.`border$color`.rgb)
    }

    private fun GuiGraphics.component(font: Font, comps: List<ClientTooltipComponent>, tx: Int, boxX: Int, boxY: Int, boxW: Int, boxH: Int, startY: Int, width: Int, totalHeight: Int) {
        enableScissor(boxX, boxY, boxX + boxW, boxY + boxH)
        val l = comps.withIndex()

        var drawY = startY
        for ((i, c) in l) {
            c.renderText(this, font, tx, drawY)
            drawY += c.getHeight(font) + if (i == 0) 2 else 0
        }

        drawY = startY
        for ((i, c) in l) {
            c.renderImage(font, tx, drawY, width, totalHeight, this)
            drawY += c.getHeight(font) + if (i == 0) 2 else 0
        }

        disableScissor()
    }

    private fun GuiGraphics.fade(x: Int, y: Int, w: Int, h: Int, scrollY: Int, contentHeight: Int) {
        val bg = CustomTooltip.`background$color`.rgb or 0xFF000000.toInt()
        val bgT = bg and 0x00FFFFFF

        enableScissor(x, y, x + w, y + h)
        if (scrollY < 0) fillGradient(x, y, x + w, y + 18, bg, bgT)
        if (scrollY > 0 || contentHeight + scrollY > h) fillGradient(x, y + h - 18, x + w, y + h, bgT, bg)
        disableScissor()
    }
}