@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general.slotbinds.impl

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.Athen
import foo.starred.athen.Athen.GSON
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GameEvent
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.PlayerEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.general.slotbinds.data.SlotBindsProfileData
import foo.starred.athen.modules.impl.general.slotbinds.gui.SlotBindsGUI
import foo.starred.athen.utils.command
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.stroke.stroke
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.kontext.api.storage.impl.profile.data.ProfileContainer
import foo.starred.kontext.api.storage.impl.profile.impl.ProfileJsonStore
import foo.starred.kontext.util.decompress
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import org.joml.Matrix3x2f
import kotlin.collections.iterator

@LoadableAthenModule
object SlotBinds : Module(
    "Slot binds",
    "Bindings for slots!",
    GENERAL
) {
    private val bind by config.keybind("Bind keybind", InputConstants.KEY_B)
    private val swap by config.keybind("Swap keybind", InputConstants.KEY_LSHIFT)
    private val lock = config.switch("Lock bound slots").unique("lock")
    private val _unused0 by config.button("Open editor") { SlotBindsGUI.open() }
    private val _unused1 by config.information("Share command: <red>/athen [import|export] slotbinds")
    private val _unused2 by config.information("Swap command: <red>/athen slotbinds profile swap [profile]")

    private val gson: Gson = GSON.newBuilder()
        .registerTypeAdapter(
            TypeToken.getParameterized(ProfileContainer::class.java, SlotBindsProfileData::class.java).type,
            JsonDeserializer { json, _, context ->
                val root = json.asJsonObject
                val data = root.getAsJsonObject("@${Athen.Data.id}:data") ?: root
                val profiles = mutableMapOf<String, SlotBindsProfileData>()
                val element = data.get("profiles")

                val container = when {
                    element == null -> null
                    element.isJsonObject -> element.asJsonObject
                    element.isJsonPrimitive && element.asJsonPrimitive.isString -> runCatching { JsonParser.parseString(element.asString).asJsonObject }.getOrNull()
                    else -> null
                }

                container?.entrySet()?.forEach { (name, value) ->
                    val profile = context.deserialize<SlotBindsProfileData>(value, SlotBindsProfileData::class.java) ?: return@forEach
                    profiles[name] = profile
                }

                if (profiles.isEmpty()) {
                    profiles["Default"] = SlotBindsProfileData()
                }

                ProfileContainer(data.get("active")?.asString ?: "Default", profiles)
            }
        )
        .create()

    private var last0: Int? = null
    private var last1: Int = 0

    val m0 = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
    val m1 = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }
    val m2 = Int2IntOpenHashMap().apply { defaultReturnValue(MochaColorScheme.Mauve.argb) }

    val store = ProfileJsonStore(Athen.Data.id, "features/slotBinds", SlotBindsProfileData::class.java, ::SlotBindsProfileData, gson)

    val palette = intArrayOf(
        MochaColorScheme.Mauve.argb,
        MochaColorScheme.Sapphire.argb,
        MochaColorScheme.Green.argb,
        MochaColorScheme.Peach.argb,
        MochaColorScheme.Pink.argb,
        MochaColorScheme.Teal.argb,
        MochaColorScheme.Red.argb,
        MochaColorScheme.Yellow.argb,
        MochaColorScheme.Sky.argb,
        MochaColorScheme.Lavender.argb
    )

    init {
        sync(store.current)

        on<GameEvent.Stop> {
            disk()
        }

        command {
            "slotbinds" {
                SlotBindsGUI.open()
            }

            "slotbinds" / "gui" {
                SlotBindsGUI.open()
            }

            "slotbinds" / "profile" / "swap" / greedyString("name") {
                val name = string("name")
                if (name !in store.profiles) return@greedyString "Profile <red>\"$name\"<r> does not exist!".mod()
                if (name == store.active) return@greedyString "Already on profile <red>\"$name\"<r>!".mod()

                load(name)
                "Swapped to profile <green>$name<r>!".mod()
            }

            "export" / "slotbinds" {
                save()
                disk()
                MinecraftClient.clipboard = store.export()
                "Exported profile '${store.active}' to clipboard!".mod()
            }

            "import" / "slotbinds" {
                val clipboard = MinecraftClient.clipboard
                if (clipboard.isEmpty()) return@invoke "No data found in clipboard!".mod()

                val imported = store.import(clipboard) ?: runCatching {
                    val json = clipboard.decompress()
                    val data = GSON.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                    val name = data["name"] as? String ?: return@runCatching null

                    val binds0 = GSON.toJson(data["binds"])
                    val binds = GSON.fromJson<Map<String, Double>>(binds0, object : TypeToken<Map<String, Double>>() {}.type) ?: emptyMap()

                    val colors0 = GSON.toJson(data["colors"] ?: emptyMap<String, Double>())
                    val colors = GSON.fromJson<Map<String, Double>>(colors0, object : TypeToken<Map<String, Double>>() {}.type) ?: emptyMap()
                    val profile = SlotBindsProfileData(binds.mapValues { it.value.toInt() }.toMutableMap(), colors.mapValues { it.value.toInt() }.toMutableMap())

                    var target = name
                    var index = 1
                    while (target in store.profiles) {
                        target = "$name ${++index}"
                    }

                    store.add(target, profile)
                    target
                }.getOrNull() ?: return@invoke "Invalid config data!".mod()

                sync(store.current)
                disk()
                "Imported profile '$imported' with ${m0.size} binds!".mod()
            }
        }

        on<GuiEvent.Slots.Input.Click> {
            val screen = MinecraftGui.screen as? InventoryScreen ?: return@on
            val current = slot?.index ?: return@on

            val target0 = m0.get(current)
            val reverse0 = if (target0 == -1) m1.get(current) else -1
            if (lock.value) {
                if (target0 != -1) cancel()
                if (reverse0 != -1) cancel()
            }

            val bound0 = GenericInputState.pressed(bind)
            val swap0 = GenericInputState.pressed(swap)
            if (!bound0 && !swap0) return@on

            if (bound0) {
                cancel()

                val previous = last0
                if (previous != null) {
                    last0 = null
                    if (previous == current) return@on

                    val hotbar0 = previous.hotbar()
                    val hotbar1 = current.hotbar()
                    if (hotbar0 == hotbar1) return@on

                    val key = if (hotbar0) current else previous
                    val value = if (hotbar0) previous else current

                    put(key, value)
                    return@on
                }

                if (target0 != -1) {
                    rk(current)
                    return@on
                }

                if (reverse0 != -1) {
                    rv(current)
                    return@on
                }

                last0 = current
                return@on
            }

            if (mouseButton != InputConstants.MOUSE_BUTTON_LEFT) return@on
            val target1 = target0.takeIf { it != -1 } ?: reverse0
            if (target1 == -1) return@on

            val hotbar = current.hotbar()
            if (!hotbar && !target1.hotbar()) return@on

            val source = if (hotbar) target1 else current
            val destination = if (hotbar) current else target1
            val button = destination - 36

            val slots = screen.menu.slots
            val empty0 = slots.getOrNull(source)?.item?.isEmpty != false
            val empty1 = slots.getOrNull(destination)?.item?.isEmpty != false
            if (empty0 && empty1) return@on

            m1.put(destination, source)
            MinecraftGui.click(screen.menu.containerId, source, button, SWAP)
            cancel()
        }

        on<GuiEvent.Slots.Render.Menu.End> {
            val screen = MinecraftGui.screen as? InventoryScreen ?: return@on
            val slots = screen.menu.slots

            val pose = Matrix3x2f(graphics.pose())
            val scissor = graphics.scissorStack.peek()

            for (entry in m0.int2IntEntrySet()) {
                val slot0 = slots.getOrNull(entry.intKey) ?: continue
                val slot1 = slots.getOrNull(entry.intValue) ?: continue
                val color0 = m2.get(entry.intKey)

                val x0 = slot0.x.toFloat()
                val y0 = slot0.y.toFloat()
                val x1 = slot1.x.toFloat()
                val y1 = slot1.y.toFloat()

                graphics.stroke(x0 + 8f, y0 + 8f, x1 + 8f, y1 + 8f, color0, 1f, false, pose, scissor)

                graphics.hollowRectangle(x0, y0, 16f, 16f, 1f, color0, pose = pose, scissor = scissor)
                graphics.hollowRectangle(x1, y1, 16f, 16f, 1f, color0, pose = pose, scissor = scissor)
            }

            val selected = last0 ?: return@on
            val selectedSlot = slots.getOrNull(selected) ?: return@on

            graphics.hollowRectangle(selectedSlot.x.toFloat(), selectedSlot.y.toFloat(), 16f, 16f, 1f, CascadeGeometricColor.WHITE)
        }

        on<PlayerEvent.Drop> {
            val slot = client.player?.inventory?.selectedSlot?.plus(36) ?: return@on
            if (bound(slot) == -1) return@on

            cancel()
        }.runWhen(lock.state)
    }

    fun sc(slot: Int): Int {
        if (m0.containsKey(slot)) return m2.get(slot)

        val key = m1.get(slot)
        if (key != -1) return m2.get(key)

        return palette[0]
    }

    fun cycle(slot: Int) {
        val key = if (m0.containsKey(slot)) slot else m1.get(slot).takeIf { it != -1 } ?: return
        val current = m2.get(key)

        val index0 = palette.indices.firstOrNull { palette[it] == current } ?: -1
        m2.put(key, palette[(index0 + 1) % palette.size])
    }

    fun bind(slot0: Int, slot1: Int) {
        if (slot0.hotbar() == slot1.hotbar()) return

        val key = if (slot0.hotbar()) slot1 else slot0
        val value = if (slot0.hotbar()) slot0 else slot1
        put(key, value)
    }

    fun unbind(slot: Int) {
        if (m0.containsKey(slot)) {
            rk(slot)
            return
        }

        if (m1.containsKey(slot)) {
            rv(slot)
        }
    }

    fun save() {
        val binds = mutableMapOf<String, Int>()
        for (entry in m0.int2IntEntrySet()) {
            binds[entry.intKey.toString()] = entry.intValue
        }

        val colors = mutableMapOf<String, Int>()
        for (entry in m2.int2IntEntrySet()) {
            colors[entry.intKey.toString()] = entry.intValue
        }

        store.profiles[store.active] = SlotBindsProfileData(binds, colors)
    }

    fun disk() {
        save()
        store.save()
    }

    fun load(name: String) {
        save()
        store.select(name)
        sync(store.current)
    }

    fun add(name: String) {
        save()
        store.add(name, SlotBindsProfileData())
        sync(store.current)
    }

    fun delete(name: String) {
        if (store.profiles.size <= 1) return

        store.delete(name)
        sync(store.current)
    }

    fun rename(old: String, new: String) {
        if (old == new) return
        if (new.isBlank()) return
        if (store.profiles.containsKey(new)) return

        store.rename(old, new)
    }

    private fun sync(profile: SlotBindsProfileData) {
        m0.clear()
        m1.clear()
        m2.clear()

        for ((key, value) in profile.binds) {
            val slot = key.toIntOrNull() ?: continue

            m0.put(slot, value)
            m1.put(value, slot)
        }

        for ((key, color) in profile.colors) {
            val slot = key.toIntOrNull() ?: continue

            m2.put(slot, color)
        }

        last1 = m2.size
    }

    private fun rk(slot: Int) {
        val target = m0.remove(slot)
        m2.remove(slot)
        if (target == -1) return
        if (m1.get(target) != slot) return

        val entry = m0.int2IntEntrySet().firstOrNull { it.intValue == target }
        if (entry != null) {
            m1.put(target, entry.intKey)
            return
        }

        m1.remove(target)
    }

    private fun rv(target: Int) {
        val slot = m1.get(target)
        if (slot == -1) return

        m0.remove(slot)
        m2.remove(slot)

        val entry = m0.int2IntEntrySet().firstOrNull { it.intValue == target }
        if (entry != null) {
            m1.put(target, entry.intKey)
            return
        }

        m1.remove(target)
    }

    private fun put(slot: Int, target: Int) {
        rk(slot)

        m0.put(slot, target)
        m1.put(target, slot)
        m2.put(slot, palette[last1++ % palette.size])
    }

    private fun bound(slot: Int): Int {
        val target = m0.get(slot)
        if (target != -1) return target

        return m1.get(slot)
    }

    private fun Int.hotbar(): Boolean {
        return this in 36..44
    }
}
