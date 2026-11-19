package foo.starred.athen.modules.impl.general.pet.impl

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.general.pet.data.PetKeybindsSlot
import foo.starred.kbus.data.event.traits.KBusCancellableTrait
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.client.extension.boundKey
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.abstraction.input.impl.MouseInputState
import foo.starred.kontext.api.data.Observable

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object PetKeybinds : Module(
    "Pet keybinds",
    "Keybinds to equip pets.",
    ConfigCategory.GENERAL,
) {
    private val cancelOther by config.switch("Cancel all other clicks")
    private val override by config.keybind("Key override", InputConstants.KEY_LCONTROL)

    private val keybinds by config.group("General keybinds")
    private val hotbar by keybinds.switch("Use hotbar binds", true)
    private val previous by keybinds.keybind("Previous page")
    private val next by keybinds.keybind("Next page")

    private val slots0 by config.group("Pet keybinds")
    val key0 by slots0.keybind("Pet 1", InputConstants.UNKNOWN.value)
    val key1 by slots0.keybind("Pet 2", InputConstants.UNKNOWN.value)
    val key2 by slots0.keybind("Pet 3", InputConstants.UNKNOWN.value)
    val key3 by slots0.keybind("Pet 4", InputConstants.UNKNOWN.value)
    val key4 by slots0.keybind("Pet 5", InputConstants.UNKNOWN.value)
    val key5 by slots0.keybind("Pet 6", InputConstants.UNKNOWN.value)
    val key6 by slots0.keybind("Pet 7", InputConstants.UNKNOWN.value)
    val key7 by slots0.keybind("Pet 8", InputConstants.UNKNOWN.value)
    val key8 by slots0.keybind("Pet 9", InputConstants.UNKNOWN.value)

    private val menuRegex = Regex("(?:\\((\\d+)/(\\d+)\\) )?Pets")
    private val open: Observable<Boolean> = Observable(false)

    private var current: Int = 0
    private var max: Int = 0

    init {
        on<GuiEvent.Open.Container> {
            val (i0, i1) = menuRegex.find(stripped)?.destructured ?: return@on

            open.value = true
            current = i0.toIntOrNull() ?: 1
            max = i1.toIntOrNull() ?: 1
        }

        on<GuiEvent.Close.Any> {
            open.value = false
        }

        on<GuiEvent.Input.Key.Press> {
            fn(KeyboardInputState.vanilla(keyEvent.key))
        }.runWhen(open)

        on<GuiEvent.Input.Mouse.Press> {
            fn(MouseInputState.vanilla(keyEvent.button()))
        }.runWhen(open)
    }

    private fun KBusCancellableTrait.fn(key: InputConstants.Key) {
        val bool0 = GenericInputState.pressed(override)
        val bool1 = key == client.options.keyInventory.boundKey()
        val bool2 = key.value == InputConstants.KEY_ESCAPE
        if (cancelOther && !bool0 && !bool1 && !bool2) cancel()

        val player = client.player ?: return

        if (key == previous) {
            if (current > 1) click(player.containerMenu.containerId, 45)
            return
        }

        if (key == next) {
            if (current < max) click(player.containerMenu.containerId, 53)
            return
        }

        val slot = PetKeybindsSlot.ALL.find { it.value(hotbar) == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
        if (slot.equipped) return

        click(player.containerMenu.containerId, slot.index)
    }

    private fun KBusCancellableTrait.click(id: Int, index: Int) {
        MinecraftGui.click(id, index)
        cancel()
    }
}
