package xyz.aerii.athen.modules.impl.general.messageactions.ui.actions

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.modules.impl.general.messageactions.data.MatchType
import xyz.aerii.athen.modules.impl.general.messageactions.MessageActions
import xyz.aerii.athen.modules.impl.general.messageactions.actions.IMessageAction
import xyz.aerii.athen.modules.impl.general.messageactions.ui.UIZoneType
import xyz.aerii.athen.ui.InputField
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.client

class ActionModalRenderer(
    private val mw: Int,
    private val mh: Int,
    private val fh: Int,
    private val padding: Int
) {
    val patternField = InputField("Pattern to match")
    val valueField = InputField("Action value")
    val delayField = InputField("0")
    var open = false
    var entry: ActionEntryView? = null
    var match = MatchType.CONTAINS
    var action = 0
    var cancel = false
    var category = ""

    var matchOpen = false
    var matchY = 0

    var catOpen = false
    var catY = 0

    val opened: Boolean
        get() = matchOpen || catOpen

    fun draw(graphics: GuiGraphics, mx: Int, my: Int, sw: Int, sh: Int, zones: MutableList<UIZone>) {
        graphics.drawRectangle(0, 0, sw, sh, Mocha.Crust.withAlpha(0.6f))
        val x0 = (sw - mw) / 2
        val y0 = (sh - mh) / 2
        val w = mw - padding * 2

        graphics.drawRectangle(x0, y0, mw, mh, Mocha.Base.argb)
        graphics.drawOutline(x0, y0, mw, mh, 1, Mocha.Surface0.argb)

        graphics.text(if (entry == null) "Create Action" else "Edit Action", x0 + padding, y0 + padding + 2, false, Mocha.Mauve.argb)
        graphics.drawRectangle(x0, y0 + 24, mw, 1, Mocha.Surface0.argb)

        var cy = y0 + 34
        val hw = w / 2 - 4

        graphics.text("Pattern", x0 + padding, cy, false, Mocha.Subtext0.argb)
        graphics.text("Match Type", x0 + padding + hw + 8, cy, false, Mocha.Subtext0.argb)
        cy += client.font.lineHeight + 2

        patternField.draw(graphics, mx, my, x0 + padding, cy, hw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, UIZoneType.MODAL_PATTERN)) }
        matchY = cy
        dropdown0(graphics, mx, my, x0 + padding + hw + 8, cy, hw, zones)
        cy += fh + 8

        graphics.text("Action", x0 + padding, cy, false, Mocha.Subtext0.argb)
        cy += client.font.lineHeight + 2

        val actions = IMessageAction.all()
        val aw = (w - (actions.size - 1) * 4) / actions.size
        for (a in actions) {
            val i = a.id
            val idx = actions.indexOf(a)
            val ax = x0 + padding + idx * (aw + 4)
            val selected = action == i
            val hovered = !opened && mx in ax until ax + aw && my in cy until cy + 14

            graphics.drawRectangle(ax, cy, aw, 14, if (selected) Mocha.Mauve.argb else if (hovered) Mocha.Surface2.argb else Mocha.Surface1.argb)
            graphics.drawOutline(ax, cy, aw, 14, 1, if (selected) Mocha.Mauve.argb else Mocha.Overlay0.argb)
            graphics.enableScissor(ax + 2, cy, ax + aw - 2, cy + 14)
            graphics.text(a.name, ax + (aw - client.font.width(a.name)) / 2, cy + (14 - client.font.lineHeight) / 2 + 1, false, if (selected) Mocha.Base.argb else Mocha.Text.argb)
            graphics.disableScissor()
            zones.add(UIZone(ax, cy, aw, 14, UIZoneType.MODAL_ACTION_TYPE, a.id))
        }
        cy += 14 + 8

        graphics.text(if (action == 0) "Value" else IMessageAction.all().firstOrNull { it.id == action }?.name ?: "Value", x0 + padding, cy, false, if (action == 0) Mocha.Overlay0.argb else Mocha.Subtext0.argb)
        graphics.text("Category", x0 + padding + hw + 8, cy, false, Mocha.Subtext0.argb)
        cy += client.font.lineHeight + 2

        if (action == 0) {
            graphics.drawRectangle(x0 + padding, cy, hw, fh, Mocha.Crust.argb)
            graphics.drawOutline(x0 + padding, cy, hw, fh, 1, Mocha.Surface0.argb)
        } else {
            valueField.draw(graphics, mx, my, x0 + padding, cy, hw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, UIZoneType.MODAL_ACTION_VALUE)) }
        }

        catY = cy
        dropdown1(graphics, mx, my, x0 + padding + hw + 8, cy, hw, zones)
        cy += fh + 8

        val c = "Cancel message"
        val c0 = client.font.width(c)
        val cx = x0 + padding
        val cy0 = cy + (fh - 14) / 2
        val ch = !opened && mx in cx until cx + 14 && my in cy0 until cy0 + 14

        graphics.drawRectangle(cx, cy0, 14, 14, if (ch) Mocha.Surface2.argb else Mocha.Base.argb)
        graphics.drawOutline(cx, cy0, 14, 14, 1, if (cancel) Mocha.Red.argb else Mocha.Overlay0.argb)

        if (cancel) graphics.drawRectangle(cx + 3, cy0 + 3, 8, 8, Mocha.Red.argb)
        graphics.text(c, cx + 18, cy0 + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Subtext0.argb)
        zones.add(UIZone(cx, cy0, 14 + 4 + c0, 14, UIZoneType.MODAL_CANCEL_TOGGLE))

        graphics.text("Delay (s)", x0 + padding + hw + 8, cy + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Subtext0.argb)
        delayField.draw(graphics, mx, my, x0 + padding + hw + 8 + client.font.width("Delay (s)") + 4, cy, hw - client.font.width("Delay (s)") - 4) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, UIZoneType.MODAL_DELAY)) }

        val y1 = y0 + mh - fh - padding
        val x1 = x0 + padding
        val x2 = x1 + hw + 8

        graphics.drawRectangle(x0 + padding, y1 - 8, w, 1, Mocha.Surface0.argb)

        graphics.drawRectangle(x2, y1, hw, fh, if (!opened && mx in x2 until x2 + hw && my in y1 until y1 + fh) Mocha.Surface2.argb else Mocha.Surface1.argb)
        graphics.drawOutline(x2, y1, hw, fh, 1, Mocha.Green.argb)
        graphics.text("Save", x2 + (hw - client.font.width("Save")) / 2, y1 + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Green.argb)
        zones.add(UIZone(x2, y1, hw, fh, UIZoneType.MODAL_SAVE))

        graphics.drawRectangle(x1, y1, hw, fh, if (!opened && mx in x1 until x1 + hw && my in y1 until y1 + fh) Mocha.Surface2.argb else Mocha.Surface1.argb)
        graphics.drawOutline(x1, y1, hw, fh, 1, Mocha.Red.argb)
        graphics.text("Cancel", x1 + (hw - client.font.width("Cancel")) / 2, y1 + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Red.argb)
        zones.add(UIZone(x1, y1, hw, fh, UIZoneType.MODAL_CANCEL))

        graphics.text("Regex: use $0 for full message, and $1, $2, $3... for groups", x1, y1 - 8 - client.font.lineHeight - 2, false, Mocha.Overlay0.argb)

        if (matchOpen) matchType(graphics, mx, my, x0 + padding + hw + 8, matchY + fh, hw)
        if (catOpen) category(graphics, mx, my, x0 + padding + hw + 8, catY + fh, hw)
    }

    private fun dropdown0(graphics: GuiGraphics, mx: Int, my: Int, x: Int, y: Int, w: Int, zones: MutableList<UIZone>) {
        val hov = (!opened || matchOpen) && mx in x until x + w && my in y until y + fh
        graphics.drawRectangle(x, y, w, fh, if (hov) Mocha.Surface2.argb else Mocha.Surface1.argb)
        graphics.drawOutline(x, y, w, fh, 1, if (matchOpen) Mocha.Mauve.argb else Mocha.Overlay0.argb)
        graphics.text(match.displayName, x + 4, y + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Text.argb)
        graphics.text(if (matchOpen) "▾" else "▸", x + w - client.font.width(if (matchOpen) "▾" else "▸") - 4, y + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Overlay0.argb)
        zones.add(UIZone(x, y, w, fh, UIZoneType.MODAL_MATCH_TYPE))
    }

    private fun matchType(graphics: GuiGraphics, mx: Int, my: Int, x: Int, y: Int, w: Int) {
        val entries = MatchType.entries
        val menuH = entries.size * 14
        graphics.drawRectangle(x, y, w, menuH, Mocha.Base.argb)
        graphics.drawOutline(x, y, w, menuH, 1, Mocha.Mauve.argb)

        var cy = y
        for (e in entries) {
            if (mx in x until x + w && my in cy until cy + 14) graphics.drawRectangle(x, cy, w, 14, Mocha.Surface1.argb)

            val sel = match == e
            graphics.text(e.displayName, x + 4, cy + (14 - client.font.lineHeight) / 2 + 1, false, if (sel) Mocha.Mauve.argb else Mocha.Text.argb)
            if (sel) graphics.text("✔", x + w - 12, cy + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Mauve.argb)
            cy += 14
        }
    }

    private fun dropdown1(graphics: GuiGraphics, mx: Int, my: Int, x: Int, y: Int, w: Int, zones: MutableList<UIZone>) {
        val hov = (!opened || catOpen) && mx in x until x + w && my in y until y + fh
        graphics.drawRectangle(x, y, w, fh, if (hov) Mocha.Surface2.argb else Mocha.Surface1.argb)
        graphics.drawOutline(x, y, w, fh, 1, if (catOpen) Mocha.Mauve.argb else Mocha.Overlay0.argb)
        graphics.enableScissor(x + 2, y, x + w - 14, y + fh)
        graphics.text(category.ifEmpty { "Uncategorized" }, x + 4, y + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Text.argb)
        graphics.disableScissor()
        graphics.text(if (catOpen) "▾" else "▸", x + w - client.font.width(if (catOpen) "▾" else "▸") - 4, y + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Overlay0.argb)
        zones.add(UIZone(x, y, w, fh, UIZoneType.MODAL_CATEGORY))
    }

    private fun category(graphics: GuiGraphics, mx: Int, my: Int, x: Int, y: Int, w: Int) {
        val cats = MessageActions.categories
        val menuH = ((cats.size + 1) * 14).coerceAtMost(80)
        graphics.drawRectangle(x, y, w, menuH, Mocha.Base.argb)
        graphics.drawOutline(x, y, w, menuH, 1, Mocha.Mauve.argb)
        graphics.enableScissor(x, y, x + w, y + menuH)

        var cy = y
        if (mx in x until x + w && my in cy until cy + 14) graphics.drawRectangle(x, cy, w, 14, Mocha.Surface1.argb)
        graphics.text("Uncategorized", x + 4, cy + (14 - client.font.lineHeight) / 2 + 1, false, if (category.isEmpty()) Mocha.Mauve.argb else Mocha.Text.argb)
        if (category.isEmpty()) graphics.text("✔", x + w - 12, cy + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Mauve.argb)
        cy += 14

        for (cat in cats) {
            if (cy + 14 > y && cy < y + menuH) {
                if (mx in x until x + w && my in cy until cy + 14) graphics.drawRectangle(x, cy, w, 14, Mocha.Surface1.argb)
                val sel = category == cat.name
                graphics.text(cat.name, x + 4, cy + (14 - client.font.lineHeight) / 2 + 1, false, if (sel) Mocha.Mauve.argb else Mocha.Text.argb)
                if (sel) graphics.text("✔", x + w - 12, cy + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Mauve.argb)
            }

            cy += 14
        }

        graphics.disableScissor()
    }

    fun open() {
        entry = null
        reset()
        open = true
    }

    fun open(a: ActionEntryView) {
        entry = a
        patternField.value = a.entry.pattern
        patternField.cursor = patternField.value.length
        patternField.selectionStart = -1
        patternField.scrollOffset = 0
        patternField.focused = true
        match = a.entry.match
        action = a.entry.id
        valueField.value = a.entry.value
        valueField.cursor = valueField.value.length
        valueField.selectionStart = -1
        valueField.scrollOffset = 0
        cancel = a.entry.cancel
        category = a.entry.category
        delayField.value = if (a.entry.delay > 0.0) a.entry.delay.toString() else ""
        delayField.cursor = delayField.value.length
        delayField.selectionStart = -1
        delayField.scrollOffset = 0
        matchOpen = false
        catOpen = false
        open = true
    }

    fun close() {
        open = false
        entry = null
        patternField.focused = false
        valueField.focused = false
        delayField.focused = false
    }

    fun click0(mouseX: Int, mouseY: Int) {
        val sw = client.window.guiScaledWidth
        val mx0 = (sw - mw) / 2
        val fw = mw - padding * 2
        val hw = fw / 2 - 4
        val x0 = mx0 + padding + hw + 8

        if (mouseX !in x0 until x0 + hw) {
            matchOpen = false
            return
        }

        val y0 = matchY + fh
        val e = MatchType.entries
        val h = e.size * 14

        if (mouseY in y0 until y0 + h) {
            var iy = y0
            for (e in e) {
                if (mouseY !in iy until iy + 14) {
                    iy += 14
                    continue
                }

                match = e
                matchOpen = false
                return
            }
        }

        matchOpen = false
    }

    fun click1(mouseX: Int, mouseY: Int) {
        val sw = client.window.guiScaledWidth
        val mx0 = (sw - mw) / 2
        val fw = mw - padding * 2
        val halfW = fw / 2 - 4
        val menuX = mx0 + padding + halfW + 8

        if (mouseX !in menuX until menuX + halfW) {
            catOpen = false
            return
        }

        val y0 = catY + fh
        val e = MessageActions.categories
        val h = ((e.size + 1) * 14).coerceAtMost(80)

        if (mouseY in y0 until y0 + h) {
            var iy = y0
            if (mouseY in iy until iy + 14) {
                category = ""
                catOpen = false
                return
            }

            iy += 14
            for (cat in e) {
                if (mouseY !in iy until iy + 14) {
                    iy += 14
                    continue
                }

                category = cat.name
                catOpen = false
                return
            }
        }

        catOpen = false
    }

    private fun reset() {
        patternField.value = ""
        patternField.cursor = 0
        patternField.selectionStart = -1
        patternField.scrollOffset = 0
        patternField.focused = true
        valueField.value = ""
        valueField.cursor = 0
        valueField.selectionStart = -1
        valueField.scrollOffset = 0
        match = MatchType.CONTAINS
        action = 0
        cancel = false
        category = ""
        delayField.value = ""
        delayField.cursor = 0
        delayField.selectionStart = -1
        delayField.scrollOffset = 0
        matchOpen = false
        catOpen = false
    }
}