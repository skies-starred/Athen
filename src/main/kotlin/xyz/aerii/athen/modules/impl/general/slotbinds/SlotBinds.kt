@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general.slotbinds

import com.google.gson.reflect.TypeToken
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.inventory.ClickType
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.Athen
import xyz.aerii.athen.Athen.GSON
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.PlayerEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.guiClick
import xyz.aerii.athen.utils.render.Render2D.drawLine
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.client
import xyz.aerii.library.api.pressed
import xyz.aerii.library.utils.compress
import xyz.aerii.library.utils.decompress
import xyz.aerii.library.utils.safely

@Load
object SlotBinds : Module(
    "Slot binds",
    "Bindings for slots!",
    Category.GENERAL
) {
    private val _unused0 by config.textParagraph("You can use the commands <red>\"/${Athen.modId} [import|export] slotbinds\"<r> to share configs!")
    private val bind by config.keybind("Bind keybind", GLFW.GLFW_KEY_B)
    private val swap by config.keybind("Swap keybind", GLFW.GLFW_KEY_LEFT_SHIFT)
    private val lock = config.switch("Lock bound slots").custom("lock")
    private val _unused1 by config.button("Open editor") { SlotBindsGUI.open() }

    private var last0: Int? = null
    private var last1: Int = 0

    private val scribble = Scribble("features/slotBinds")
    var active: String by scribble.string("active", "Default")
    var saved: String by scribble.string("profiles")

    val m0 = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
    val m1 = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
    val m2 = Int2IntOpenHashMap().apply { defaultReturnValue(Catppuccin.Mocha.Mauve.argb) }
    val map0 = mutableMapOf<String, Int2IntOpenHashMap>()
    val map1 = mutableMapOf<String, Int2IntOpenHashMap>()

    val palette = intArrayOf(
        Catppuccin.Mocha.Mauve.argb,
        Catppuccin.Mocha.Sapphire.argb,
        Catppuccin.Mocha.Green.argb,
        Catppuccin.Mocha.Peach.argb,
        Catppuccin.Mocha.Pink.argb,
        Catppuccin.Mocha.Teal.argb,
        Catppuccin.Mocha.Red.argb,
        Catppuccin.Mocha.Yellow.argb,
        Catppuccin.Mocha.Sky.argb,
        Catppuccin.Mocha.Lavender.argb,
    )

    init {
        on<GameEvent.Start> {
            safely {
                val a = saved.takeIf { it.isNotBlank() }
                if (a != null) {
                    val kv = GSON.fromJson(a, object : TypeToken<Map<String, Map<String, Any>>>() {}.type) as Map<String, Map<String, Any>>
                    map0.clear()
                    map1.clear()

                    for ((k, v) in kv) {
                        val m0 = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
                        val m1 = Int2IntOpenHashMap().apply { defaultReturnValue(palette[0]) }

                        val binds = v["binds"] as? Map<*, *> ?: emptyMap<String, Any>()
                        for ((a, b) in binds) (a as? String)?.toIntOrNull()?.let { m0.put(it, (b as Number).toInt()) }

                        val colors = v["colors"] as? Map<*, *> ?: emptyMap<String, Any>()
                        for ((a, b) in colors) (a as? String)?.toIntOrNull()?.let { m1.put(it, (b as Number).toInt()) }

                        map0[k] = m0
                        map1[k] = m1
                    }
                }

                if (map0.isEmpty()) {
                    map0["Default"] = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
                }

                if (!map0.containsKey(active)) {
                    active = map0.keys.first()
                }

                sync(map0[active] ?: Int2IntOpenHashMap(), map1[active])
            }
        }

        on<GameEvent.Stop> {
            save()
            disk()
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("slotbinds") {
                    callback {
                        SlotBindsGUI.open()
                    }

                    thenCallback("gui") {
                        SlotBindsGUI.open()
                    }
                }

                then("import") {
                    thenCallback("slotbinds") {
                        val clipboard = McClient.clipboard
                        if (clipboard.isEmpty()) return@thenCallback "No data found in clipboard!".modMessage()

                        val data = GSON.fromJson(clipboard.decompress(), object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                        val name = data["name"] as? String ?: return@thenCallback "Invalid config data!".modMessage()

                        val raw = GSON.toJson(data["binds"])
                        val binds = GSON.fromJson(raw, object : TypeToken<Map<String, Double>>() {}.type) as Map<String, Double>

                        val m = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
                        for ((k, v) in binds) k.toIntOrNull()?.let { m.put(it, v.toInt()) }

                        val cm = Int2IntOpenHashMap().apply { defaultReturnValue(palette[0]) }
                        val c = data["colors"]
                        if (c != null) {
                            val colors = GSON.fromJson(GSON.toJson(c), object : TypeToken<Map<String, Double>>() {}.type) as Map<String, Double>
                            for ((k, v) in colors) k.toIntOrNull()?.let { cm.put(it, v.toInt()) }
                        }

                        var n = name
                        var i = 1
                        while (n in map0) n = "$name ${++i}"

                        map0[n] = m
                        map1[n] = cm

                        load(n)
                        disk()
                        "Imported profile '$n' with ${m.size} binds!".modMessage()
                    }
                }

                then("export") {
                    thenCallback("slotbinds") {
                        save()

                        val binds = mutableMapOf<String, Int>()
                        for (e in (map0[active] ?: return@thenCallback).int2IntEntrySet()) binds[e.intKey.toString()] = e.intValue

                        val colors = mutableMapOf<String, Int>()
                        val c = map1[active]
                        if (c != null) for (e in c.int2IntEntrySet()) colors[e.intKey.toString()] = e.intValue

                        McClient.clipboard = GSON.toJson(mapOf("name" to active, "binds" to binds, "colors" to colors)).compress()
                        "Exported profile '$active' to clipboard!".modMessage()
                    }
                }
            }
        }

        on<GuiEvent.Slots.Click> {
            val s = client.screen as? InventoryScreen ?: return@on
            val h = slot?.index ?: return@on

            val f = m0.get(h)
            val r = if (f == -1) m1.get(h) else -1
            if (lock.value && (f != -1 || r != -1)) cancel()

            val b0 = bind.bound && bind.pressed
            val b1 = swap.bound && swap.pressed
            if (!b0 && !b1) return@on

            if (b0) {
                cancel()

                val l = last0
                if (l != null) {
                    last0 = null
                    if (l == h) return@on

                    val b2 = l.fn()
                    val b3 = h.fn()
                    if (b2 == b3) return@on

                    val a = if (b2) h else l
                    val b = if (b2) l else h

                    put(a, b)

                    return@on
                }

                val f = m0.get(h)
                if (f != -1) {
                    rk(h)
                    return@on
                }

                val r = m1.get(h)
                if (r != -1) {
                    rv(h)
                    return@on
                }

                last0 = h
                return@on
            }

            if (mouseButton != 0) return@on
            val b = m0.get(h).takeIf { it != -1 } ?: m1.get(h)
            if (b == -1) return@on

            val b3 = h.fn()
            if (!b3 && !b.fn()) return@on

            val c = if (b3) b else h
            val d = (if (b3) h else b) - 36

            guiClick(s.menu.containerId, c, d, ClickType.SWAP)
            cancel()
        }

        on<GuiEvent.Slots.Render.Post> {
            val s = client.screen as? InventoryScreen ?: return@on
            val m = s.menu.slots
            if (slot != m.last()) return@on

            for (e in m0.int2IntEntrySet()) {
                val a = m.getOrNull(e.intKey) ?: continue
                val b = m.getOrNull(e.intValue) ?: continue
                val c = m2.get(e.intKey)

                graphics.drawLine(a.x + 8, a.y + 8, b.x + 8, b.y + 8, c, 1)

                graphics.drawOutline(a.x, a.y, 16, 16, 1, c, true)
                graphics.drawOutline(b.x, b.y, 16, 16, 1, c, true)
            }

            val l = last0 ?: return@on
            val a = m.getOrNull(l) ?: return@on

            graphics.drawOutline(a.x, a.y, 16, 16, 1, inset = true)
        }

        on<PlayerEvent.Drop> {
            val h = client.player?.inventory?.selectedSlot?.plus(36) ?: return@on
            if (h.get() == -1) return@on

            cancel()
        }.runWhen(lock.state)
    }

    fun sc(slot: Int): Int {
        if (m0.containsKey(slot)) return m2.get(slot)
        val k = m1.get(slot)
        if (k != -1) return m2.get(k)
        return palette[0]
    }

    fun cycle(slot: Int) {
        val k = if (m0.containsKey(slot)) slot else m1.get(slot).takeIf { it != -1 } ?: return
        val cur = m2.get(k)
        val idx = palette.indices.firstOrNull { palette[it] == cur } ?: -1
        m2.put(k, palette[(idx + 1) % palette.size])
    }

    fun bind(a: Int, b: Int) {
        if (a.fn() == b.fn()) return
        val k = if (a.fn()) b else a
        val v = if (a.fn()) a else b
        put(k, v)
    }

    fun unbind(slot: Int) {
        if (m0.containsKey(slot)) rk(slot)
        else if (m1.containsKey(slot)) rv(slot)
    }

    fun save() {
        map0[active] = Int2IntOpenHashMap(m0).apply { defaultReturnValue(-1) }
        map1[active] = Int2IntOpenHashMap(m2).apply { defaultReturnValue(palette[0]) }
    }

    fun disk() {
        val a = mutableMapOf<String, Any>()

        for ((k, v) in map0) {
            val binds = mutableMapOf<String, Int>()
            for (e in v.int2IntEntrySet()) binds[e.intKey.toString()] = e.intValue
            val colors = mutableMapOf<String, Int>()
            map1[k]?.let { cm -> for (e in cm.int2IntEntrySet()) colors[e.intKey.toString()] = e.intValue }
            a[k] = mapOf("binds" to binds, "colors" to colors)
        }

        saved = GSON.toJson(a)
    }

    fun load(name: String) {
        save()
        active = name
        sync(map0[name] ?: Int2IntOpenHashMap(), map1[name])
    }

    fun add(name: String) {
        map0[name] = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
        map1[name] = Int2IntOpenHashMap().apply { defaultReturnValue(palette[0]) }
        load(name)
    }

    fun delete(name: String) {
        if (map0.size <= 1) return
        map0.remove(name)
        map1.remove(name)

        if (active != name) return
        active = map0.keys.first()

        sync(map0[active] ?: Int2IntOpenHashMap(), map1[active])
    }

    private fun rk(k: Int) {
        val v = m0.remove(k)
        if (v != -1) m1.remove(v)
        m2.remove(k)
    }

    private fun rv(v: Int) {
        val k = m1.remove(v)
        if (k == -1) return

        m0.remove(k)
        m2.remove(k)
    }

    private fun put(k: Int, v: Int) {
        rk(k)
        rv(v)

        m0.put(k, v)
        m1.put(v, k)
        m2.put(k, palette[last1++ % palette.size])
    }

    private fun Int.get(): Int {
        val v = m0.get(this)
        if (v != -1) return v

        return m1.get(this)
    }

    private fun sync(a: Int2IntOpenHashMap, b: Int2IntOpenHashMap?) {
        m0.clear()
        m1.clear()
        m2.clear()

        for (e in a.int2IntEntrySet()) {
            m0.put(e.intKey, e.intValue)
            m1.put(e.intValue, e.intKey)
        }

        if (b != null) for (e in b.int2IntEntrySet()) m2.put(e.intKey, e.intValue)
        last1 = m2.size
    }

    private fun Int.fn(): Boolean {
        return this in 36..44
    }
}