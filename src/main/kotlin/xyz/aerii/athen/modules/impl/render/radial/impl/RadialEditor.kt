@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.modules.impl.render.radial.impl

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.impl.render.radial.base.ISlot
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.CommandAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.MessageAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.NoAction
import xyz.aerii.athen.modules.impl.render.radial.impl.RadialMenu.configs
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text

object RadialEditor : Scram("Radial menu editor [Athen]") {
    private data class Zone(val x: Int, val y: Int, val w: Int, val h: Int, val type: ZoneType, val data: Int = 0, val data2: Int = -1)
    private enum class ZoneType {
        SLOT, ADD_SLOT, ADD_SUB, REMOVE, TYPE, FIELD, TOGGLE, MOVE_UP, MOVE_DOWN, CFG_PREV, CFG_NEXT, CFG_ADD, CFG_DEL, CFG_NAME
    }

    private const val sw = 130
    private const val padding = 6
    private const val rh = 20
    private const val srh = 18
    private const val fh = 14
    private const val bs = 12

    private val working = mutableListOf<ISlot>()
    private var selMain = 0
    private var selSub = -1
    private var focus = -1
    private val collapsed = mutableSetOf<Int>()

    private val nameBuf = StringBuilder()
    private val itemBuf = StringBuilder()
    private val valBuf = StringBuilder()
    private val cfgBuf = StringBuilder()
    private val texBuf = StringBuilder()
    private var type = 0

    private var scroll = 0
    private var maxScroll = 0
    private val zones = mutableListOf<Zone>()

    private var notif: String? = null
    private var notifTime = 0L

    private val slot: ISlot?
        get() = if (selSub >= 0) working.getOrNull(selMain)?.sub?.getOrNull(selSub) else working.getOrNull(selMain)

    private val configNames: List<String>
        get() = configs.keys.toList()

    private val maxSub
        get(): Int = when (RadialMenu.subMenu) {
            1 -> 5
            2 -> working.size
            else -> Int.MAX_VALUE
        }

    override fun onScramInit() {
        working.clear()
        working.addAll(RadialMenu.slots)
        collapsed.clear()
        reload(0, -1)
    }

    override fun onScramClose() {
        commit()
        RadialMenu.slots.clear()
        RadialMenu.slots.addAll(working)
        RadialMenu.save()
        RadialMenu.disk()
    }

    private fun reload(main: Int = selMain, sub: Int = selSub) {
        selMain = main.coerceIn(0, maxOf(0, working.size - 1))
        selSub = if (sub >= 0) sub.coerceIn(0, maxOf(0, (working.getOrNull(selMain)?.sub?.size ?: 1) - 1)) else -1
        focus = -1

        val s = slot ?: return
        nameBuf.replace(0, nameBuf.length, s.name)
        itemBuf.replace(0, itemBuf.length, s.itemId)

        type = when (s.action) {
            is CommandAction -> 1
            is MessageAction -> 2
            else -> 0
        }

        valBuf.replace(0, valBuf.length, when (val a = s.action) {
            is CommandAction -> a.command
            is MessageAction -> a.message
            else -> ""
        })

        texBuf.replace(0, texBuf.length, s.text ?: "")
    }

    private fun commit() {
        val s = slot ?: return
        s.name = nameBuf.toString()
        s.itemId = itemBuf.toString()
        s.text = texBuf.toString().ifBlank { null }
        s.action = when (type) {
            1 -> CommandAction(valBuf.toString())
            2 -> MessageAction(valBuf.toString())
            else -> NoAction
        }
    }

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        zones.clear()

        val sw = width
        val sh = height

        guiGraphics.drawRectangle(0, 0, sw, sh, Mocha.Base.argb)
        guiGraphics.drawRectangle(0, 0, RadialEditor.sw, sh, Mocha.Surface0.argb)
        guiGraphics.drawOutline(0, 0, RadialEditor.sw, sh, 1, Mocha.Overlay0.argb)

        var hy = padding

