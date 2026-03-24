@file:Suppress("ConstPropertyName", "SameParameterValue")

package xyz.aerii.athen.modules.impl.render.radial.impl

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.impl.render.radial.base.ISlot
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.CommandAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.MessageAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.NoAction
import xyz.aerii.athen.modules.impl.render.radial.impl.RadialMenu.configs
import xyz.aerii.athen.ui.InputField
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.IZoneType
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text

object RadialEditor : Scram("Radial menu editor [Athen]") {
    private enum class ZoneType : IZoneType {
        SLOT,
        ADD_SLOT,
        ADD_SUB,
        REMOVE,
        TYPE,
        TOGGLE,
        MOVE_UP,
        MOVE_DOWN,
        CONFIG_PREV,
        CONFIG_NEXT,
        CONFIG_ADD,
        CONFIG_DELETE,
        CONFIG_NAME,
        FIELD_NAME,
        FIELD_ITEM,
        FIELD_VALUE,
        FIELD_TEXTURE,
        PREVIEW
    }

    private val working = mutableListOf<ISlot>()
    private var selMain = 0
    private var selSub = -1
    private val collapsed = mutableSetOf<Int>()

    private val nameField = InputField("Name")
    private val itemField = InputField("Item ID")
    private val valField = InputField("Value")
    private val cfgField = InputField("Config name")
    private val texField = InputField("Texture ID")
    private val fields = listOf(nameField, itemField, valField, texField)
    private var type = 0

    private var scroll = 0
    private var maxScroll = 0
    private val zones = mutableListOf<UIZone>()

    private var notif: String? = null
    private var notifTime = 0L
    private var editing = false

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
        unfocusAll()

        val s = slot ?: return
        nameField.value = s.name
        nameField.cursor = s.name.length
        itemField.value = s.itemId
        itemField.cursor = s.itemId.length

        type = when (s.action) {
            is CommandAction -> 1
            is MessageAction -> 2
            else -> 0
        }

        val actionVal = when (val a = s.action) {
            is CommandAction -> a.command
            is MessageAction -> a.message
            else -> ""
        }
        valField.value = actionVal
        valField.cursor = actionVal.length

