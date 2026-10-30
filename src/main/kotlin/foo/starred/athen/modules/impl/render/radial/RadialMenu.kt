@file:Suppress("ObjectPropertyName", "Unused")

package foo.starred.athen.modules.impl.render.radial

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.Athen
import foo.starred.athen.Athen.GSON
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GameEvent
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.InputEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.athen.modules.impl.render.radial.utils.RadialRenderState
import foo.starred.athen.utils.command
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.abstraction.input.impl.MouseInputState
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.data.Observable
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.storage.impl.profile.data.ProfileContainer
import foo.starred.kontext.api.storage.impl.profile.impl.ProfileJsonStore
import foo.starred.kontext.util.decompress
import java.lang.reflect.Type
import kotlin.math.hypot

@LoadableAthenModule
object RadialMenu : Module(
    "Radial menu",
    "Shows a cool radial menu with a ton of options for customisations!",
    ConfigCategory.RENDER
) {
    private val keybind by config.keybind("Keybind", InputConstants.KEY_R)
    private val releaseClose by config.switch("Release to close", true)
    val direction by config.switch("General direction click").description("Enabling this will make your clicks be on the slot closest to the cursor.")
    val type by config.selector("Sub menu type", listOf("Full", "Mini", "Mini extended"))
    val radius1 by config.slider("Inner radius", 50f, 20f, 120f, "pixels")
    val radius2 by config.slider("Outer radius", 80f, 40f, 180f, "pixels")
    val thickness by config.slider("Sub thickness", 18f, 8f, 40f, "pixels")

    val `color$normal` by config.colorPicker("Normal color", MochaColorScheme.Surface0.alpha(0.5f))
    val `color$hover` by config.colorPicker("Hover color", MochaColorScheme.Lavender.alpha(0.5f))

    private val _unused0 by config.button("Open editor") {
        RadialEditor.open()
    }

    private val _unused1 by config.information("Command: <red>/athen radial help")

    private val gsonType: Type = object : TypeToken<List<RadialSlot>>() {}.type
    private val gson: Gson = GSON.newBuilder()
        .registerTypeAdapter(
            TypeToken.getParameterized(ProfileContainer::class.java, gsonType).type,
            JsonDeserializer { json, _, context ->
                val root = json.asJsonObject
                val data = root.getAsJsonObject("@${Athen.Data.id}:data") ?: root
                val profiles = mutableMapOf<String, List<RadialSlot>>()
                val element = data.get("profiles") ?: data.get("configs")

                val container = when {
                    element == null -> null
                    element.isJsonObject -> element.asJsonObject
                    element.isJsonPrimitive && element.asJsonPrimitive.isString -> runCatching { JsonParser.parseString(element.asString).asJsonObject }.getOrNull()
                    else -> null
                }

                container?.entrySet()?.forEach { (name, value) ->
                    val list = context.deserialize<List<RadialSlot>>(value, gsonType) ?: return@forEach
                    profiles[name] = list
                }

                if (profiles.isEmpty()) {
                    profiles["Default"] = emptyList()
                }

                ProfileContainer(data.get("active")?.asString ?: "Default", profiles)
            }
        )
        .create()

    private val stack = ArrayDeque<List<RadialSlot>>()
    val store = ProfileJsonStore<List<RadialSlot>>(Athen.Data.id, "features/radialMenu", gsonType, ::emptyList, gson)
    val slots = mutableListOf<RadialSlot>()

    val current: List<RadialSlot>
        get() = stack.lastOrNull() ?: slots

    val open = Observable(false).observe {
        if (it) return@observe

        stack.clear()
        i0 = -1
        i1 = -1
        i2 = -1
    }

    var i0 = -1
        private set

    var i1 = -1
        private set

    var i2 = -1
        private set

    init {
        fill(store.active)

        command {
            "radial" {
                help()
            }

            "radial" / "help" {
                help()
            }

            "radial" / "edit" {
                RadialEditor.open()
            }

            "import" / "radial" {
                val clipboard = MinecraftClient.clipboard
                if (clipboard.isEmpty()) return@invoke "No data found in clipboard!".mod()

                val imported = store.import(clipboard) ?: runCatching {
                    val json = clipboard.decompress()
                    val map = GSON.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                    val name = map["name"] as? String ?: return@runCatching null
                    val raw = GSON.toJson(map["slots"] ?: map["data"])
                    val data = GSON.fromJson<List<RadialSlot>>(raw, gsonType) ?: return@runCatching null

                    var target = name
                    var index = 1
                    while (target in store.profiles) {
                        target = "$name ${++index}"
                    }

                    store.add(target, data)
                    target
                }.getOrNull() ?: return@invoke "Invalid config data!".mod()

                load(imported)
                disk()
                "Imported config '$imported' with ${slots.size} slots!".mod()
            }

            "export" / "radial" {
                save()
                disk()
                MinecraftClient.clipboard = store.export()
                "Exported config '${store.active}' to clipboard!".mod()
            }
        }

        on<GameEvent.Stop> {
            save()
            disk()
        }

        on<InputEvent.Keyboard.Press> {
            if (MinecraftGui.screen != null) return@on
            if (keyEvent.key != keybind.value) return@on

            react(releaseClose || !open.value, true)
        }

        on<InputEvent.Keyboard.Release> {
            if (MinecraftGui.screen != null) return@on
            if (keyEvent.key != keybind.value) return@on
            if (!open.value) return@on
            if (!releaseClose) return@on

            trigger()
            react(false, bool = true)
        }

        on<InputEvent.Mouse.Press> {
            if (MinecraftGui.screen != null) return@on
            if (buttonInfo.button() != keybind.value) return@on
            if (open.value) return@on

            react(releaseClose || !open.value, true)
        }

        on<InputEvent.Mouse.Release> {
            if (MinecraftGui.screen != null) return@on
            if (buttonInfo.button() != keybind.value) return@on
            if (!open.value) return@on
            if (!releaseClose) return@on

            trigger()
            react(false, bool = true)
        }

        on<InputEvent.Mouse.Press> {
            val x1 = client.window.guiScaledWidth / 2
            val y1 = client.window.guiScaledHeight / 2

            if (dist(x1, y1) < 15f) {
                if (type == 2 && i2 != -1) {
                    i2 = -1
                    i1 = -1
                    return@on cancel()
                }

                if (stack.isEmpty()) {
                    react(false, bool = true)
                    return@on cancel()
                }

                back()
                return@on cancel()
            }

            if (buttonInfo.button() == 1 && stack.isNotEmpty()) {
                back()
                return@on cancel()
            }

            if (i1 != -1 && i2 in current.indices) {
                current[i2].sub.getOrNull(i1)?.action?.run()
                react(false, bool = true)
                return@on cancel()
            }

            val slot = current.getOrNull(i0) ?: return@on cancel()

            if ((type == 1 || type == 2) && slot.sub.isNotEmpty()) {
                i2 = if (i2 == i0) -1 else i0
                i1 = -1
                return@on cancel()
            }

            if (type == 0 && slot.sub.isNotEmpty()) {
                stack.addLast(slot.sub)
                i0 = RadialRenderState.hit(MouseInputState.Position.Scaled.x, MouseInputState.Position.Scaled.y, x1, y1, maxOf(1, stack.last().size), radius1, radius2)
                return@on cancel()
            }

            slot.action.run()
            react(false, bool = true)
            cancel()
        }.runWhen(open)

        on<InputEvent.Mouse.Move> {
            cancel()

            val x0 = MouseInputState.Position.Scaled.x
            val y0 = MouseInputState.Position.Scaled.y

            val x1 = client.window.guiScaledWidth / 2
            val y1 = client.window.guiScaledHeight / 2

            if (dist(x1, y1) < 15f) {
                i0 = -1
                i1 = -1
                return@on
            }

            if (type == 2 && i2 in current.indices) {
                val ring = layout()
                val hit = RadialRenderState.hitRing(x0, y0, x1, y1, maxOf(1, current.size), radius2, ring.map { it.first }, direction, thickness)
                if (hit != -1) {
                    i1 = hit
                    return@on
                }
            }

            if (type == 1 && i2 in current.indices) {
                val hit = RadialRenderState.hitNested(x0, y0, x1, y1, maxOf(1, current.size), radius2, i2, current[i2].sub.size, direction, thickness)
                if (hit != -1) {
                    i0 = i2
                    i1 = hit
                    return@on
                }
            }

            i1 = -1
            i0 = RadialRenderState.hit(x0, y0, x1, y1, maxOf(1, current.size), radius1, radius2, direction || (type == 2 && i2 != -1))
        }.runWhen(open)

        on<GuiEvent.Render.Any.Post> {
            val x = graphics.guiWidth() / 2
            val y = graphics.guiHeight() / 2
            val size = maxOf(1, current.size)

            val mini = if (type == 1 && i2 in current.indices) current[i2].sub else emptyList()
            val ring = layout()
            val ringIndex = if (type == 2 && i1 != -1) ring.getOrNull(i1)?.first ?: -1 else -1

            graphics.guiRenderState.addGuiElement(RadialRenderState(graphics, x, y, size, mini, ring, i3 = ringIndex))
            graphics.guiRenderState.nextStratum()

            for (i in current.indices) {
                val (x, y) = RadialRenderState.anchor(x, y, size, radius1, radius2, i)
                graphics.item(current[i].item, x - 8, y - 8)
            }

            if (type == 1 && i2 in current.indices) {
                for (i in current[i2].sub.indices) {
                    val (x, y) = RadialRenderState.nested(x, y, size, radius2, i2, i, thickness)
                    graphics.item(current[i2].sub[i].item, x - 8, y - 8)
                }
            }

            if (type == 2) {
                for ((i, value) in ring) {
                    val (x, y) = RadialRenderState.ring(x, y, size, radius2, i, thickness)
                    graphics.item(value.item, x - 8, y - 8)
                }
            }

            val bool0 = dist(x, y) < 15f
            val bool1 = stack.isNotEmpty() || (type == 2 && i2 != -1)

            MinecraftFontRenderer.extract(graphics, if (bool1) "←" else "✕", x - MinecraftFontMeasurer.width(if (bool1) "←" else "✕") / 2, y - MinecraftFontMeasurer.height / 2, false, if (bool0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)

            val hovered = when {
                i1 != -1 && i2 in current.indices -> current[i2].sub.getOrNull(i1)
                else -> current.getOrNull(i0)
            }

            val label = if (bool0) (if (bool1) "Back" else "Exit") else hovered?.name ?: return@on
            val x1 = MouseInputState.Position.Scaled.x.toInt() + 12
            val y1 = MouseInputState.Position.Scaled.y.toInt() - 4

            graphics.rectangle(x1 - 5f, y1 - 5f, MinecraftFontMeasurer.width(label) + 10f, MinecraftFontMeasurer.height + 10f, MochaColorScheme.Base.argb)
            MinecraftFontRenderer.extract(graphics, label, x1, y1, false, MochaColorScheme.Text.argb)
        }.runWhen(open)

        on<GuiEvent.Open.Any> {
            react(false, bool = false)
        }
    }

    fun disk() {
        save()
        store.save()
    }

    fun save() {
        store.profiles[store.active] = slots.map { it.clone() }
    }

    fun load(name: String) {
        save()
        store.select(name)
        fill(name)
    }

    fun add(name: String) {
        save()
        store.add(name, emptyList())
        fill(name)
    }

    fun delete(name: String) {
        if (store.profiles.size <= 1) return

        store.delete(name)
        fill(store.active)
    }

    fun rename(old: String, new: String) {
        if (old == new) return
        if (new.isBlank()) return
        if (store.profiles.containsKey(new)) return

        store.rename(old, new)
    }

    private fun fill(name: String) {
        slots.clear()

        store.profiles[name]?.map { it.clone() }?.let {
            slots.addAll(it)
        }
    }

    private fun dist(x1: Int, y1: Int): Double {
        return hypot((MouseInputState.Position.Scaled.x - x1).toDouble(), (MouseInputState.Position.Scaled.y - y1).toDouble())
    }

    private fun back() {
        stack.removeLast()
        i0 = -1
        i2 = -1
    }

    private fun trigger() {
        when {
            i1 != -1 && i2 in current.indices -> current[i2].sub.getOrNull(i1)?.action?.run()
            i0 in current.indices -> current[i0].action.run()
        }
    }

    private fun react(visible: Boolean, bool: Boolean) {
        open.value = visible

        if (visible) {
            client.mouseHandler.releaseMouse()
            return
        }

        if (bool) {
            client.mouseHandler.grabMouse()
        }
    }

    private fun help() {
        val divider = ("§8§m" + MinecraftChat.repeat("-")).literal()
        MinecraftMessages.display(divider)
        MinecraftMessages.display(MinecraftChat.center("§bRadial Menu §7[Athen]"))
        MinecraftMessages.display(divider)
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen radial edit <gray>- Opens editor")
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen import radial <gray>- Imports config from clipboard")
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen export radial <gray>- Exports current config to clipboard")
        MinecraftMessages.display(divider)
        MinecraftMessages.display("Want to explore <red>presets<r>? Join the <hover:<red>Click to join!><click:url:${Athen.Data.discord}><${MochaColorScheme.Lavender.argb}>discord!")
        MinecraftMessages.display(divider)
    }

    private fun layout(): List<Pair<Int, RadialSlot>> {
        if (type != 2) return emptyList()
        if (i2 !in current.indices) return emptyList()

        val sub = current[i2].sub
        val count = sub.size
        val total = maxOf(1, current.size)
        val parent = i2

        return List(count) { index0 ->
            ((parent - count / 2 + index0 + total * 2) % total) to sub[index0]
        }
    }
}