        val bx = RadialEditor.sw - padding - bs * 3 - 4
        val lbe = padding + bs + 2

        guiGraphics.button(mouseX, mouseY, padding, hy + (fh - bs) / 2, "◀", Mocha.Text.argb, ZoneType.CFG_PREV)
        guiGraphics.button(mouseX, mouseY, bx, hy + (fh - bs) / 2, "▶", Mocha.Text.argb, ZoneType.CFG_NEXT)
        guiGraphics.button(mouseX, mouseY, bx + bs + 2, hy + (fh - bs) / 2, "+", Mocha.Green.argb, ZoneType.CFG_ADD)
        guiGraphics.button(mouseX, mouseY, bx + (bs + 2) * 2, hy + (fh - bs) / 2, "×", Mocha.Red.argb, ZoneType.CFG_DEL)

        val nw = bx - lbe - 2
        val ny = hy + (fh - client.font.lineHeight) / 2

        if (focus == 3) {
            val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            val txt = cfgBuf.toString() + cursor
            guiGraphics.text(txt, lbe + (nw - client.font.width(txt)) / 2, ny + 1, false, Mocha.Text.argb)
        } else {
            val n = RadialMenu.active
            guiGraphics.text(n, lbe + (nw - client.font.width(n)) / 2, ny + 1, false, Mocha.Mauve.argb)
        }

        zones.add(Zone(lbe, hy, nw, fh, ZoneType.CFG_NAME))

        hy += fh + 4
        guiGraphics.drawRectangle(padding, hy, RadialEditor.sw - padding * 2, 1, Mocha.Overlay0.argb)
        hy += 4

        guiGraphics.text("Slots", padding, hy, false, Mocha.Subtext0.argb)
        hy += client.font.lineHeight + 2
        guiGraphics.drawRectangle(padding, hy, RadialEditor.sw - padding * 2, 1, Mocha.Overlay0.argb)

        val ly = hy + 4
        val lh = sh - ly - padding

        guiGraphics.enableScissor(0, ly, RadialEditor.sw, ly + lh)

