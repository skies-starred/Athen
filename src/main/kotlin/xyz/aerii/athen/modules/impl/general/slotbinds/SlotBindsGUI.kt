@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.modules.impl.general.slotbinds

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.ui.IZoneType
import xyz.aerii.athen.ui.InputField
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawLine
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.client

object SlotBindsGUI : Scram("Slot Binds Editor [Athen]") {
    private enum class ZoneType : IZoneType {
        PROFILE_TAB,
        PROFILE_ADD,
        PROFILE_NAME,
        SLOT
    }

    private val zones = mutableListOf<UIZone>()
    private val name = InputField("Name")
    private var creating = false
    private var deleting: String? = null
    private var selected: Int? = null

    private var s = 0
    private var ms = 0

    private var tt: String? = null
    private var tc = 0
    private var tx = 0
    private var ty = 0

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun onScramInit() {
        creating = false
        deleting = null
        selected = null
        name.reset()
        s = 0
    }

    override fun onScramClose() {
        SlotBinds.save()
        SlotBinds.disk()
    }

    override fun onScramRender(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        zones.clear()
        tt = null
        graphics.drawRectangle(0, 0, width, height, Mocha.Crust.withAlpha(0.6f))

        val previewW = 210
        val sideW = 110
        val gapW = 6
        val pw = sideW + gapW + previewW
        val ph = 160
        val px = (width - pw) / 2
        val py = (height - ph) / 2

        graphics.profiles(mouseX, mouseY, px, py, ph)
        graphics.preview(mouseX, mouseY, px + sideW + gapW, py, previewW, ph)

        tt?.let { tip ->
            val tw = client.font.width(tip)
            graphics.drawRectangle(tx, ty, tw + 8, client.font.lineHeight + 6, Mocha.Base.argb)
            graphics.drawOutline(tx, ty, tw + 8, client.font.lineHeight + 6, 1, Mocha.Overlay0.argb)
            graphics.text(tip, tx + 4, ty + 4, false, tc)
        }
    }

