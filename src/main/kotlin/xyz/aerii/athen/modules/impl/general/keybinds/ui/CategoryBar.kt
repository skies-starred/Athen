package xyz.aerii.athen.modules.impl.general.keybinds.ui

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds
import xyz.aerii.athen.ui.InputField
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.client

class CategoryBar(
    private val sidebarW: Int,
    private val rowH: Int
) {
    var selected: String? = null
    var creating = false
    var deleting: String? = null
    val nameField = InputField("Name")
    var scrollOffset = 0

    var tooltipText: String? = null
    var tooltipColor = 0
    var tooltipX = 0
    var tooltipY = 0

    fun draw(guiGraphics: GuiGraphics, mx: Int, my: Int, sx: Int, sy: Int, sh: Int, modalOpen: Boolean, zones: MutableList<UIZone>) {
        guiGraphics.drawRectangle(sx, sy, sidebarW, sh, Mocha.Base.argb)
        guiGraphics.drawOutline(sx, sy, sidebarW, sh, 1, Mocha.Surface0.argb)

        val cats = Keybinds.categories.value
        val lx = sx + 4
        val lw = sidebarW - 8
        scrollOffset = scrollOffset.coerceIn(-maxOf(0, (cats.size + 1) * rowH + 18 - (sh - 8)), 0)

        guiGraphics.enableScissor(sx + 1, sy + 1, sx + sidebarW - 1, sy + sh - 1)

        var cy = sy + 4 + scrollOffset
        tooltipText = null

        val allSelected = selected == null
        val allHov = !modalOpen && mx in lx until lx + lw && my in cy until cy + rowH
        if (allSelected) guiGraphics.drawRectangle(lx, cy, lw, rowH, Mocha.Surface0.argb)
        else if (allHov) guiGraphics.drawRectangle(lx, cy, lw, rowH, Mocha.Surface0.withAlpha(0.5f))
        guiGraphics.text("All", lx + 4, cy + (rowH - client.font.lineHeight) / 2 + 1, false, if (allSelected) Mocha.Mauve.argb else Mocha.Subtext0.argb)
        zones.add(UIZone(lx, cy, lw, rowH, UIZoneType.CATEGORY_TAB, category = ""))
        cy += rowH

        for ((i, cat) in cats.withIndex()) {
            val hov = !modalOpen && mx in lx until lx + lw && my in cy until cy + rowH

            if (deleting == cat.name) {
                guiGraphics.drawRectangle(lx + 1, cy + 1, lw - 2, rowH - 2, Mocha.Red.withAlpha(0.15f))
                guiGraphics.drawOutline(lx + 1, cy + 1, lw - 2, rowH - 2, 1, Mocha.Red.argb)
                guiGraphics.enableScissor(lx + 4, cy, lx + lw - 18, cy + rowH)
                guiGraphics.text(cat.name, lx + 4, cy + (rowH - client.font.lineHeight) / 2 + 1, false, Mocha.Red.argb)
                guiGraphics.disableScissor()

                if (hov) {
                    tooltipText = "Left click to confirm"
                    tooltipColor = Mocha.Red.argb
                    tooltipX = lx + lw + 4
                    tooltipY = cy
                }
            } else {
                if (selected == cat.name) guiGraphics.drawRectangle(lx, cy, lw, rowH, Mocha.Surface0.argb)
                else if (hov) guiGraphics.drawRectangle(lx, cy, lw, rowH, Mocha.Surface0.withAlpha(0.5f))

                if (hov) {
                    tooltipText = "Right click to delete"
                    tooltipColor = Mocha.Subtext0.argb
                    tooltipX = lx + lw + 4
                    tooltipY = cy
                }

                guiGraphics.enableScissor(lx + 4, cy, lx + lw - 18, cy + rowH)
                guiGraphics.text(cat.name, lx + 4, cy + (rowH - client.font.lineHeight) / 2 + 1, false, when {
                    !cat.enabled -> Mocha.Overlay0.argb
                    selected == cat.name -> Mocha.Mauve.argb
                    else -> Mocha.Subtext0.argb
                })
                guiGraphics.disableScissor()
            }

            val tx = lx + lw - 14
            val ty = cy + (rowH - 10) / 2
            val tHov = !modalOpen && mx in tx until tx + 10 && my in ty until ty + 10
            guiGraphics.drawRectangle(tx, ty, 10, 10, if (tHov) Mocha.Surface2.argb else Mocha.Mantle.argb)
            guiGraphics.drawOutline(tx, ty, 10, 10, 1, if (cat.enabled) Mocha.Green.argb else Mocha.Overlay0.argb)
            if (cat.enabled) guiGraphics.drawRectangle(tx + 2, ty + 2, 6, 6, Mocha.Green.argb)

            zones.add(UIZone(tx, ty, 10, 10, UIZoneType.CATEGORY_TOGGLE, i))
            zones.add(UIZone(lx, cy, lw - 18, rowH, UIZoneType.CATEGORY_TAB, i, category = cat.name))
            cy += rowH
        }

        if (creating) {
            nameField.draw(guiGraphics, mx, my, lx, cy + 2, lw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, UIZoneType.CATEGORY_ADD)) }
        } else {
            val addHov = !modalOpen && mx in lx until lx + lw && my in cy + 2 until cy + 16
            if (addHov) guiGraphics.drawRectangle(lx, cy + 2, lw, 14, Mocha.Surface0.withAlpha(0.5f))
            guiGraphics.text("+", lx + (lw - client.font.width("+")) / 2, cy + 2 + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Overlay0.argb)
            zones.add(UIZone(lx, cy + 2, lw, 14, UIZoneType.CATEGORY_ADD))
        }

        guiGraphics.disableScissor()
    }

    fun drawTooltip(guiGraphics: GuiGraphics) {
        val tip = tooltipText ?: return
        val tw = client.font.width(tip)

        guiGraphics.drawRectangle(tooltipX, tooltipY, tw + 8, client.font.lineHeight + 6, Mocha.Base.argb)
        guiGraphics.drawOutline(tooltipX, tooltipY, tw + 8, client.font.lineHeight + 6, 1, Mocha.Overlay0.argb)
        guiGraphics.text(tip, tooltipX + 4, tooltipY + 3, false, tooltipColor)
    }

    fun handleScroll(amount: Int, sh: Int) {
        val contentH = (Keybinds.categories.value.size + 1) * rowH + 18
        scrollOffset = (scrollOffset + amount).coerceIn(-maxOf(0, contentH - (sh - 8)), 0)
    }

    fun confirmCreate() {
        val name = nameField.value.trim()
        if (name.isNotEmpty()) Keybinds.addCategory(name)
        nameField.value = ""
        nameField.cursor = 0
        nameField.selectionStart = -1
        nameField.focused = false
        creating = false
    }

    fun cancelCreate() {
        nameField.value = ""
        nameField.cursor = 0
        nameField.selectionStart = -1
        nameField.focused = false
        creating = false
    }

    fun startCreate() {
        creating = true
        deleting = null
        nameField.value = ""
        nameField.cursor = 0
        nameField.selectionStart = -1
        nameField.scrollOffset = 0
        nameField.focused = true
    }
}