        var iy = ly - scroll
        for (i in working.indices) {
            val selected = i == selMain && selSub < 0
            val hasSub = working[i].sub.isNotEmpty()
            val expanded = hasSub && i !in collapsed

            if (iy + rh > ly && iy < ly + lh) {
                val hovered = mouseX in 0 until RadialEditor.sw && mouseY in iy until iy + rh

                when {
                    selected -> {
                        guiGraphics.drawRectangle(1, iy, RadialEditor.sw - 2, rh, Mocha.Surface2.argb)
                        guiGraphics.drawRectangle(0, iy, 2, rh, Mocha.Mauve.argb)
                    }

                    hovered -> {
                        guiGraphics.drawRectangle(1, iy, RadialEditor.sw - 2, rh, Mocha.Surface1.argb)
                    }
                }

                guiGraphics.renderItem(working[i].item, padding, iy + 2)
                val ty = iy + (rh - client.font.lineHeight) / 2
                val label = working[i].name.ifBlank { "..." }

                if (!selected) {
                    guiGraphics.text(label, padding + 18, ty + 1, false, Mocha.Subtext0.argb)
                } else {
                    val bx = RadialEditor.sw - padding - (bs + 2) * 3
                    guiGraphics.text(label, padding + 18, ty + 1, false, Mocha.Text.argb)
                    guiGraphics.button(mouseX, mouseY, bx, iy + (rh - bs) / 2, "↑", Mocha.Text.argb, ZoneType.MOVE_UP)
                    guiGraphics.button(mouseX, mouseY, bx + bs + 2, iy + (rh - bs) / 2, "↓", Mocha.Text.argb, ZoneType.MOVE_DOWN)
                    guiGraphics.button(mouseX, mouseY, bx + (bs + 2) * 2, iy + (rh - bs) / 2, "×", Mocha.Red.argb, ZoneType.REMOVE)
                }

                if (hasSub) {
                    val arrow = if (expanded) "▾" else "▸"
                    val ax = if (selected) RadialEditor.sw - padding - (bs + 2) * 3 - client.font.width(arrow) - 4 else RadialEditor.sw - padding - client.font.width(arrow) - 2
                    guiGraphics.text(arrow, ax, ty + 1, false, Mocha.Overlay0.argb)
                    zones.add(Zone(ax - 2, iy, RadialEditor.sw - ax + 2, rh, ZoneType.TOGGLE, i))
                }

                zones.add(Zone(0, iy, if (hasSub) RadialEditor.sw - padding - 14 else RadialEditor.sw, rh, ZoneType.SLOT, i, -1))
            }
            iy += rh

            if (expanded) {
                for (j in working[i].sub.indices) {
                    if (iy + srh > ly && iy < ly + lh) {
                        val a = i == selMain && j == selSub
                        val subHov = mouseX in 0 until RadialEditor.sw && mouseY in iy until iy + srh

                        when {
                            a -> {
                                guiGraphics.drawRectangle(1, iy, RadialEditor.sw - 2, srh, Mocha.Surface2.argb)
                                guiGraphics.drawRectangle(0, iy, 2, srh, Mocha.Mauve.argb)
                            }

                            subHov -> {
                                guiGraphics.drawRectangle(1, iy, RadialEditor.sw - 2, srh, Mocha.Surface1.argb)
                            }
                        }

                        guiGraphics.drawRectangle(padding + 4, iy + 2, 1, srh - 4, Mocha.Overlay0.argb)
                        guiGraphics.renderItem(working[i].sub[j].item, padding + 10, iy + 1)
                        val sty = iy + (srh - client.font.lineHeight) / 2
                        val subLabel = working[i].sub[j].name.ifBlank { "..." }

                        if (!a) {
                            guiGraphics.text(subLabel, padding + 26, sty + 1, false, Mocha.Subtext0.argb)
                        } else {
                            val bx = RadialEditor.sw - padding - (bs + 2) * 3
                            guiGraphics.text(subLabel, padding + 26, sty + 1, false, Mocha.Text.argb)
                            guiGraphics.button(mouseX, mouseY, bx, iy + (srh - bs) / 2, "↑", Mocha.Text.argb, ZoneType.MOVE_UP)
                            guiGraphics.button(mouseX, mouseY, bx + bs + 2, iy + (srh - bs) / 2, "↓", Mocha.Text.argb, ZoneType.MOVE_DOWN)
                            guiGraphics.button(mouseX, mouseY, bx + (bs + 2) * 2, iy + (srh - bs) / 2, "×", Mocha.Red.argb, ZoneType.REMOVE)
                        }

                        zones.add(Zone(0, iy, RadialEditor.sw, srh, ZoneType.SLOT, i, j))
                    }

                    iy += srh
                }
            }

            val a = expanded || (selected && !hasSub)
            if (a && iy + srh > ly && iy < ly + lh) {
                val addHov = mouseX in padding until padding + 50 && mouseY in iy until iy + srh
                guiGraphics.drawRectangle(padding + 4, iy + 2, 1, srh - 4, Mocha.Overlay0.argb)
                guiGraphics.text("+ Sub", padding + 10, iy + (srh - client.font.lineHeight) / 2 + 1, false, if (addHov) Mocha.Green.argb else Mocha.Overlay0.argb)
                zones.add(Zone(padding, iy, 50, srh, ZoneType.ADD_SUB, i))
            }

            if (a) iy += srh
        }

        if (iy + rh > ly && iy < ly + lh) {
            val label = "+ Slot"
            val bw = RadialEditor.sw - padding * 2
            val bx = padding
            val by = iy + (rh - fh) / 2
            val addHov = mouseX in bx until bx + bw && mouseY in by until by + fh

            guiGraphics.drawRectangle(bx, by, bw, fh, if (addHov) Mocha.Surface2.argb else Mocha.Surface1.argb)
            guiGraphics.drawOutline(bx, by, bw, fh, 1, Mocha.Overlay0.argb)
            guiGraphics.text(label, bx + (bw - client.font.width(label)) / 2, by + (fh - client.font.lineHeight) / 2 + 1, false, Mocha.Green.argb)
            zones.add(Zone(bx, by, bw, fh, ZoneType.ADD_SLOT))
        }
        iy += rh