        val tex = s.text ?: ""
        texField.value = tex
        texField.cursor = tex.length
    }

    private fun commit() {
        val s = slot ?: return
        s.name = nameField.value
        s.itemId = itemField.value
        s.text = texField.value.ifBlank { null }
        s.action = when (type) {
            1 -> CommandAction(valField.value)
            2 -> MessageAction(valField.value)
            else -> NoAction
        }
    }

    private fun unfocusAll() {
        for (f in fields) f.focused = false
        cfgField.focused = false
        editing = false
    }

    private fun focusedField(): InputField? = fields.firstOrNull { it.focused } ?: if (cfgField.focused) cfgField else null

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        zones.clear()

        guiGraphics.drawRectangle(0, 0, width, height, Mocha.Crust.withAlpha(0.6f))

        val px = (width - 906) / 2
        val py = (height - 320) / 2

        drawSidebar(guiGraphics, mouseX, mouseY, px, py)

        val mx = px + 130 + 6
        drawMainPanel(guiGraphics, mouseX, mouseY, mx, py)

        val prx = mx + 444 + 6
        drawPreviewPanel(guiGraphics, mouseX, mouseY, prx, py)
    }

    private fun fnSub2(): List<Pair<Int, ISlot>> {
        if (RadialMenu.subMenu != 2 || selMain !in working.indices) return emptyList()
        val sub = working[selMain].sub
        val n = sub.size
        val m = maxOf(3, working.size)
        val p = selMain

        return List(n) { i ->
            ((p - n / 2 + i + m * 2) % m) to sub[i]
        }
    }

    private fun slotHover(mx: Float, my: Float, cx: Int, cy: Int, num: Int, inn: Float, out: Float): Int {
        if (num == 0) return -1
        val inner = inn * 1.3f
        val outer = out * 1.3f
        val step = (kotlin.math.PI * 2 / num).toFloat()
        val gap = Math.toRadians(4.0).toFloat()
        val off = (-kotlin.math.PI / 2 - step / 2).toFloat()

        for (i in 0 until num) {
            val s = i * step + gap * .5f + off
            val e = (i + 1) * step - gap * .5f + off
            val c0 = Mth.cos(s)
            val s0 = Mth.sin(s)
            val c1 = Mth.cos(e)
            val s1 = Mth.sin(e)
            val x0 = cx + outer * c0
            val y0 = cy + outer * s0
            val x1 = cx + inner * c0
            val y1 = cy + inner * s0
            val x2 = cx + inner * c1
            val y2 = cy + inner * s1
            val x3 = cx + outer * c1
            val y3 = cy + outer * s1

            if (SlotsRenderState.tri(mx, my, x0, y0, x1, y1, x2, y2) || SlotsRenderState.tri(mx, my, x0, y0, x2, y2, x3, y3)) return i
        }
        return -1
    }

    private fun drawPreviewPanel(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, prx: Int, pry: Int) {
        guiGraphics.drawOutline(prx, pry, 320, 320, 1, Mocha.Surface0.argb)

        if (working.isEmpty()) return

        val cx = prx + 320 / 2
        val cy = pry + 320 / 2

        val bool = RadialMenu.subMenu == 0 && selSub >= 0 && selMain in working.indices
        val current = if (bool) working[selMain].sub else working
        val num = maxOf(3, current.size)

        var hm = -1
        var hs = -1
        val dx = mouseX - cx
        val dy = mouseY - cy
        val hCenter = dx * dx + dy * dy < 144f

        val ex = if (!bool) fnSub2() else emptyList()

        if (!hCenter && dx * dx + dy * dy >= 225f) {
            if (!bool && RadialMenu.subMenu == 2 && selMain in working.indices) {
                val hit = SlotsRenderState.hitSub0(mouseX.toFloat(), mouseY.toFloat(), cx, cy, num, 80f, ex.map { it.first })
                if (hit != -1) hs = hit
            }

            if (!bool && hs == -1 && RadialMenu.subMenu == 1 && selMain in working.indices) {
                val hit = SlotsRenderState.hitSub(mouseX.toFloat(), mouseY.toFloat(), cx, cy, num, 80f, selMain, working[selMain].sub.size)
                if (hit != -1) {
                    hm = selMain
                    hs = hit
                }
            }

            if (hm == -1 && hs == -1) {
                hm = slotHover(mouseX.toFloat(), mouseY.toFloat(), cx, cy, num, 50f, 80f)
            }
        }

        val mini = if (!bool && RadialMenu.subMenu == 1 && selMain in working.indices) working[selMain].sub else emptyList()
        val idx2 = if (!bool && RadialMenu.subMenu == 2 && hs != -1) ex.getOrNull(hs)?.first ?: -1 else -1

        val am = if (bool) selSub else selMain
        val cNorm = java.awt.Color(Mocha.Surface0.withAlpha(0.5f), true).rgb
        val cHov = java.awt.Color(Mocha.Mauve.withAlpha(0.5f), true).rgb

        guiGraphics.guiRenderState.submitGuiElement(SlotsRenderState(guiGraphics, cx, cy, num, 50f, 80f, cNorm, cHov, hm, mini, hs, am, ex, idx2))
        guiGraphics.guiRenderState.nextStratum()

        for (i in current.indices) {
            val (sx, sy) = SlotsRenderState.centerSlot(cx, cy, num, 50f, 80f, i)
            guiGraphics.renderItem(current[i].item, sx - 8, sy - 8)
        }

        if (!bool && RadialMenu.subMenu == 1 && selMain in working.indices) {
            for (j in working[selMain].sub.indices) {
                val (sx, sy) = SlotsRenderState.centerSub(cx, cy, num, 80f, selMain, j)
                guiGraphics.renderItem(working[selMain].sub[j].item, sx - 8, sy - 8)
            }
        }

        if (!bool && RadialMenu.subMenu == 2) {
            for ((i, s) in ex) {
                val (sx, sy) = SlotsRenderState.centerSub0(cx, cy, num, 80f, i)
                guiGraphics.renderItem(s.item, sx - 8, sy - 8)
            }
        }

        val back = bool || (RadialMenu.subMenu == 2 && selMain in working.indices && selSub >= 0)
        val str = if (back) "←" else "✕"

        guiGraphics.drawRectangle(cx - 12, cy - 12, 24, 24, if (hCenter) Mocha.Surface2.argb else Mocha.Surface1.argb)
        guiGraphics.drawOutline(cx - 12, cy - 12, 24, 24, 1, if (hCenter) Mocha.Mauve.argb else Mocha.Overlay0.argb)
        guiGraphics.drawString(client.font, str, cx - client.font.width(str) / 2, cy - client.font.lineHeight / 2, if (hCenter) Mocha.Mauve.argb else Mocha.Subtext0.argb, false)

        val label = if (hCenter) (if (back) "Back" else "Exit") else {
            if (hs != -1) working.getOrNull(selMain)?.sub?.getOrNull(hs)?.name
            else current.getOrNull(hm)?.name
        }

        if (label != null && mouseX in prx until prx + 320 && mouseY in pry until pry + 320) {
            val tw = client.font.width(label)
            val lmx = mouseX + 12
            val lmy = mouseY - 4
            guiGraphics.drawRectangle(lmx - 5, lmy - 5, tw + 10, client.font.lineHeight + 10, Mocha.Base.argb)
            guiGraphics.drawOutline(lmx - 5, lmy - 5, tw + 10, client.font.lineHeight + 10, 1, Mocha.Mauve.argb)
            guiGraphics.text(label, lmx, lmy, false, Mocha.Text.argb)
        }

        val zMain = if (hCenter) -2 else if (bool) selMain else hm
        val zSub = if (hCenter) -1 else if (bool) hm else hs
        zones.add(UIZone(prx, pry, 320, 320, ZoneType.PREVIEW, zMain, zSub))
    }

    private fun drawSidebar(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, sx: Int, sy: Int) {
        guiGraphics.drawRectangle(sx, sy, 130, 320, Mocha.Base.argb)
        guiGraphics.drawOutline(sx, sy, 130, 320, 1, Mocha.Surface0.argb)

        var hy = sy + 6

        val bx = sx + 130 - 6 - 12 * 3 - 4
        val lbe = sx + 6 + 12 + 2

        guiGraphics.button(mouseX, mouseY, sx + 6, hy + (14 - 12) / 2, "<", Mocha.Text.argb, ZoneType.CONFIG_PREV)
        guiGraphics.button(mouseX, mouseY, bx, hy + (14 - 12) / 2, ">", Mocha.Text.argb, ZoneType.CONFIG_NEXT)
        guiGraphics.button(mouseX, mouseY, bx + 12 + 2, hy + (14 - 12) / 2, "+", Mocha.Green.argb, ZoneType.CONFIG_ADD)
        guiGraphics.button(mouseX, mouseY, bx + (12 + 2) * 2, hy + (14 - 12) / 2, "×", Mocha.Red.argb, ZoneType.CONFIG_DELETE)

        val nw = bx - lbe - 2
        val ny = hy + (14 - client.font.lineHeight) / 2

        if (editing) {
            val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            val txt = cfgField.value + cursor
            guiGraphics.text(txt, lbe + (nw - client.font.width(txt)) / 2, ny + 1, false, Mocha.Text.argb)
        } else {
            val n = RadialMenu.active
            guiGraphics.text(n, lbe + (nw - client.font.width(n)) / 2, ny + 1, false, Mocha.Mauve.argb)
        }

        zones.add(UIZone(lbe, hy, nw, 14, ZoneType.CONFIG_NAME))

        hy += 14 + 4
        guiGraphics.drawRectangle(sx + 6, hy, 130 - 6 * 2, 1, Mocha.Surface0.argb)
        hy += 4

        guiGraphics.text("Slots", sx + 6, hy, false, Mocha.Subtext0.argb)
        hy += client.font.lineHeight + 2
        guiGraphics.drawRectangle(sx + 6, hy, 130 - 6 * 2, 1, Mocha.Surface0.argb)

        val ly = hy + 4
        val lh = sy + 320 - ly - 6

        guiGraphics.enableScissor(sx, ly, sx + 130, ly + lh)

        var iy = ly - scroll
        for (i in working.indices) {
            val selected = i == selMain && selSub < 0
            val hasSub = working[i].sub.isNotEmpty()
            val expanded = hasSub && i !in collapsed

            if (iy + 20 > ly && iy < ly + lh) {
                val hovered = mouseX in sx until sx + 130 && mouseY in iy until iy + 20

                when {
                    selected -> {
                        guiGraphics.drawRectangle(sx + 1, iy, 130 - 2, 20, Mocha.Surface2.argb)
                        guiGraphics.drawRectangle(sx, iy, 2, 20, Mocha.Mauve.argb)
                    }

                    hovered -> {
                        guiGraphics.drawRectangle(sx + 1, iy, 130 - 2, 20, Mocha.Surface1.argb)
                    }
                }

                guiGraphics.renderItem(working[i].item, sx + 6, iy + 2)
                val ty = iy + (20 - client.font.lineHeight) / 2
                val label = working[i].name.ifBlank { "..." }

                if (!selected) {
                    guiGraphics.text(label, sx + 6 + 18, ty + 1, false, Mocha.Subtext0.argb)
                } else {
                    val rbx = sx + 130 - 6 - (12 + 2) * 3
                    guiGraphics.text(label, sx + 6 + 18, ty + 1, false, Mocha.Text.argb)
                    guiGraphics.button(mouseX, mouseY, rbx, iy + (20 - 12) / 2, "↑", Mocha.Text.argb, ZoneType.MOVE_UP)
                    guiGraphics.button(mouseX, mouseY, rbx + 12 + 2, iy + (20 - 12) / 2, "↓", Mocha.Text.argb, ZoneType.MOVE_DOWN)
                    guiGraphics.button(mouseX, mouseY, rbx + (12 + 2) * 2, iy + (20 - 12) / 2, "×", Mocha.Red.argb, ZoneType.REMOVE)
                }

                if (hasSub) {
                    val arrow = if (expanded) "▾" else "▸"
                    val ax = if (selected) sx + 130 - 6 - (12 + 2) * 3 - client.font.width(arrow) - 4 else sx + 130 - 6 - client.font.width(arrow) - 2
                    guiGraphics.text(arrow, ax, ty + 1, false, Mocha.Overlay0.argb)
                    zones.add(UIZone(ax - 2, iy, sx + 130 - ax + 2, 20, ZoneType.TOGGLE, i))
                }

                zones.add(UIZone(sx, iy, if (hasSub) 130 - 6 - 14 else 130, 20, ZoneType.SLOT, i, -1))
            }
            iy += 20

            if (expanded) {
                for (j in working[i].sub.indices) {
                    if (iy + 18 > ly && iy < ly + lh) {
                        val a = i == selMain && j == selSub
                        val subHov = mouseX in sx until sx + 130 && mouseY in iy until iy + 18

                        when {
                            a -> {
                                guiGraphics.drawRectangle(sx + 1, iy, 130 - 2, 18, Mocha.Surface2.argb)
                                guiGraphics.drawRectangle(sx, iy, 2, 18, Mocha.Mauve.argb)
                            }

                            subHov -> {
                                guiGraphics.drawRectangle(sx + 1, iy, 130 - 2, 18, Mocha.Surface1.argb)
                            }
                        }

                        guiGraphics.drawRectangle(sx + 6 + 4, iy + 2, 1, 18 - 4, Mocha.Overlay0.argb)
                        guiGraphics.renderItem(working[i].sub[j].item, sx + 6 + 8, iy + 1)
                        val sty = iy + (18 - client.font.lineHeight) / 2
                        val subLabel = working[i].sub[j].name.ifBlank { "..." }

                        if (!a) {
                            guiGraphics.text(subLabel, sx + 6 + 26, sty + 1, false, Mocha.Subtext0.argb)
                        } else {
                            val rbx = sx + 130 - 6 - (12 + 2) * 3
                            guiGraphics.text(subLabel, sx + 6 + 26, sty + 1, false, Mocha.Text.argb)
                            guiGraphics.button(mouseX, mouseY, rbx, iy + (18 - 12) / 2, "↑", Mocha.Text.argb, ZoneType.MOVE_UP)
                            guiGraphics.button(mouseX, mouseY, rbx + 12 + 2, iy + (18 - 12) / 2, "↓", Mocha.Text.argb, ZoneType.MOVE_DOWN)
                            guiGraphics.button(mouseX, mouseY, rbx + (12 + 2) * 2, iy + (18 - 12) / 2, "×", Mocha.Red.argb, ZoneType.REMOVE)
                        }

                        zones.add(UIZone(sx, iy, 130, 18, ZoneType.SLOT, i, j))
                    }

                    iy += 18
                }
            }

            val a = expanded || (selected && !hasSub)
            if (a && iy + 18 > ly && iy < ly + lh) {
                val addHov = mouseX in sx + 6 until sx + 6 + 50 && mouseY in iy until iy + 18
                guiGraphics.drawRectangle(sx + 6 + 4, iy + 2, 1, 18 - 4, Mocha.Overlay0.argb)
                guiGraphics.text("+ Sub", sx + 6 + 10, iy + (18 - client.font.lineHeight) / 2 + 1, false, if (addHov) Mocha.Green.argb else Mocha.Overlay0.argb)
                zones.add(UIZone(sx + 6, iy, 50, 18, ZoneType.ADD_SUB, i))
            }

            if (a) iy += 18
        }

        if (iy + 20 > ly && iy < ly + lh) {
            val label = "+ Slot"
            val bw = 130 - 6 * 2
            val bx = sx + 6
            val by = iy + (20 - 14) / 2
            val addHov = mouseX in bx until bx + bw && mouseY in by until by + 14

            guiGraphics.drawRectangle(bx, by, bw, 14, if (addHov) Mocha.Surface2.argb else Mocha.Surface1.argb)
            guiGraphics.drawOutline(bx, by, bw, 14, 1, Mocha.Overlay0.argb)
            guiGraphics.text(label, bx + (bw - client.font.width(label)) / 2, by + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Green.argb)
            zones.add(UIZone(bx, by, bw, 14, ZoneType.ADD_SLOT))
        }
        iy += 20

        guiGraphics.disableScissor()

        maxScroll = maxOf(0, (iy - (ly - scroll)) - lh)
        scroll = scroll.coerceIn(0, maxScroll)
    }

    private fun drawMainPanel(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, mx: Int, my: Int) {
        guiGraphics.drawRectangle(mx, my, 444, 320, Mocha.Base.argb)
        guiGraphics.drawOutline(mx, my, 444, 320, 1, Mocha.Surface0.argb)

        val rx = mx + 6
        val fw = 444 - 6 * 2
        var fy = my + 6

        guiGraphics.text("Edit Slot", rx, fy, false, Mocha.Text.argb)
        fy += client.font.lineHeight + 2
        guiGraphics.drawRectangle(rx, fy, fw, 1, Mocha.Surface0.argb)
        fy += 5

        if (slot == null) return guiGraphics.text("No slot selected.", rx, fy, false, Mocha.Subtext0.argb)

        guiGraphics.text("Name", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2
        nameField.draw(guiGraphics, mouseX, mouseY, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_NAME)) }
        fy += 16 + 4

        guiGraphics.text("Item ID", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2
        itemField.draw(guiGraphics, mouseX, mouseY, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_ITEM)) }
        fy += 16 + 4

        guiGraphics.text("Action", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2

        val aw = 55
        for ((i, l) in listOf("None", "Command", "Message").withIndex()) {
            val ax = rx + i * (aw + 4)
            val selected = type == i
            val hovered = mouseX in ax until ax + aw && mouseY in fy until fy + 14

            guiGraphics.drawRectangle(ax, fy, aw, 14, if (selected) Mocha.Mauve.argb else if (hovered) Mocha.Surface2.argb else Mocha.Surface1.argb)
            guiGraphics.drawOutline(ax, fy, aw, 14, 1, if (selected) Mocha.Mauve.argb else Mocha.Overlay0.argb)
            guiGraphics.text(l, ax + (aw - client.font.width(l)) / 2, fy + (14 - client.font.lineHeight) / 2 + 1, false, if (selected) Mocha.Base.argb else Mocha.Text.argb)

            zones.add(UIZone(ax, fy, aw, 14, ZoneType.TYPE, i))
        }

        fy += 14 + 4

        if (type != 0) {
            guiGraphics.text(if (type == 1) "Command" else "Message", rx, fy, false, Mocha.Subtext0.argb)
            fy += client.font.lineHeight + 2
            valField.draw(guiGraphics, mouseX, mouseY, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_VALUE)) }
            fy += 16 + 4
        }

        if (itemField.value == "player_head") {
            guiGraphics.text("Texture", rx, fy, false, Mocha.Subtext0.argb)
            fy += client.font.lineHeight + 2
            texField.draw(guiGraphics, mouseX, mouseY, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_TEXTURE)) }
        }

        notif?.let {
            val life = System.currentTimeMillis() - notifTime
            if (life > 2000) return@let ::notif.set(null)

            val alpha = if (life > 1500) 1f - (life - 1500) / 500f else 1f
            val tw = client.font.width(it)
            val x = mx + (444 - tw) / 2
            val y = my + 320 - 30

            guiGraphics.drawRectangle(x - 6, y - 4, tw + 12, client.font.lineHeight + 8, Mocha.Surface0.withAlpha(alpha))
            guiGraphics.drawOutline(x - 6, y - 4, tw + 12, client.font.lineHeight + 8, 1, Mocha.Mauve.withAlpha(alpha))
            guiGraphics.text(it, x, y, false, Mocha.Text.withAlpha(alpha))
        }
    }

    private fun GuiGraphics.button(mx: Int, my: Int, x: Int, y: Int, label: String, color: Int, type: ZoneType) {
        val hovered = mx in x until x + 12 && my in y until y + 12

        drawRectangle(x, y, 12, 12, if (hovered) Mocha.Surface2.argb else Mocha.Surface1.argb)
        drawOutline(x, y, 12, 12, 1, Mocha.Overlay0.argb)
        text(label, x + (12 - client.font.width(label)) / 2, y + (12 - client.font.lineHeight) / 2 + 1, false, color)

        zones.add(UIZone(x, y, 12, 12, type))
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false

        val hit = zones.firstOrNull {
            mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h
        } ?: run {
            unfocusAll()
            return false
        }

        val zt = hit.type as? ZoneType ?: return false

        when (zt) {
            ZoneType.SLOT -> {
                if (hit.data != selMain || hit.data2 != selSub) {
                    commit()
                    reload(hit.data, hit.data2)
                }
                unfocusAll()
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

            ZoneType.FIELD_NAME -> {
                unfocusAll()
                nameField.focused = true
                nameField.updateClick(mouseX, hit.x)
            }

            ZoneType.FIELD_ITEM -> {
                unfocusAll()
                itemField.focused = true
                itemField.updateClick(mouseX, hit.x)
            }

            ZoneType.FIELD_VALUE -> {
                unfocusAll()
                valField.focused = true
                valField.updateClick(mouseX, hit.x)
            }

            ZoneType.FIELD_TEXTURE -> {
                unfocusAll()
                texField.focused = true
                texField.updateClick(mouseX, hit.x)
            }

            ZoneType.CONFIG_PREV -> {
                commitSave()
                val names = configNames
                switch(names[(names.indexOf(RadialMenu.active) - 1 + names.size) % names.size])
            }

            ZoneType.CONFIG_NEXT -> {
                commitSave()
                val names = configNames
                switch(names[(names.indexOf(RadialMenu.active) + 1) % names.size])
            }

            ZoneType.CONFIG_ADD -> {
                commitSave()
                var name = "New"
                var n = 1
                while (configNames.contains(name)) name = "New ${++n}"
                RadialMenu.add(name)
                working.clear()
                working.addAll(RadialMenu.slots)
                reload(0, -1)
            }

            ZoneType.CONFIG_DELETE -> {
                if (configNames.size <= 1) return true
                commit()
                RadialMenu.slots.clear()
                RadialMenu.slots.addAll(working)
                RadialMenu.delete(RadialMenu.active)
                working.clear()
                working.addAll(RadialMenu.slots)
                reload(0, -1)
            }

            ZoneType.CONFIG_NAME -> {
                if (editing) {
                    rename()
                } else {
                    unfocusAll()
                    editing = true
                    cfgField.value = RadialMenu.active
                    cfgField.cursor = cfgField.value.length
                    cfgField.focused = true
                }
            }

            ZoneType.PREVIEW -> {
                commit()

                if (hit.data == -2) {
                    reload(selMain, -1)
                } else if (hit.data != -1 || hit.data2 != -1) {
                    val m = hit.data.takeIf { it != -1 } ?: selMain
                    val s = hit.data2

                    if (RadialMenu.subMenu == 0 && s == -1 && working.getOrNull(m)?.sub?.isNotEmpty() == true) reload(m, 0)
                    else reload(m, s)
                }

                unfocusAll()
            }
        }

        return true
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            val f = focusedField()
            if (f != null) {
                if (!editing) commit()
                unfocusAll()
                return true
            }

            return false
        }

        if (editing && cfgField.focused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                rename()
                return true
            }

            return cfgField.handleKey(keyCode, modifiers)
        }

        val f = focusedField() ?: return false

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            commit()
            unfocusAll()
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            commit()
            val order = mutableListOf(nameField, itemField)
            if (type != 0) order.add(valField)
            if (itemField.value == "player_head") order.add(texField)

            val idx = order.indexOf(f)
            if (idx >= 0) {
                unfocusAll()
                val next = order[(idx + 1) % order.size]
                next.focused = true
            }

            return true
        }

        val handled = f.handleKey(keyCode, modifiers)
        if (handled) commit()
        return handled
    }

    override fun onScramCharType(char: Char, modifiers: Int): Boolean {
        if (editing && cfgField.focused) return cfgField.handleChar(char)

        val f = focusedField() ?: return false
        val handled = f.handleChar(char)
        if (handled) commit()
        return handled
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        val px = (width - 906) / 2
        if (mouseX < px || mouseX > px + 130) return false
        scroll = (scroll - (vertical * 5).toInt()).coerceIn(0, maxScroll)
        return true
    }

    private fun String.notify() {
        notif = this
        notifTime = System.currentTimeMillis()
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
        val n = cfgField.value.trim()
        if (n.isNotBlank() && n != RadialMenu.active && n !in configNames) RadialMenu.rename(RadialMenu.active, n)
        unfocusAll()
    }
}