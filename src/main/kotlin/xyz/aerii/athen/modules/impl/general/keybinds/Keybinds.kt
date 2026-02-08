package xyz.aerii.athen.modules.impl.general.keybinds

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.location.LocationAPI
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.InputEvent
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.message
import xyz.aerii.athen.modules.Module

@Load
object Keybinds : Module(
    "Keybinds",
    "Keybinds and shortcuts for various actions.",
    Category.GENERAL
) {
    private val keys = mutableSetOf<Int>()
    private val buttons = mutableSetOf<Int>()
    private val triggered = mutableSetOf<KeybindEntry>()

    @Suppress("unused")
    private val _unused by config.button("Open manager") { client.setScreen(KeybindsGUI) }

    val storage = Scribble("features/Keybinds")
    var bindings = storage.mutableList("bindings", KeybindEntry.CODEC)

    init {
        on<InputEvent.Keyboard.Press> {
            if (client.screen != null) return@on

            keys.add(keyEvent.key)
            check()
        }

        on<InputEvent.Keyboard.Release> {
            keys.remove(keyEvent.key)
            reset()
        }

        on<InputEvent.Mouse.Press> {
            if (client.screen != null) return@on

            val mouseCode = -(buttonInfo.button + 1)
            buttons.add(mouseCode)
            check()
        }

        on<InputEvent.Mouse.Release> {
            buttons.remove(-(buttonInfo.button + 1))
            reset()
        }
    }

    private fun check() {
        val all = (keys + buttons).toHashSet()
        val currentIsland = LocationAPI.island.value

        for (binding in bindings.value) {
            val ks = binding.keys
            if (ks.isEmpty() || binding in triggered) continue
            if (!ks.all(all::contains)) continue
            if (binding.island != null && binding.island != currentIsland) continue

            triggered.add(binding)

            val command = binding.command
            if (command.isEmpty()) continue
            if (command[0] == '/') command.command() else command.message()
        }
    }

    private fun reset() {
        val pressed = keys + buttons
        triggered.removeIf { b -> b.keys.any { it !in pressed } }
    }

    fun List<Int>.add(command: String, island: SkyBlockIsland?): Boolean {
        if (command.isBlank() || isEmpty()) return false

        bindings.update { add(KeybindEntry(this@add, command, island)) }
        return true
    }

    fun Int.remove(): Boolean {
        if (this !in bindings.value.indices) return false

        bindings.update { removeAt(this@remove) }
        return true
    }

    fun Int.update(keys: List<Int>, command: String, island: SkyBlockIsland?): Boolean {
        if (this !in bindings.value.indices || command.isBlank() || keys.isEmpty()) return false
        val int = this

        bindings.update { set(int, KeybindEntry(keys, command, island)) }
        return true
    }

    data class KeybindEntry(
        val keys: List<Int>,
        val command: String,
        val island: SkyBlockIsland? = null
    ) {
        companion object {
            val CODEC: Codec<KeybindEntry> = RecordCodecBuilder.create { inst ->
                inst.group(
                    Codec.INT.listOf().fieldOf("keys").forGetter(KeybindEntry::keys),
                    Codec.STRING.fieldOf("command").forGetter(KeybindEntry::command),
                    Codec.STRING.optionalFieldOf("island").forGetter { it.island?.id?.let { id -> java.util.Optional.of(id) } ?: java.util.Optional.empty() }
                ).apply(inst) { keys, command, islandId ->
                    KeybindEntry(keys, command, islandId.map { SkyBlockIsland.getByKey(it) }.orElse(null))
                }
            }
        }
    }
}