    private fun GuiGraphics.profiles(mx: Int, my: Int, sx: Int, sy: Int, sh: Int) {
        val sideW = 110
        drawRectangle(sx, sy, sideW, sh, Mocha.Base.argb)
        drawOutline(sx, sy, sideW, sh, 1, Mocha.Surface0.argb)

        val names = SlotBinds.map0.keys
        val lx = sx + 4
        val lw = sideW - 8
        val rowH = 20

        enableScissor(sx + 1, sy + 1, sx + sideW - 1, sy + sh - 1)

        var cy = sy + 4 + s

        for (name in names) {
            val hov = mx in lx until lx + lw && my in cy until cy + rowH
            val selected = name == SlotBinds.active

            if (deleting == name) {
                drawRectangle(lx + 1, cy + 1, lw - 2, rowH - 2, Mocha.Red.withAlpha(0.15f))
                drawOutline(lx + 1, cy + 1, lw - 2, rowH - 2, 1, Mocha.Red.argb)

                enableScissor(lx + 4, cy, lx + lw - 4, cy + rowH)
                text(name, lx + 4, cy + (rowH - client.font.lineHeight) / 2 + 1, false, Mocha.Red.argb)
                disableScissor()

                if (hov) {
                    tt = "Left click to confirm"
                    tc = Mocha.Red.argb
                    tx = lx + lw + 4
                    ty = cy + 2
                }
            } else {
                if (selected) drawRectangle(lx, cy, lw, rowH, Mocha.Surface0.argb)
                else if (hov) drawRectangle(lx, cy, lw, rowH, Mocha.Surface0.withAlpha(0.5f))

                if (hov && !selected) {
                    tt = "Right click to delete"
                    tc = Mocha.Subtext0.argb
                    tx = lx + lw + 4
                    ty = cy + 2
                }

                enableScissor(lx + 4, cy, lx + lw - 4, cy + rowH)
                text(name, lx + 4, cy + (rowH - client.font.lineHeight) / 2 + 1, false, if (selected) Mocha.Mauve.argb else Mocha.Subtext0.argb)
                disableScissor()
            }

            zones.add(UIZone(lx, cy, lw, rowH, ZoneType.PROFILE_TAB, category = name))
            cy += rowH
        }

        if (creating) {
            name.draw(this, mx, my, lx, cy + 2, lw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.PROFILE_NAME)) }
        } else {
            if (mx in lx until lx + lw && my in cy + 2 until cy + 16) drawRectangle(lx, cy + 2, lw, 14, Mocha.Surface0.withAlpha(0.5f))
            text("+", lx + (lw - client.font.width("+")) / 2, cy + 2 + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Overlay0.argb)
            zones.add(UIZone(lx, cy + 2, lw, 14, ZoneType.PROFILE_ADD))
        }

        disableScissor()
        ms = -maxOf(0, (names.size * rowH + 18) - (sh - 8))
        s = s.coerceIn(ms, 0)
    }

    private fun GuiGraphics.preview(mx: Int, my: Int, px: Int, py: Int, pw: Int, ph: Int) {
        drawRectangle(px, py, pw, ph, Mocha.Base.argb)
        drawOutline(px, py, pw, ph, 1, Mocha.Surface0.argb)

        text("Preview", px + 6, py + 6, false, Mocha.Text.argb)
        drawRectangle(px + 6, py + 6 + client.font.lineHeight + 2, pw - 12, 1, Mocha.Surface0.argb)

        val binds = SlotBinds.m0
        for (row in 0 until 3) for (col in 0 until 9) {
            val slot = 9 + row * 9 + col
            val x = px + (pw - 178) / 2 + col * 20
            val y = py + ph - 112 + row * 20
            val bound = fn(slot)
            val bc = if (bound) SlotBinds.sc(slot) else 0
            val bool = slot == selected

            drawRectangle(x, y, 18, 18, if (bool) Mocha.Surface1.argb else if (bound) Mocha.Surface2.argb else Mocha.Surface0.argb)
            drawOutline(x, y, 18, 18, 1, if (bool) Mocha.Lavender.argb else if (bound) bc else Mocha.Overlay0.argb)

            val lbl = slot.toString()
            text(lbl, x + (18 - client.font.width(lbl)) / 2, y + (18 - client.font.lineHeight) / 2 + 1, false, if (bool) Mocha.Lavender.argb else if (bound) bc else Mocha.Subtext0.argb)

            zones.add(UIZone(x, y, 18, 18, ZoneType.SLOT, data = slot))
            if (mx in x until x + 18 && my in y until y + 18) {
                if (bound) {
                    tt = "L: Cycle | R: Remove"
                    tc = bc
                } else if (selected != null && (selected!! in 36..44) != (slot in 36..44)) {
                    tt = "Click to bind"
                    tc = Mocha.Green.argb
                }

                tx = x + 20
                ty = y + 1
            }
        }

        val y = py + ph - 48
        drawRectangle(px + (pw - 178) / 2, y - 4, 178, 1, Mocha.Surface0.argb)

        for (col in 0 until 9) {
            val slot = 36 + col
            val x = px + (pw - 178) / 2 + col * 20
            val bound = fn(slot)
            val bc = if (bound) SlotBinds.sc(slot) else 0
            val bool = slot == selected

            drawRectangle(x, y, 18, 18, if (bool) Mocha.Surface1.argb else if (bound) Mocha.Surface2.argb else Mocha.Surface0.argb)
            drawOutline(x, y, 18, 18, 1, if (bool) Mocha.Lavender.argb else if (bound) bc else Mocha.Overlay0.argb)

            val lbl = slot.toString()
            text(lbl, x + (18 - client.font.width(lbl)) / 2, y + (18 - client.font.lineHeight) / 2 + 1, false, if (bool) Mocha.Lavender.argb else if (bound) bc else Mocha.Subtext0.argb)

            zones.add(UIZone(x, y, 18, 18, ZoneType.SLOT, data = slot))
            if (mx in x until x + 18 && my in y until y + 18) {
                if (bound) {
                    tt = "L: Cycle | R: Remove"
                    tc = bc
                } else if (selected != null && (selected!! in 36..44) != (slot in 36..44)) {
                    tt = "Click to bind"
                    tc = Mocha.Green.argb
                }

                tx = x + 20
                ty = y + 1
            }
        }

        for (e in binds.int2IntEntrySet()) {
            val posA = pos(e.intKey, px + (pw - 178) / 2, py + ph - 112, y) ?: continue
            val posB = pos(e.intValue, px + (pw - 178) / 2, py + ph - 112, y) ?: continue
            drawLine(posA.first, posA.second, posB.first, posB.second, SlotBinds.m2.get(e.intKey), 1)
        }

        if (binds.isEmpty()) {
            text("No binds yet.", px + (pw - client.font.width("No binds yet.")) / 2, py + 30, false, Mocha.Overlay0.argb)
        }

        val count = "${binds.size} bind${if (binds.size != 1) "s" else ""}"
        text(count, px + pw - 6 - client.font.width(count), py + ph - 6 - client.font.lineHeight, false, Mocha.Overlay0.argb)
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (creating) {
            val z = zones.firstOrNull { it.type == ZoneType.PROFILE_NAME }
            if (z != null && mouseX in z.x until z.x + z.w && mouseY in z.y until z.y + z.h) {
                if (button == 0) {
                    name.focused = true
                    name.updateClick(mouseX, z.x)
                }

                return true
            }

            create()
            return true
        }

        val sz = zones.lastOrNull { it.type == ZoneType.SLOT && mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h }
        if (sz != null) {
            val slot = sz.data
            val bound = fn(slot)

            if (button == 1) {
                if (bound) SlotBinds.unbind(slot)
                selected = null
                return true
            }

            if (button == 0) {
                if (bound) {
                    SlotBinds.cycle(slot)
                    selected = null
                    return true
                }

                val s = selected
                when {
                    s == slot -> {
                        selected = null
                    }

                    s != null && ((s in 36..44) != (slot in 36..44)) -> {
                        SlotBinds.bind(s, slot)
                        selected = null
                    }

                    else -> {
                        selected = slot
                    }
                }

                return true
            }
        }

        if (button == 1) {
            val z = zones.lastOrNull { it.type == ZoneType.PROFILE_TAB && it.category.isNotEmpty() && mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h }
            deleting = if (z == null || deleting == z.category) null else z.category
            return true
        }

        if (button != 0) return false

        val hit = zones.lastOrNull { mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h } ?: return false
        val zt = hit.type as? ZoneType ?: return false

        when (zt) {
            ZoneType.PROFILE_TAB -> {
                val name = hit.category
                if (deleting != null && deleting == name) {
                    if (SlotBinds.map0.size > 1) {
                        SlotBinds.save()
                        SlotBinds.delete(name)
                    }

                    deleting = null
                    return true
                }

                deleting = null
                selected = null

                if (name != SlotBinds.active) {
                    SlotBinds.save()
                    SlotBinds.disk()
                    SlotBinds.load(name)
                }
            }

            ZoneType.PROFILE_ADD -> {
                SlotBinds.save()
                SlotBinds.disk()

                creating = true
                deleting = null
                selected = null

                name.reset()
                name.focused = true
            }

            ZoneType.PROFILE_NAME -> {
                name.focused = true
                name.updateClick(mouseX, hit.x)
            }

            ZoneType.SLOT -> {}
        }

        return true
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!creating) return super.onScramKeyPress(keyCode, scanCode, modifiers)

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            create()
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            name.reset()
            creating = false
            return true
        }

        if (name.focused) {
            name.handleKey(keyCode, modifiers)
        }

        return true
    }

    override fun onScramCharType(char: Char): Boolean {
        if (creating && name.focused) return name.handleChar(char)
        return super.onScramCharType(char)
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        val pw = 110 + 6 + 210
        val px = (width - pw) / 2
        if (mouseX < px || mouseX > px + 110) return false

        s = (s + (vertical * 10).toInt()).coerceIn(-maxOf(0, (SlotBinds.map0.size * 20 + 18) - 152), 0)
        return true
    }

    private fun create() {
        val str = name.value.trim()
        if (str.isNotEmpty() && !SlotBinds.map0.containsKey(str)) {
            SlotBinds.save()
            SlotBinds.disk()
            SlotBinds.add(str)
        }

        name.reset()
        creating = false
    }

    private fun fn(int: Int): Boolean =
        SlotBinds.m0.containsKey(int) || SlotBinds.m1.containsKey(int)

    private fun pos(slot: Int, invX: Int, invY: Int, hotY: Int): Pair<Int, Int>? =
        when (slot) {
            in 9..35 -> (invX + (slot - 9) % 9 * 20 + 9) to (invY + (slot - 9) / 9 * 20 + 9)
            in 36..44 -> (invX + (slot - 36) * 20 + 9) to (hotY + 9)
            else -> null
        }
}