@file:Suppress("ConstPropertyName", "SameParameterValue")

package xyz.aerii.athen.modules.impl.render.radial.impl

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.api.rendering.ui.effects.outline.outline
import xyz.aerii.athen.api.rendering.ui.shapes.rectangle.rectangle
import xyz.aerii.athen.api.rendering.ui.text.vanilla.extensions.extractText
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.modules.impl.render.radial.base.ISlot
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction
import xyz.aerii.athen.modules.impl.render.radial.impl.RadialMenu.configs
import xyz.aerii.athen.ui.IZoneType
import xyz.aerii.athen.ui.InputField
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.library.api.client
import xyz.aerii.library.utils.hovered
import kotlin.math.*

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
    private var sel0 = 0
    private var sel1 = -1
    private val collapsed = mutableSetOf<Int>()

    private val nameField = InputField("Name")
    private val itemField = InputField("Item ID")
    private val valField = InputField("Value")
    private val cfgField = InputField("Config name")
    private val texField = InputField("Texture ID")
    private val fields = listOf(nameField, itemField, valField, texField)
    private var type = 0

    private var last = -1

    private var scroll = 0
    private var maxScroll = 0
    private val zones = mutableListOf<UIZone>()

    private var notif: String? = null
    private var notifTime = 0L
    private var editing = false

    private val slot: ISlot?
        get() = if (sel1 >= 0) working.getOrNull(sel0)?.sub?.getOrNull(sel1) else working.getOrNull(sel0)

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

        if (last != -1) return
        last = client.options.guiScale().get()
        client.options.guiScale().set(2)
    }

    override fun onScramClose() {
        if (last != -1) {
            client.options.guiScale().set(last)
            last = -1
        }

        commit()
        RadialMenu.slots.clear()
        RadialMenu.slots.addAll(working)
        RadialMenu.save()
        RadialMenu.disk()
    }

    private fun reload(main: Int = sel0, sub: Int = sel1) {
        sel0 = main.coerceIn(0, maxOf(0, working.size - 1))
        sel1 = if (sub >= 0) sub.coerceIn(0, maxOf(0, (working.getOrNull(sel0)?.sub?.size ?: 1) - 1)) else -1
        unfocusAll()

        val s = slot ?: return
        nameField.value = s.name
        nameField.cursor = s.name.length
        itemField.value = s.itemId
        itemField.cursor = s.itemId.length

        type = s.action.id
        valField.value = s.action.serializable
        valField.cursor = s.action.serializable.length

        val tex = s.text ?: ""
        texField.value = tex
        texField.cursor = tex.length
    }

    private fun commit() {
        val s = slot ?: return
        s.name = nameField.value
        s.itemId = itemField.value
        s.text = texField.value.ifBlank { null }
        s.action = IAction.create(type, valField.value)
    }

    private fun unfocusAll() {
        for (f in fields) f.focused = false
        cfgField.focused = false
        editing = false
    }

    private fun focusedField(): InputField? = fields.firstOrNull { it.focused } ?: if (cfgField.focused) cfgField else null

    override fun onScramRender(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        zones.clear()

        graphics.rectangle(0, 0, width, height, Mocha.Crust.withAlpha(0.6f))

        val px = (width - 906) / 2
        val py = (height - 320) / 2

        drawSidebar(graphics, px, py)

        val mx = px + 130 + 6
        drawMainPanel(graphics, mx, py)

        val prx = mx + 444 + 6
        drawPreviewPanel(graphics, mouseX, mouseY, prx, py)
    }

    private fun fnSub2(): List<Pair<Int, ISlot>> {
        if (RadialMenu.subMenu != 2 || sel0 !in working.indices) return emptyList()
        val sub = working[sel0].sub
        val n = sub.size
        val m = maxOf(3, working.size)
        val p = sel0

        return List(n) { i ->
            ((p - n / 2 + i + m * 2) % m) to sub[i]
        }
    }

    private fun slotHover(mx: Float, my: Float, cx: Int, cy: Int, num: Int, inn: Float, out: Float): Int {
        if (num == 0) return -1
        val inner = inn * 1.3f
        val outer = out * 1.3f
        val step = (PI * 2 / num).toFloat()
        val gap = Math.toRadians(4.0).toFloat()
        val off = (-PI / 2 - step / 2).toFloat()

        for (i in 0 until num) {
            val s = i * step + gap * .5f + off
            val e = (i + 1) * step - gap * .5f + off
            val c0 = Mth.cos(s.toDouble())
            val s0 = Mth.sin(s.toDouble())
            val c1 = Mth.cos(e.toDouble())
            val s1 = Mth.sin(e.toDouble())
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

    private fun drawPreviewPanel(graphics: GuiGraphics, mouseX: Int, mouseY: Int, prx: Int, pry: Int) {
        graphics.outline(prx, pry, 320, 320, 1, Mocha.Surface0.argb)

        if (working.isEmpty()) return

        val cx = prx + 320 / 2
        val cy = pry + 320 / 2

        val bool = RadialMenu.subMenu == 0 && sel1 >= 0 && sel0 in working.indices
        val current = if (bool) working[sel0].sub else working
        val num = maxOf(3, current.size)

        val dx = mouseX - cx
        val dy = mouseY - cy
        val hc = dx * dx + dy * dy < 144f
        val ex = if (!bool) fnSub2() else emptyList()

        var hm = -1
        var hs = -1

        if (!hc && dx * dx + dy * dy >= 225f) {
            if (!bool && RadialMenu.subMenu == 2 && sel0 in working.indices) {
                val hit = SlotsRenderState.hitSub0(mouseX.toFloat(), mouseY.toFloat(), cx, cy, num, 80f, ex.map { it.first })
                if (hit != -1) hs = hit
            }

            if (!bool && hs == -1 && RadialMenu.subMenu == 1 && sel0 in working.indices) {
                val hit = SlotsRenderState.hitSub(mouseX.toFloat(), mouseY.toFloat(), cx, cy, num, 80f, sel0, working[sel0].sub.size)
                if (hit != -1) {
                    hm = sel0
                    hs = hit
                }
            }

            if (hm == -1 && hs == -1) {
                hm = slotHover(mouseX.toFloat(), mouseY.toFloat(), cx, cy, num, 50f, 80f)
            }
        }

        val mini = if (!bool && RadialMenu.subMenu == 1 && sel0 in working.indices) working[sel0].sub else emptyList()
        val idx2 = if (!bool && RadialMenu.subMenu == 2 && hs != -1) ex.getOrNull(hs)?.first ?: -1 else -1

        //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
        graphics.guiRenderState.submitGuiElement(SlotsRenderState(graphics, cx, cy, num, 50f, 80f, Mocha.Surface0.withAlpha(0.5f), Mocha.Mauve.withAlpha(0.5f), hm, mini, hs, if (bool) sel1 else sel0, ex, idx2))
        graphics.guiRenderState.nextStratum()

        for (i in current.indices) {
            val (sx, sy) = SlotsRenderState.centerSlot(cx, cy, num, 50f, 80f, i)
            //~ if >= 26.1 'renderItem(' -> 'item('
            graphics.renderItem(current[i].item, sx - 8, sy - 8)
        }

        if (!bool && RadialMenu.subMenu == 1 && sel0 in working.indices) {
            for (j in working[sel0].sub.indices) {
                val (sx, sy) = SlotsRenderState.centerSub(cx, cy, num, 80f, sel0, j)
                //~ if >= 26.1 'renderItem(' -> 'item('
                graphics.renderItem(working[sel0].sub[j].item, sx - 8, sy - 8)
            }
        }

        if (!bool && RadialMenu.subMenu == 2) {
            for ((i, s) in ex) {
                val (sx, sy) = SlotsRenderState.centerSub0(cx, cy, num, 80f, i)
                //~ if >= 26.1 'renderItem(' -> 'item('
                graphics.renderItem(s.item, sx - 8, sy - 8)
            }
        }

        val back = bool || (RadialMenu.subMenu == 2 && sel0 in working.indices && sel1 >= 0)
        val str = if (back) "←" else "✕"

        graphics.rectangle(cx - 12, cy - 12, 24, 24, if (hc) Mocha.Surface2.argb else Mocha.Surface1.argb)
        graphics.outline(cx - 12, cy - 12, 24, 24, 1, if (hc) Mocha.Mauve.argb else Mocha.Overlay0.argb)
        graphics.extractText(str, cx - client.font.width(str) / 2, cy - client.font.lineHeight / 2, false, if (hc) Mocha.Mauve.argb else Mocha.Subtext0.argb)

        val label = if (hc) (if (back) "Back" else "Exit") else {
            if (hs != -1) working.getOrNull(sel0)?.sub?.getOrNull(hs)?.name
            else current.getOrNull(hm)?.name
        }

        if (label != null && hovered(prx, pry, 320, 320, true)) {
            val tw = client.font.width(label)
            val lmx = mouseX + 12
            val lmy = mouseY - 4

            graphics.rectangle(lmx - 5, lmy - 5, tw + 10, client.font.lineHeight + 10, Mocha.Base.argb)
            graphics.outline(lmx - 5, lmy - 5, tw + 10, client.font.lineHeight + 10, 1, Mocha.Mauve.argb)
            graphics.extractText(label, lmx, lmy, false, Mocha.Text.argb)
        }

        val z0 = if (hc) -2 else if (bool) sel0 else hm
        val z1 = if (hc) -1 else if (bool) hm else hs
        zones.add(UIZone(prx, pry, 320, 320, ZoneType.PREVIEW, z0, z1))
    }

    private fun drawSidebar(graphics: GuiGraphics, sx: Int, sy: Int) {
        graphics.rectangle(sx, sy, 130, 320, Mocha.Base.argb)
        graphics.outline(sx, sy, 130, 320, 1, Mocha.Surface0.argb)

        var hy = sy + 6

        val bx = sx + 130 - 6 - 12 * 3 - 4
        val lbe = sx + 6 + 12 + 2

        graphics.button(sx + 6, hy + (14 - 12) / 2, "<", Mocha.Text.argb, ZoneType.CONFIG_PREV)
        graphics.button(bx, hy + (14 - 12) / 2, ">", Mocha.Text.argb, ZoneType.CONFIG_NEXT)
        graphics.button(bx + 12 + 2, hy + (14 - 12) / 2, "+", Mocha.Green.argb, ZoneType.CONFIG_ADD)
        graphics.button(bx + (12 + 2) * 2, hy + (14 - 12) / 2, "×", Mocha.Red.argb, ZoneType.CONFIG_DELETE)

        val nw = bx - lbe - 2
        val ny = hy + (14 - client.font.lineHeight) / 2

        if (editing) {
            val txt = cfgField.value + if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            graphics.extractText(txt, lbe + (nw - client.font.width(txt)) / 2, ny + 1, false, Mocha.Text.argb)
        } else {
            val n = RadialMenu.active
            graphics.extractText(n, lbe + (nw - client.font.width(n)) / 2, ny + 1, false, Mocha.Mauve.argb)
        }

        zones.add(UIZone(lbe, hy, nw, 14, ZoneType.CONFIG_NAME))

        hy += 14 + 4
        graphics.rectangle(sx + 6, hy, 130 - 6 * 2, 1, Mocha.Surface0.argb)
        hy += 4

        graphics.extractText("Slots", sx + 6, hy, false, Mocha.Subtext0.argb)
        hy += client.font.lineHeight + 2
        graphics.rectangle(sx + 6, hy, 130 - 6 * 2, 1, Mocha.Surface0.argb)

        val ly = hy + 4
        val lh = sy + 320 - ly - 6

        graphics.enableScissor(sx, ly, sx + 130, ly + lh)

        var iy = ly - scroll
        for (i in working.indices) {
            val selected = i == sel0 && sel1 < 0
            val hasSub = working[i].sub.isNotEmpty()
            val expanded = hasSub && i !in collapsed

            if (iy + 20 > ly && iy < ly + lh) {
                when {
                    selected -> {
                        graphics.rectangle(sx + 1, iy, 130 - 2, 20, Mocha.Surface2.argb)
                        graphics.rectangle(sx, iy, 2, 20, Mocha.Mauve.argb)
                    }

                    hovered(sx, iy, 130, 20, true) -> {
                        graphics.rectangle(sx + 1, iy, 130 - 2, 20, Mocha.Surface1.argb)
                    }
                }

                //~ if >= 26.1 'renderItem(' -> 'item('
                graphics.renderItem(working[i].item, sx + 6, iy + 2)
                val ty = iy + (20 - client.font.lineHeight) / 2
                val label = working[i].name.ifBlank { "..." }

                if (!selected) {
                    graphics.extractText(label, sx + 6 + 18, ty + 1, false, Mocha.Subtext0.argb)
                } else {
                    val rbx = sx + 130 - 6 - (12 + 2) * 3
                    graphics.extractText(label, sx + 6 + 18, ty + 1, false, Mocha.Text.argb)
                    graphics.button(rbx, iy + (20 - 12) / 2, "↑", Mocha.Text.argb, ZoneType.MOVE_UP)
                    graphics.button(rbx + 12 + 2, iy + (20 - 12) / 2, "↓", Mocha.Text.argb, ZoneType.MOVE_DOWN)
                    graphics.button(rbx + (12 + 2) * 2, iy + (20 - 12) / 2, "×", Mocha.Red.argb, ZoneType.REMOVE)
                }

                if (hasSub) {
                    val arrow = if (expanded) "▾" else "▸"
                    val ax = if (selected) sx + 130 - 6 - (12 + 2) * 3 - client.font.width(arrow) - 4 else sx + 130 - 6 - client.font.width(arrow) - 2
                    graphics.extractText(arrow, ax, ty + 1, false, Mocha.Overlay0.argb)
                    zones.add(UIZone(ax - 2, iy, sx + 130 - ax + 2, 20, ZoneType.TOGGLE, i))
                }

                zones.add(UIZone(sx, iy, if (hasSub) 130 - 6 - 14 else 130, 20, ZoneType.SLOT, i, -1))
            }

            iy += 20

            if (expanded) {
                for (j in working[i].sub.indices) {
                    if (iy + 18 > ly && iy < ly + lh) {
                        val a = i == sel0 && j == sel1

                        when {
                            a -> {
                                graphics.rectangle(sx + 1, iy, 130 - 2, 18, Mocha.Surface2.argb)
                                graphics.rectangle(sx, iy, 2, 18, Mocha.Mauve.argb)
                            }

                            hovered(sx, iy, 130, 18, true) -> {
                                graphics.rectangle(sx + 1, iy, 130 - 2, 18, Mocha.Surface1.argb)
                            }
                        }

                        graphics.rectangle(sx + 6 + 4, iy + 2, 1, 18 - 4, Mocha.Overlay0.argb)
                        //~ if >= 26.1 'renderItem(' -> 'item('
                        graphics.renderItem(working[i].sub[j].item, sx + 6 + 8, iy + 1)
                        val sty = iy + (18 - client.font.lineHeight) / 2
                        val subLabel = working[i].sub[j].name.ifBlank { "..." }

                        if (!a) {
                            graphics.extractText(subLabel, sx + 6 + 26, sty + 1, false, Mocha.Subtext0.argb)
                        } else {
                            val rbx = sx + 130 - 6 - (12 + 2) * 3
                            graphics.extractText(subLabel, sx + 6 + 26, sty + 1, false, Mocha.Text.argb)
                            graphics.button(rbx, iy + (18 - 12) / 2, "↑", Mocha.Text.argb, ZoneType.MOVE_UP)
                            graphics.button(rbx + 12 + 2, iy + (18 - 12) / 2, "↓", Mocha.Text.argb, ZoneType.MOVE_DOWN)
                            graphics.button(
                                rbx + (12 + 2) * 2, iy + (18 - 12) / 2, "×", Mocha.Red.argb, ZoneType.REMOVE)
                        }

                        zones.add(UIZone(sx, iy, 130, 18, ZoneType.SLOT, i, j))
                    }

                    iy += 18
                }
            }

            val a = expanded || (selected && !hasSub)
            if (a && iy + 18 > ly && iy < ly + lh) {
                graphics.rectangle(sx + 6 + 4, iy + 2, 1, 18 - 4, Mocha.Overlay0.argb)
                graphics.extractText("+ Sub", sx + 6 + 10, iy + (18 - client.font.lineHeight) / 2 + 1, false, if (hovered(sx + 6, iy, 50, 18, true)) Mocha.Green.argb else Mocha.Overlay0.argb)
                zones.add(UIZone(sx + 6, iy, 50, 18, ZoneType.ADD_SUB, i))
            }

            if (a) iy += 18
        }

        if (iy + 20 > ly && iy < ly + lh) {
            val label = "+ Slot"
            val bw = 130 - 6 * 2
            val bx = sx + 6
            val by = iy + (20 - 14) / 2

            graphics.rectangle(bx, by, bw, 14, if (hovered(bx, by, bw, 14, true)) Mocha.Surface2.argb else Mocha.Surface1.argb)
            graphics.outline(bx, by, bw, 14, 1, Mocha.Overlay0.argb)
            graphics.extractText(label, bx + (bw - client.font.width(label)) / 2, by + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Green.argb)
            zones.add(UIZone(bx, by, bw, 14, ZoneType.ADD_SLOT))
        }

        iy += 20
        graphics.disableScissor()

        maxScroll = maxOf(0, (iy - (ly - scroll)) - lh)
        scroll = scroll.coerceIn(0, maxScroll)
    }

    private fun drawMainPanel(graphics: GuiGraphics, mx: Int, my: Int) {
        graphics.rectangle(mx, my, 444, 320, Mocha.Base.argb)
        graphics.outline(mx, my, 444, 320, 1, Mocha.Surface0.argb)

        val rx = mx + 6
        val fw = 444 - 6 * 2
        var fy = my + 6

        graphics.extractText("Edit Slot", rx, fy, false, Mocha.Text.argb)
        fy += client.font.lineHeight + 2
        graphics.rectangle(rx, fy, fw, 1, Mocha.Surface0.argb)
        fy += 5

        if (slot == null) return graphics.extractText("No slot selected.", rx, fy, false, Mocha.Subtext0.argb)

        graphics.extractText("Name", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2
        nameField.draw(graphics, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_NAME)) }
        fy += 16 + 4

        graphics.extractText("Item ID", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2
        itemField.draw(graphics, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_ITEM)) }
        fy += 16 + 4

        graphics.extractText("Action", rx, fy, false, Mocha.Subtext0.argb)
        fy += client.font.lineHeight + 2

        val aw = 55
        val actions = IAction.all()

        for (a in actions) {
            val i = a.id
            val ax = rx + i * (aw + 4)
            val selected = type == i

            graphics.rectangle(ax, fy, aw, 14, if (selected) Mocha.Mauve.argb else if (hovered(ax, fy, aw, 14, true)) Mocha.Surface2.argb else Mocha.Surface1.argb)
            graphics.outline(ax, fy, aw, 14, 1, if (selected) Mocha.Mauve.argb else Mocha.Overlay0.argb)
            graphics.extractText(a.name, ax + (aw - client.font.width(a.name)) / 2, fy + (14 - client.font.lineHeight) / 2 + 1, false, if (selected) Mocha.Base.argb else Mocha.Text.argb)

            zones.add(UIZone(ax, fy, aw, 14, ZoneType.TYPE, a.id))
        }

        fy += 14 + 4

        if (type != 0) {
            graphics.extractText(IAction.all().firstOrNull { it.id == type }?.name ?: "Value", rx, fy, false, Mocha.Subtext0.argb)
            fy += client.font.lineHeight + 2
            valField.draw(graphics, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_VALUE)) }
            fy += 16 + 4
        }

        if (itemField.value == "player_head") {
            graphics.extractText("Texture", rx, fy, false, Mocha.Subtext0.argb)
            fy += client.font.lineHeight + 2
            texField.draw(graphics, rx, fy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, ZoneType.FIELD_TEXTURE)) }
        }

        notif?.let {
            val life = System.currentTimeMillis() - notifTime
            if (life > 2000) return@let ::notif.set(null)

            val alpha = if (life > 1500) 1f - (life - 1500) / 500f else 1f
            val tw = client.font.width(it)
            val x = mx + (444 - tw) / 2
            val y = my + 320 - 30

            graphics.rectangle(x - 6, y - 4, tw + 12, client.font.lineHeight + 8, Mocha.Surface0.withAlpha(alpha))
            graphics.outline(x - 6, y - 4, tw + 12, client.font.lineHeight + 8, 1, Mocha.Mauve.withAlpha(alpha))
            graphics.extractText(it, x, y, false, Mocha.Text.withAlpha(alpha))
        }
    }

    private fun GuiGraphics.button(x: Int, y: Int, label: String, color: Int, type: ZoneType) {
        rectangle(x, y, 12, 12, if (hovered(x, y, 12, 12, true)) Mocha.Surface2.argb else Mocha.Surface1.argb)
        outline(x, y, 12, 12, 1, Mocha.Overlay0.argb)
        extractText(label, x + (12 - client.font.width(label)) / 2, y + (12 - client.font.lineHeight) / 2 + 1, false, color)

        zones.add(UIZone(x, y, 12, 12, type))
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false

        val hit = zones.firstOrNull {
            hovered(it.x, it.y, it.w, it.h, true)
        } ?: run {
            unfocusAll()
            return false
        }

        when (hit.type as? ZoneType ?: return false) {
            ZoneType.SLOT -> {
                if (hit.data != sel0 || hit.data2 != sel1) {
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

                if (sel1 >= 0) {
                    val parent = working[sel0]
                    val subs = parent.sub.toMutableList()
                    subs.removeAt(sel1)
                    parent.sub = subs
                    return reload(sel0, if (subs.isEmpty()) -1 else maxOf(0, sel1 - 1)).let { true }
                }

                if (working.isEmpty()) return true
                working.removeAt(sel0)
                reload(maxOf(0, sel0 - 1), -1)
            }

            ZoneType.MOVE_UP -> {
                commit()

                if (sel1 > 0) {
                    val parent = working[sel0]
                    val subs = parent.sub.toMutableList()
                    val tmp = subs[sel1]
                    subs[sel1] = subs[sel1 - 1]
                    subs[sel1 - 1] = tmp
                    parent.sub = subs
                    return reload(sel0, sel1 - 1).let { true }
                }

                if (sel1 >= 0 || sel0 <= 0) return true
                val tmp = working[sel0]
                working[sel0] = working[sel0 - 1]
                working[sel0 - 1] = tmp
                reload(sel0 - 1, -1)
            }

            ZoneType.MOVE_DOWN -> {
                commit()

                if (sel1 >= 0 && sel1 < (working.getOrNull(sel0)?.sub?.lastIndex ?: -1)) {
                    val parent = working[sel0]
                    val subs = parent.sub.toMutableList()
                    val tmp = subs[sel1]

                    subs[sel1] = subs[sel1 + 1]
                    subs[sel1 + 1] = tmp
                    parent.sub = subs

                    return reload(sel0, sel1 + 1).let { true }
                }

                if (sel1 >= 0 || sel0 >= working.lastIndex) return true
                val tmp = working[sel0]
                working[sel0] = working[sel0 + 1]
                working[sel0 + 1] = tmp
                reload(sel0 + 1, -1)
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
                    reload(sel0, -1)
                } else if (hit.data != -1 || hit.data2 != -1) {
                    val m = hit.data.takeIf { it != -1 } ?: sel0
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

    override fun onScramCharType(char: Char): Boolean {
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