package xyz.aerii.athen.modules.impl.general.keybinds.ui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.client

class BindingsListRenderer(
    private val entryH: Int,
    private val entrySpacing: Int,
    private val fh: Int,
    private val padding: Int
) {
    private var scrollOffset = 0
    private var maxScroll = 0

    fun draw(guiGraphics: GuiGraphics, mx: Int, my: Int, listX: Int, listY: Int, listW: Int, listH: Int, entries: List<BindingEntry>, modalOpen: Boolean, zones: MutableList<UIZone>) {
        if (entries.isEmpty()) return guiGraphics.text("No keybinds", listX + (listW - client.font.width("No keybinds")) / 2, listY + listH / 2, false, Mocha.Subtext0.argb)

        maxScroll = maxOf(0, entries.size * (entryH + entrySpacing) - entrySpacing - listH)
        scrollOffset = scrollOffset.coerceIn(-maxScroll, 0)

        guiGraphics.enableScissor(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2)

        var cy = listY + scrollOffset
        for (e in entries) {
            if (cy + entryH > listY - 5 && cy < listY + listH + 5) drawEntry(guiGraphics, mx, my, listX, cy, listW, e, modalOpen, zones)
            cy += entryH + entrySpacing
        }

        guiGraphics.disableScissor()
    }

    private fun drawEntry(guiGraphics: GuiGraphics, mx: Int, my: Int, x: Int, y: Int, w: Int, entry: BindingEntry, open: Boolean, zones: MutableList<UIZone>) {
        val hovered = !open && mx in x until x + w && my in y until y + entryH
        val en = entry.binding.enabled && (entry.binding.category.isEmpty() || Keybinds.categories.value.find { it.name == entry.binding.category }?.enabled != false)

        guiGraphics.drawRectangle(x, y, w, entryH, if (hovered) Mocha.Surface1.argb else if (!en) Mocha.Red.withAlpha(0.15f) else Mocha.Surface0.argb)
        guiGraphics.drawOutline(x, y, w, entryH, 1, if (!en) Mocha.Red.withAlpha(0.6f) else Mocha.Overlay0.argb)

        var cx = x + padding
        val toggleY = y + (entryH - 14) / 2
        val tHov = !open && mx in cx until cx + 14 && my in toggleY until toggleY + 14

        guiGraphics.drawRectangle(cx, toggleY, 14, 14, if (tHov) Mocha.Surface2.argb else Mocha.Base.argb)
        guiGraphics.drawOutline(cx, toggleY, 14, 14, 1, if (en) Mocha.Green.argb else Mocha.Overlay0.argb)
        if (entry.toggleAnim > 0.05f) guiGraphics.drawRectangle(cx + 3, toggleY + 3, 8, 8, Mocha.Green.withAlpha(entry.toggleAnim))
        zones.add(UIZone(cx, toggleY, 14, 14, UIZoneType.ENTRY_TOGGLE, entry.index))
        cx += 14 + padding

        val kStr = entry.binding.keys.str()
        val kw = client.font.width(kStr) + 8
        guiGraphics.drawRectangle(cx, y + (entryH - fh) / 2, kw, fh, Mocha.Surface2.argb)
        guiGraphics.drawOutline(cx, y + (entryH - fh) / 2, kw, fh, 1, Mocha.Crust.argb)
        guiGraphics.text(kStr, cx + 4, y + (entryH - client.font.lineHeight) / 2 + 1, false, Mocha.Text.argb)
        cx += kw + padding

        guiGraphics.enableScissor(cx, y, cx + (w - (cx - x) - 60 - padding * 2), y + entryH)
        guiGraphics.text(entry.binding.command, cx, y + (entryH - client.font.lineHeight) / 2 + 1, false, if (en) Mocha.Text.argb else Mocha.Red.argb)
        guiGraphics.disableScissor()

        val ex = x + w - 60 - padding * 2
        val eHov = !open && mx in ex until ex + 40 && my in y + (entryH - fh) / 2 until y + (entryH - fh) / 2 + fh
        guiGraphics.drawRectangle(ex, y + (entryH - fh) / 2, 40, fh, if (eHov) Mocha.Surface2.argb else Mocha.Base.argb)
        guiGraphics.drawOutline(ex, y + (entryH - fh) / 2, 40, fh, 1, Mocha.Overlay0.argb)
        guiGraphics.text("Edit", ex + (40 - client.font.width("Edit")) / 2, y + (entryH - client.font.lineHeight) / 2 + 1, false, Mocha.Text.argb)
        zones.add(UIZone(ex, y + (entryH - fh) / 2, 40, fh, UIZoneType.ENTRY_EDIT, entry.index))

        val dx = x + w - 20 - padding
        val dHov = !open && mx in dx until dx + 20 && my in y + (entryH - fh) / 2 until y + (entryH - fh) / 2 + fh
        guiGraphics.drawRectangle(dx, y + (entryH - fh) / 2, 20, fh, if (dHov) Mocha.Red.argb else Mocha.Base.argb)
        guiGraphics.drawOutline(dx, y + (entryH - fh) / 2, 20, fh, 1, Mocha.Overlay0.argb)
        guiGraphics.text("×", dx + (21 - client.font.width("×")) / 2, y + (entryH - client.font.lineHeight) / 2 + 1, false, Mocha.Text.argb)
        zones.add(UIZone(dx, y + (entryH - fh) / 2, 20, fh, UIZoneType.ENTRY_DELETE, entry.index))
    }

    fun handleScroll(amount: Int) {
        scrollOffset = (scrollOffset + amount).coerceIn(-maxScroll, 0)
    }

    companion object {
        fun List<Int>.str(): String = if (isEmpty()) "None" else joinToString(" + ") { it.str() }

        fun Int.str(): String = when (this) {
            -1 -> "LMB"
            -2 -> "RMB"
            -3 -> "MMB"
            in Int.MIN_VALUE..-4 -> "M${-this - 1}"
            else -> InputConstants.Type.KEYSYM.getOrCreate(this).displayName.string.let {
                if (it.length == 1) it.uppercase() else it
            }
        }
    }
}
