@file:Suppress("ObjectPrivatePropertyName", "Unused")

package xyz.aerii.athen.modules.impl.render.radial.impl

import com.google.gson.reflect.TypeToken
import net.minecraft.util.Mth
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.Athen
import xyz.aerii.athen.Athen.GSON
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.InputEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.render.radial.base.ISlot
import xyz.aerii.athen.modules.impl.render.radial.base.SlotData
import xyz.aerii.athen.modules.impl.render.radial.base.toData
import xyz.aerii.athen.modules.impl.render.radial.base.toSlot
import xyz.aerii.athen.modules.impl.render.radial.impl.SlotsRenderState.Companion.tri
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.center
import xyz.aerii.library.api.client
import xyz.aerii.library.api.lie
import xyz.aerii.library.api.repeat
import xyz.aerii.library.handlers.Observable
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.*
import java.awt.Color
import kotlin.math.PI
import kotlin.math.atan2

@Load
object RadialMenu : Module(
    "Radial menu",
    "Shows a cool radial menu with a ton of options for customisations!",
    Category.RENDER
) {
    private val keybind by config.keybind("Keybind", GLFW.GLFW_KEY_R)
    private val releaseClose by config.switch("Release to close", true)
    private val generalDirection by config.switch("General direction click")
    val subMenu by config.dropdown("Sub menu type", listOf("Full", "Mini", "Mini extended"))
    private val _unused by config.textParagraph("Enabling \"General direction click\" will make your clicks be on the slot closest to the cursor when it's not on a slot.")

    private val `color$normal` by config.colorPicker("Normal color", Color(Mocha.Surface0.withAlpha(0.5f), true))
    private val `color$hover` by config.colorPicker("Hover color", Color(Mocha.Mauve.withAlpha(0.5f), true))

    private val _unused0 by config.button("Open editor") {
        RadialEditor.open()
    }

    private val _unused1 by config.textParagraph("The configs can be exported/imported using the command <red>\"/athen radial [export|import]\"<r>. View all commands using <red>\"/athen radial help\"<r>!")

    private val stack = ArrayDeque<List<ISlot>>()

    private val current: List<ISlot>
        get() = stack.lastOrNull() ?: slots

    private var idx = -1
    private var idx0 = -1
    private var idx1 = -1

    private val scribble = Scribble("features/radialMenu")

    val slots = mutableListOf<ISlot>()
    val configs = mutableMapOf<String, List<SlotData>>()

    val open = Observable(false).onChange {
        if (it) return@onChange

        stack.clear()
        idx = -1
        idx0 = -1
        idx1 = -1
    }

    var active: String by scribble.string("active", "Default")
        private set

    var saved: String by scribble.string("configs")
        private set

    init {
        on<GameEvent.Start> {
            safely {
                val raw = saved.takeIf { it.isNotBlank() }
                if (raw != null) {
                    val map: Map<String, List<SlotData>> = GSON.fromJson(raw, object : TypeToken<Map<String, List<SlotData>>>() {}.type)
                    configs.clear()
                    configs.putAll(map)
                }

                if (configs.isEmpty()) configs["Default"] = emptyList()
                slots.clear()
                configs[active]?.map { it.toSlot() }?.let { slots.addAll(it) }
            }
        }

        on<GameEvent.Stop> {
            save()
            disk()
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("radial") {
                    callback {
                        help()
                    }

                    thenCallback("help") {
                        help()
                    }

                    thenCallback("edit") {
                        RadialEditor.open()
                    }
                }

                then("import") {
                    thenCallback("radial") {
                        val clipboard = McClient.clipboard
                        if (clipboard.isEmpty()) return@thenCallback "No data found in clipboard!".modMessage()

                        val map: Map<String, Any> = GSON.fromJson(clipboard.decompress(), object : TypeToken<Map<String, Any>>() {}.type)
                        val name = map["name"] as? String ?: return@thenCallback "Invalid config data!".modMessage()
                        val raw = GSON.toJson(map["slots"])
                        val data: List<SlotData> = GSON.fromJson(raw, object : TypeToken<List<SlotData>>() {}.type)

                        var n = name
                        var i = 1
                        while (n in configs) n = "$name ${++i}"

                        configs[n] = data
                        load(n)
                        disk()
                        "Imported config '$n' with ${data.size} slots!".modMessage()
                    }
                }

                then("export") {
                    thenCallback("radial") {
                        save()
                        val data = mapOf("name" to active, "slots" to slots.map { it.toData() })
                        McClient.clipboard = GSON.toJson(data).compress()
                        "Exported config '$active' to clipboard!".modMessage()
                    }
                }
            }
        }

        on<InputEvent.Keyboard.Press> {
            if (client.screen != null) return@on
            if (keyEvent.key != keybind) return@on

            react(if (releaseClose) true else !open.value, true)
        }

        on<InputEvent.Keyboard.Release> {
            if (client.screen != null) return@on
            if (keyEvent.key != keybind) return@on
            if (!open.value) return@on
            if (!releaseClose) return@on

            when {
                idx0 != -1 && idx1 in current.indices -> current[idx1].sub.getOrNull(idx0)?.action?.run()
                idx in current.indices -> current[idx].action.run()
            }

            react(false, bool = true)
        }

        on<InputEvent.Mouse.Press> {
            val cx = client.window.guiScaledWidth / 2
            val cy = client.window.guiScaledHeight / 2
            val dx = mouseSX - cx
            val dy = mouseSY - cy
            val a = stack.isNotEmpty()

            if (dx * dx + dy * dy < 225f) {
                if (subMenu == 2 && idx1 != -1) {
                    idx1 = -1
                    idx0 = -1
                    return@on cancel()
                }

                if (!a) {
                    react(false, bool = true)
                    return@on cancel()
                }

                stack.removeLast()
                idx = -1
                idx1 = -1
                return@on cancel()
            }

            if (buttonInfo.button() == 1 && a) {
                stack.removeLast()
                idx = -1
                idx1 = -1
                return@on cancel()
            }

            if (idx0 != -1 && idx1 in current.indices) {
                current[idx1].sub.getOrNull(idx0)?.action?.run()
                react(false, bool = true)
                return@on cancel()
            }

            val slot = current.getOrNull(idx) ?: return@on cancel()
            val b = slot.sub.isNotEmpty()

            if ((subMenu == 1 || subMenu == 2) && b) {
                idx1 = if (idx1 == idx) -1 else idx
                idx0 = -1
                return@on cancel()
            }

            if (subMenu == 0 && b) {
                stack.addLast(slot.sub)
                idx = slot(cx, cy, maxOf(3, stack.last().size), 50f, 80f)
                return@on cancel()
            }

            slot.action.run()
            react(false, bool = true)
            cancel()
        }.runWhen(open)

        on<InputEvent.Mouse.Move> {
            cancel()

            val cx = client.window.guiScaledWidth / 2
            val cy = client.window.guiScaledHeight / 2
            val dx = mouseSX - cx
            val dy = mouseSY - cy

            if (dx * dx + dy * dy < 225f) {
                idx = -1
                idx0 = -1
                return@on
            }

            if (subMenu == 2 && idx1 in current.indices) {
                val pairs = fn()
                val hit = SlotsRenderState.hitSub0(mouseSX, mouseSY, cx, cy, maxOf(3, current.size), 80f, pairs.map { it.first }, generalDirection)
                if (hit != -1) {
                    idx0 = hit
                    return@on
                }
            }

            if (subMenu == 1 && idx1 in current.indices) {
                val hit = SlotsRenderState.hitSub(mouseSX, mouseSY, cx, cy, maxOf(3, current.size), 80f, idx1, current[idx1].sub.size)
                if (hit != -1) {
                    idx = idx1
                    idx0 = hit
                    return@on
                }
            }

            idx0 = -1
            idx = slot(cx, cy, maxOf(3, current.size), 50f, 80f, subMenu == 2 && idx1 != -1)
        }.runWhen(open)

        on<GuiEvent.Render.Post> {
            val cx = graphics.guiWidth() / 2
            val cy = graphics.guiHeight() / 2
            val num = maxOf(3, current.size)

            val mini = if (subMenu == 1 && idx1 in current.indices) current[idx1].sub else emptyList()
            val ex = fn()
            val idx2 = if (subMenu == 2 && idx0 != -1) ex.getOrNull(idx0)?.first ?: -1 else -1

            graphics.guiRenderState.submitGuiElement(SlotsRenderState(graphics, cx, cy, num, 50f, 80f, `color$normal`.rgb, `color$hover`.rgb, idx, mini, idx0, idx1, ex, idx2))
            graphics.guiRenderState.nextStratum()

            for (i in current.indices) {
                val (sx, sy) = SlotsRenderState.centerSlot(cx, cy, num, 50f, 80f, i)
                graphics.renderItem(current[i].item, sx - 8, sy - 8)
            }

            if (subMenu == 1 && idx1 in current.indices) {
                for (j in current[idx1].sub.indices) {
                    val (sx, sy) = SlotsRenderState.centerSub(cx, cy, num, 80f, idx1, j)
                    graphics.renderItem(current[idx1].sub[j].item, sx - 8, sy - 8)
                }
            }

            if (subMenu == 2) {
                for ((i, s) in ex) {
                    val (sx, sy) = SlotsRenderState.centerSub0(cx, cy, num, 80f, i)
                    graphics.renderItem(s.item, sx - 8, sy - 8)
                }
            }

            val dx = mouseSX - cx
            val dy = mouseSY - cy
            val h = dx * dx + dy * dy < 144f
            val back = stack.isNotEmpty() || (subMenu == 2 && idx1 != -1)
            val str = if (back) "←" else "✕"

            graphics.drawRectangle(cx - 12, cy - 12, 24, 24, if (h) Mocha.Surface2.argb else Mocha.Surface1.argb)
            graphics.drawOutline(cx - 12, cy - 12, 24, 24, 1, if (h) Mocha.Mauve.argb else Mocha.Overlay0.argb)
            graphics.drawString(client.font, str, cx - client.font.width(str) / 2, cy - client.font.lineHeight / 2, if (h) Mocha.Mauve.argb else Mocha.Subtext0.argb, false)

            val sel = when {
                idx0 != -1 && idx1 in current.indices -> current[idx1].sub.getOrNull(idx0)
                else -> current.getOrNull(idx)
            }

            val label = if (h) (if (back) "Back" else "Exit") else sel?.name ?: return@on
            val tw = client.font.width(label)
            val mx = mouseSX.toInt() + 12
            val my = mouseSY.toInt() - 4

            graphics.drawRectangle(mx - 5, my - 5, tw + 10, client.font.lineHeight + 10, Mocha.Base.argb)
            graphics.drawOutline(mx - 5, my - 5, tw + 10, client.font.lineHeight + 10, 1, Mocha.Mauve.argb)
            graphics.text(label, mx, my, false, Mocha.Text.argb)
        }.runWhen(open)

        on<GuiEvent.Open.Any> {
            react(false, bool = false)
        }
    }

    fun disk() {
        saved = GSON.toJson(configs, object : TypeToken<Map<String, List<SlotData>>>() {}.type)
    }

    fun save() {
        configs[active] = slots.map { it.toData() }
    }

    fun load(name: String) {
        save()
        active = name
        slots.clear()
        configs[name]?.map { it.toSlot() }?.let { slots.addAll(it) }
    }

    fun add(name: String) {
        configs[name] = emptyList()
        load(name)
    }

    fun delete(name: String) {
        if (configs.size <= 1) return
        configs.remove(name)
        if (active != name) return
        active = configs.keys.first()
        slots.clear()
        configs[active]?.map { it.toSlot() }?.let { slots.addAll(it) }
    }

    fun rename(old: String, new: String) {
        val data = configs.remove(old) ?: return
        configs[new] = data
        if (active == old) active = new
    }

    private fun react(v: Boolean, bool: Boolean) {
        open.value = v
        if (v) client.mouseHandler.releaseMouse() else if (bool) client.mouseHandler.grabMouse()
    }

    private fun help() {
        val divider = ("§8§m" + "-".repeat()).literal()
        divider.lie()
        "§bRadial Menu §7[Athen]".center().lie()
        divider.lie()
        " <dark_gray>• <${Mocha.Green.argb}>/${Athen.modId} radial edit <gray>- Opens editor".parse().lie()
        " <dark_gray>• <${Mocha.Green.argb}>/${Athen.modId} import radial <gray>- Imports config from clipboard".parse().lie()
        " <dark_gray>• <${Mocha.Green.argb}>/${Athen.modId} export radial <gray>- Exports current config to clipboard".parse().lie()
        divider.lie()
        "Want to explore <red>presets<r>? Join the <hover:<red>Click to join!><click:url:${Athen.discordUrl}><${Mocha.Mauve.argb}>discord!".parse().lie()
        divider.lie()
    }

    private fun fn(): List<Pair<Int, ISlot>> {
        if (subMenu != 2 || idx1 !in current.indices) return emptyList()
        val sub = current[idx1].sub
        val n = sub.size
        val m = maxOf(3, current.size)
        val p = idx1

        return List(n) { i ->
            ((p - n / 2 + i + m * 2) % m) to sub[i]
        }
    }

    private fun slot(cx: Int, cy: Int, num: Int, inn: Float, out: Float, dir: Boolean = false): Int {
        if (num == 0) return -1

        val mx = mouseSX
        val my = mouseSY

        val inner = inn * 1.3f
        val outer = out * 1.3f
        val step = (PI * 2 / num).toFloat()
        val gap = Math.toRadians(4.0).toFloat()
        val off = (-PI / 2 - step / 2).toFloat()

        for (i in 0 until num) {
            val s = i * step + gap * .5f + off
            val e = (i + 1) * step - gap * .5f + off

            val c0 = Mth.cos(s/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val s0 = Mth.sin(s/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val c1 = Mth.cos(e/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val s1 = Mth.sin(e/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)

            val x0 = cx + outer * c0
            val y0 = cy + outer * s0
            val x1 = cx + inner * c0
            val y1 = cy + inner * s0
            val x2 = cx + inner * c1
            val y2 = cy + inner * s1
            val x3 = cx + outer * c1
            val y3 = cy + outer * s1

            if (tri(mx, my, x0, y0, x1, y1, x2, y2) || tri(mx, my, x0, y0, x2, y2, x3, y3)) return i
        }

        if (generalDirection && !dir) {
            val dx = mx - cx
            val dy = my - cy
            if (dx == 0f && dy == 0f) return -1
            var a = atan2(dy, dx) - off
            if (a < 0) a += (PI * 2).toFloat()
            return (a / step).toInt().coerceIn(0, num - 1)
        }

        return -1
    }
}