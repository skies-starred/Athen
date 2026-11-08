package foo.starred.athen.modules.impl.general.keybinds

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.InputEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.general.keybinds.data.CategoryEntry
import foo.starred.athen.modules.impl.general.keybinds.data.KeybindCondition
import foo.starred.athen.modules.impl.general.keybinds.data.KeybindEntry
import foo.starred.athen.modules.impl.general.keybinds.ui.KeybindsGUI
import foo.starred.athen.utils.command
import foo.starred.kontext.abstraction.chat.command.MinecraftCommands
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.abstraction.input.impl.MouseInputState
import foo.starred.kontext.api.storage.impl.generic.extension.mutableList

@LoadableAthenModule
object Keybinds : Module(
    "Keybinds",
    "Keybinds and shortcuts for various actions.",
    ConfigCategory.GENERAL
) {
    private val pressed = mutableSetOf<InputConstants.Key>()
    private val triggered = mutableSetOf<KeybindEntry>()

    @Suppress("unused")
    private val _unused by config.button("Open manager") { KeybindsGUI.open() }

    val storage = JsonStore("features/Keybinds")
    var bindings = storage.mutableList("bindings", KeybindEntry.CODEC)
    var categories = storage.mutableList("categories", CategoryEntry.CODEC)

    init {
        command {
            "keybinds" {
                KeybindsGUI.open()
            }
        }

        on<InputEvent.Keyboard.Press> {
            pressed.add(KeyboardInputState.vanilla(keyEvent.key))
            check()
        }

        on<InputEvent.Keyboard.Release> {
            pressed.remove(KeyboardInputState.vanilla(keyEvent.key))
            reset()
        }

        on<InputEvent.Mouse.Press> {
            pressed.add(MouseInputState.vanilla(buttonInfo.button))
            check()
        }

        on<InputEvent.Mouse.Release> {
            pressed.remove(MouseInputState.vanilla(buttonInfo.button))
            reset()
        }
    }

    private fun check() {
        val disabled = categories.value.filter { !it.enabled }.map { it.name }

        for (binding in bindings.value) {
            if (!binding.condition.eval()) continue

            val keys = binding.keys
            if (keys.isEmpty()) continue
            if (binding in triggered) continue
            if (!binding.enabled) continue
            if (binding.category.isNotEmpty() && binding.category in disabled) continue
            if (!keys.all(pressed::contains)) continue

            triggered.add(binding)

            val command = binding.command
            if (command.isEmpty()) continue
            if (command[0] == '/') MinecraftCommands.client(command) else MinecraftMessages.send(command)
        }
    }

    private fun reset() {
        triggered.removeIf { binding ->
            binding.keys.any { it !in pressed }
        }
    }

    fun List<InputConstants.Key>.add(command: String, category: String = "", condition: KeybindCondition = KeybindCondition()): Boolean {
        if (command.isBlank()) return false
        if (isEmpty()) return false

        bindings.update { add(KeybindEntry(this@add, command, true, category, condition)) }
        return true
    }

    fun Int.remove(): Boolean {
        if (this !in bindings.value.indices) return false

        bindings.update { removeAt(this@remove) }
        return true
    }

    fun Int.update(keys: List<InputConstants.Key>, command: String, enabled: Boolean, category: String = "", condition: KeybindCondition): Boolean {
        if (this !in bindings.value.indices) return false
        if (command.isBlank()) return false
        if (keys.isEmpty()) return false

        val index = this
        bindings.update { set(index, KeybindEntry(keys, command, enabled, category, condition)) }

        return true
    }

    fun addCategory(name: String): Boolean {
        if (name.isBlank()) return false
        if (categories.value.any { it.name == name }) return false

        categories.update { add(CategoryEntry(name)) }
        return true
    }

    fun removeCategory(name: String) {
        categories.update {
            removeIf { it.name == name }
        }

        bindings.update {
            val updated = map { if (it.category == name) it.copy(category = "") else it }
            clear()
            addAll(updated)
        }
    }

    fun toggleCategory(name: String) {
        categories.update {
            val index = indexOfFirst { it.name == name }
            if (index >= 0) set(index, get(index).copy(enabled = !get(index).enabled))
        }
    }
}