        guiGraphics.disableScissor()

        maxScroll = maxOf(0, (iy - (ly - scroll)) - lh)
        scroll = scroll.coerceIn(0, maxScroll)

        val rx = RadialEditor.sw + padding * 2
        val fw = sw - rx - padding
        var fy = padding

        guiGraphics.text("Edit Slot", rx, fy, false, Mocha.Text.argb)
        fy += client.font.lineHeight + 2
        guiGraphics.drawRectangle(rx, fy, fw, 1, Mocha.Overlay0.argb)
        fy += 5

        if (slot == null) return guiGraphics.text("No slot selected.", rx, fy, false, Mocha.Subtext0.argb)

        fy = guiGraphics.field(mouseX, mouseY, "Name", nameBuf, 0, rx, fy, fw) + 4
        fy = guiGraphics.field(mouseX, mouseY, "Item ID", itemBuf, 1, rx, fy, fw) + 4

        guiGraphics.text("Action", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2

        val aw = 55
        for ((i, l) in listOf("None", "Command", "Message").withIndex()) {
            val ax = rx + i * (aw + 4)
            val selected = type == i
            val hovered = mouseX in ax until ax + aw && mouseY in fy until fy + fh

            guiGraphics.drawRectangle(ax, fy, aw, fh, if (selected) Mocha.Mauve.argb else if (hovered) Mocha.Surface2.argb else Mocha.Surface1.argb)
            guiGraphics.drawOutline(ax, fy, aw, fh, 1, if (selected) Mocha.Mauve.argb else Mocha.Overlay0.argb)

            guiGraphics.text(l, ax + (aw - client.font.width(l)) / 2, fy + (fh - client.font.lineHeight) / 2 + 1, false, if (selected) Mocha.Base.argb else Mocha.Text.argb)

            zones.add(Zone(ax, fy, aw, fh, ZoneType.TYPE, i))
        }

        fy += fh + 4

        if (type != 0) guiGraphics.field(mouseX, mouseY, if (type == 1) "Command" else "Message", valBuf, 2, rx, fy, fw) + 4

        if (itemBuf.toString() == "player_head") {
            fy += if (type != 0) client.font.lineHeight + fh + 6 else 0
            guiGraphics.field(mouseX, mouseY, "Texture", texBuf, 4, rx, fy, fw) + 4
        }

        notif?.let {
            val life = System.currentTimeMillis() - notifTime
            if (life > 2000) return@let ::notif.set(null)

            val alpha = if (life > 1500) 1f - (life - 1500) / 500f else 1f
            val tw = client.font.width(it)
            val x = (sw - tw) / 2
            val y = sh - 30

            guiGraphics.drawRectangle(x - 6, y - 4, tw + 12, client.font.lineHeight + 8, Mocha.Surface0.withAlpha(alpha))
            guiGraphics.drawOutline(x - 6, y - 4, tw + 12, client.font.lineHeight + 8, 1, Mocha.Mauve.withAlpha(alpha))
            guiGraphics.text(it, x, y, false, Mocha.Text.withAlpha(alpha))
        }
    }

    private fun GuiGraphics.field(mx: Int, my: Int, label: String, buf: StringBuilder, id: Int, x: Int, y: Int, w: Int): Int {
        text(label, x, y, false, Mocha.Subtext0.argb)

        val iy = y + client.font.lineHeight + 2
        val focused = focus == id
        val hovered = mx in x until x + w && my in iy until iy + fh

        drawRectangle(x, iy, w, fh, if (focused) Mocha.Surface2.argb else Mocha.Surface1.argb)
        drawOutline(x, iy, w, fh, 1, (if (focused) Mocha.Mauve else if (hovered) Mocha.Overlay0 else Mocha.Surface2).argb)

        val ty = iy + (fh - client.font.lineHeight) / 2
        val txt = buf.toString()
        val tw = client.font.width(txt)
        val maxW = w - 6

        enableScissor(x + 2, iy, x + w - 2, iy + fh)

        val tx = if (tw > maxW) x + 3 + maxW - tw else x + 3
        text(txt, tx, ty + 1, false, Mocha.Text.argb)

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0L) {
            text("|", tx + tw, ty + 1, false, Mocha.Mauve.argb)
        }

