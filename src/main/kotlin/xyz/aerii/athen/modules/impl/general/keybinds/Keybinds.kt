package xyz.aerii.athen.modules.impl.general.keybinds

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.InputEvent
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.modules.Module
import xyz.aerii.library.api.client
import xyz.aerii.library.api.command
import xyz.aerii.library.api.message

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
    var categories = storage.mutableList("categories", CategoryEntry.CODEC)

    init {
        on<InputEvent.Keyboard.Press> {
            keys.add(keyEvent.key)
            check()
        }

        on<InputEvent.Keyboard.Release> {
            keys.remove(keyEvent.key)
            reset()
        }

        on<InputEvent.Mouse.Press> {
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
        val disabled = categories.value.filter { !it.enabled }.map { it.name }

        for (binding in bindings.value) {
            if (!binding.condition.eval()) continue

            val ks = binding.keys
            if (ks.isEmpty()) continue
            if (binding in triggered) continue
            if (!binding.enabled) continue
            if (binding.category.isNotEmpty() && binding.category in disabled) continue
            if (!ks.all(all::contains)) continue

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

    fun List<Int>.add(command: String, category: String = "", condition: KeybindCondition = KeybindCondition()): Boolean {
        if (command.isBlank() || isEmpty()) return false
        bindings.update { add(KeybindEntry(this@add, command, true, category, condition)) }
        return true
    }

    fun Int.remove(): Boolean {
        if (this !in bindings.value.indices) return false
        bindings.update { removeAt(this@remove) }
        return true
    }

    fun Int.update(keys: List<Int>, command: String, enabled: Boolean, category: String = "", condition: KeybindCondition): Boolean {
        if (this !in bindings.value.indices || command.isBlank() || keys.isEmpty()) return false
        val int = this
        bindings.update { set(int, KeybindEntry(keys, command, enabled, category, condition)) }
        return true
    }

    fun addCategory(name: String): Boolean {
        if (name.isBlank() || categories.value.any { it.name == name }) return false
        categories.update { add(CategoryEntry(name)) }
        return true
    }

    fun removeCategory(name: String) {
        categories.update { removeIf { it.name == name } }
        bindings.update {
            val updated = map { if (it.category == name) it.copy(category = "") else it }
            clear()
            addAll(updated)
        }
    }

    fun toggleCategory(name: String) {
        categories.update {
            val idx = indexOfFirst { it.name == name }
            if (idx >= 0) set(idx, get(idx).copy(enabled = !get(idx).enabled))
        }
    }

    data class KeybindEntry(
        val keys: List<Int>,
        val command: String,
        val enabled: Boolean = true,
        val category: String = "",
        val condition: KeybindCondition = KeybindCondition()
    ) {
        companion object {
            val CODEC: Codec<KeybindEntry> = RecordCodecBuilder.create { inst ->
                inst.group(
                    Codec.INT.listOf().fieldOf("keys").forGetter(KeybindEntry::keys),
                    Codec.STRING.fieldOf("command").forGetter(KeybindEntry::command),
                    Codec.BOOL.optionalFieldOf("enabled", true).forGetter(KeybindEntry::enabled),
                    Codec.STRING.optionalFieldOf("category", "").forGetter(KeybindEntry::category),
                    KeybindCondition.CODEC.optionalFieldOf("condition", KeybindCondition()).forGetter(KeybindEntry::condition)
                ).apply(inst, ::KeybindEntry)
            }
        }
    }
}