        disableScissor()

        zones.add(Zone(x, iy, w, fh, ZoneType.FIELD, id))
        return iy + fh
    }

    private fun GuiGraphics.button(mx: Int, my: Int, x: Int, y: Int, label: String, color: Int, type: ZoneType) {
        val hovered = mx in x until x + bs && my in y until y + bs

        drawRectangle(x, y, bs, bs, if (hovered) Mocha.Surface2.argb else Mocha.Surface1.argb)
        drawOutline(x, y, bs, bs, 1, Mocha.Overlay0.argb)
        text(label, x + (bs - client.font.width(label)) / 2, y + (bs - client.font.lineHeight) / 2 + 1, false, color)

        zones.add(Zone(x, y, bs, bs, type))
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        val hit = zones.firstOrNull {
            mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h
        } ?: run {
            focus = -1
            return false
        }

        when (hit.type) {
            ZoneType.SLOT -> {
                if (hit.data != selMain || hit.data2 != selSub) {
                    commit()
                    reload(hit.data, hit.data2)
                }

                focus = -1
            }

            ZoneType.TOGGLE -> {
                if (hit.data in collapsed) collapsed.remove(hit.data)
                else collapsed.add(hit.data)
            }

            ZoneType.ADD_SLOT -> {
                commit()
                working.add(ISlot("New Slot"))
                reload(working.lastIndex, -1)
            }

            ZoneType.ADD_SUB -> {
                commit()
                val parent = working.getOrNull(hit.data) ?: return true

                if (parent.sub.size >= maxSub) {
                    "Reached max limit! Try a different sub-menu style to have more options!".notify()
                    return true
                }

                parent.sub += ISlot("New Sub")
                collapsed.remove(hit.data)
                reload(hit.data, working[hit.data].sub.lastIndex)
            }

            ZoneType.REMOVE -> {
                commit()

                if (selSub >= 0) {
                    val parent = working[selMain]
                    val subs = parent.sub.toMutableList()
                    subs.removeAt(selSub)
                    parent.sub = subs
                    return reload(selMain, if (subs.isEmpty()) -1 else maxOf(0, selSub - 1)).let { true }
                }

                if (working.isEmpty()) return true
                working.removeAt(selMain)
                reload(maxOf(0, selMain - 1), -1)
            }

            ZoneType.MOVE_UP -> {
                commit()

                if (selSub > 0) {
                    val parent = working[selMain]
                    val subs = parent.sub.toMutableList()
                    val tmp = subs[selSub]
                    subs[selSub] = subs[selSub - 1]
                    subs[selSub - 1] = tmp
                    parent.sub = subs
                    return reload(selMain, selSub - 1).let { true }
                }

                if (selSub >= 0 || selMain <= 0) return true
                val tmp = working[selMain]
                working[selMain] = working[selMain - 1]
                working[selMain - 1] = tmp
                reload(selMain - 1, -1)
            }

            ZoneType.MOVE_DOWN -> {
                commit()

                if (selSub >= 0 && selSub < (working.getOrNull(selMain)?.sub?.lastIndex ?: -1)) {
                    val parent = working[selMain]
                    val subs = parent.sub.toMutableList()
                    val tmp = subs[selSub]

                    subs[selSub] = subs[selSub + 1]
                    subs[selSub + 1] = tmp
                    parent.sub = subs

                    return reload(selMain, selSub + 1).let { true }
                }

                if (selSub >= 0 || selMain >= working.lastIndex) return true
                val tmp = working[selMain]
                working[selMain] = working[selMain + 1]
                working[selMain + 1] = tmp
                reload(selMain + 1, -1)
            }

            ZoneType.TYPE -> {
                type = hit.data
                commit()
            }

            ZoneType.FIELD -> {
                focus = hit.data
            }

            ZoneType.CFG_PREV -> {
                commitSave()
                val names = configNames
                switch(names[(names.indexOf(RadialMenu.active) - 1 + names.size) % names.size])
            }

            ZoneType.CFG_NEXT -> {
                commitSave()
                val names = configNames
                switch(names[(names.indexOf(RadialMenu.active) + 1) % names.size])
            }

            ZoneType.CFG_ADD -> {
                commitSave()
                var name = "New"
                var n = 1
                while (configNames.contains(name)) name = "New ${++n}"
                RadialMenu.add(name)
                working.clear()
                working.addAll(RadialMenu.slots)
                reload(0, -1)
            }

            ZoneType.CFG_DEL -> {
                if (configNames.size <= 1) return true
                commit()
                RadialMenu.slots.clear()
                RadialMenu.slots.addAll(working)
                RadialMenu.delete(RadialMenu.active)
                working.clear()
                working.addAll(RadialMenu.slots)
                reload(0, -1)
            }

            ZoneType.CFG_NAME -> {
                if (focus == 3) {
                    rename()
                } else {
                    focus = 3
                    cfgBuf.replace(0, cfgBuf.length, RadialMenu.active)
                }
            }
        }

        return true
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && focus != -1) {
            if (focus != 3) commit()
            focus = -1
            return true
        }

        if (focus == -1) return false
        val buf = buf() ?: return false

        when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE if (buf.isNotEmpty()) -> {
                buf.deleteCharAt(buf.lastIndex)
                if (focus != 3) commit()
            }

            GLFW.GLFW_KEY_ENTER -> {
                if (focus == 3) rename() else commit()
                focus = -1
            }

            GLFW.GLFW_KEY_TAB -> {
                if (focus == 3) {
                    rename()
                } else {
                    commit()
                    val max = if (itemBuf.toString() == "player_head") 5 else if (type != 0) 3 else 2
                    focus = (focus + 1) % max
                    if (focus == 4 && itemBuf.toString() != "player_head") focus = 0
                }
            }

            GLFW.GLFW_KEY_V if (modifiers and GLFW.GLFW_MOD_CONTROL != 0) -> {
                buf.append(McClient.clipboard)
                if (focus != 3) commit()
            }

            GLFW.GLFW_KEY_A if (modifiers and GLFW.GLFW_MOD_CONTROL != 0) -> {
                buf.replace(0, buf.length, "")
                if (focus != 3) commit()
            }
        }

        return true
    }

    override fun onScramCharType(char: Char, modifiers: Int): Boolean {
        if (focus == -1) return false
        val buf = buf() ?: return false

        buf.append(char)
        if (focus != 3) commit()
        return true
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        if (mouseX >= sw) return false
        scroll = (scroll - (vertical * 5).toInt()).coerceIn(0, maxScroll)
        return true
    }

    private fun String.notify() {
        notif = this
        notifTime = System.currentTimeMillis()
    }

    private fun buf() = when (focus) {
        0 -> nameBuf
        1 -> itemBuf
        2 -> valBuf
        3 -> cfgBuf
        4 -> texBuf
        else -> null
    }

    private fun commitSave() {
        commit()
        RadialMenu.slots.clear()
        RadialMenu.slots.addAll(working)
        RadialMenu.save()
    }

    private fun switch(name: String) {
        RadialMenu.load(name)
        working.clear()
        working.addAll(RadialMenu.slots)
        collapsed.clear()
        scroll = 0
        reload(0, -1)
    }

    private fun rename() {
        val n = cfgBuf.toString().trim()
        if (n.isNotBlank() && n != RadialMenu.active && n !in configNames) RadialMenu.rename(RadialMenu.active, n)
        focus = -1
    }
}