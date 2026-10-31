@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.kbus.data.event.traits.KBusCancellableTrait
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.client.extension.boundKey
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object WardrobeKeybinds : Module(
    "Wardrobe keybinds",
    "Keybinds for wardrobe slots!",
    ConfigCategory.GENERAL
) {
    private val preventUnequip by config.switch("Prevent unequip")
    private val cancelAll by config.switch("Cancel all other clicks")
    private val override by config.keybind("Key override", InputConstants.KEY_LCONTROL)
    private val cancelRender = config.switch("Cancel gui render").unique("cancelRender")
    private val ping by config.slider("Ping", 250, 10, 1000, "ms").description("Ping is used to estimate internal calculations.")

    private val keybinds by config.group("General keybinds")
    private val useHotbar by keybinds.switch("Use hotbar binds", true)
    private val prevPage by keybinds.keybind("Previous page")
    private val nextPage by keybinds.keybind("Next page")

    private val swaps by config.group("Swap keybinds")
    private val swapKey by swaps.switch("Swap key")
    private val swapKeybind by swaps.keybind("Swap keybind")
    private val swapKey1 by swaps.selector("Swap slot 1", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9"))
    private val swapKey2 by swaps.selector("Swap slot 2", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9"))

    private val slots0 by config.group("Slot keybinds")
    private val key0 by slots0.keybind("Slot 1", InputConstants.KEY_1)
    private val key1 by slots0.keybind("Slot 2", InputConstants.KEY_2)
    private val key2 by slots0.keybind("Slot 3", InputConstants.KEY_3)
    private val key3 by slots0.keybind("Slot 4", InputConstants.KEY_4)
    private val key4 by slots0.keybind("Slot 5", InputConstants.KEY_5)
    private val key5 by slots0.keybind("Slot 6", InputConstants.KEY_6)
    private val key6 by slots0.keybind("Slot 7", InputConstants.KEY_7)
    private val key7 by slots0.keybind("Slot 8", InputConstants.KEY_8)
    private val key8 by slots0.keybind("Slot 9", InputConstants.KEY_9)

    private val slots = listOf(
        WardrobeSlot(36) { key0 },
        WardrobeSlot(37) { key1 },
        WardrobeSlot(38) { key2 },
        WardrobeSlot(39) { key3 },
        WardrobeSlot(40) { key4 },
        WardrobeSlot(41) { key5 },
        WardrobeSlot(42) { key6 },
        WardrobeSlot(43) { key7 },
        WardrobeSlot(44) { key8 }
    )

    private var menuRegex: Regex = Regex("^\\((?<cur>\\d)/(?<max>\\d)\\) Armor Sets$")
    private var current: Int = 0
    private var max: Int = 0
    private var last: Long = 0
    private var open: Boolean = false

    init {
        on<GuiEvent.Open.Container> {
            val (cur, max) = menuRegex.find(stripped)?.destructured ?: return@on
            open = true
            current = cur.toInt()
            WardrobeKeybinds.max = max.toInt()
        }

        on<GuiEvent.Close.Container> {
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (!open) return@on
            fn(KeyboardInputState.vanilla(keyEvent.key))
        }

        on<GuiEvent.Input.Mouse.Press> {
            if (!open) return@on
            fn(KeyboardInputState.vanilla(keyEvent.button()))
        }

        on<GuiEvent.Render.Screen.Pre> {
            if (!open) return@on
            cancel()
        }.runWhen(cancelRender.state)
    }

    private fun KBusCancellableTrait.fn(key: InputConstants.Key) {
        val bool0 = GenericInputState.pressed(override)
        val bool1 = key == client.options.keyInventory.boundKey()
        val bool2 = key.value == InputConstants.KEY_ESCAPE
        if (cancelAll && !bool0 && !bool1 && !bool2) cancel()

        if (System.currentTimeMillis() - last < ping) return
        val player = client.player ?: return

        if (key == prevPage) {
            if (current > 1) MinecraftGui.click(player.containerMenu.containerId, 45)
            return
        }

        if (key == nextPage) {
            if (current < max) MinecraftGui.click(player.containerMenu.containerId, 53)
            return
        }

        if (swapKey && key == swapKeybind) {
            if (swapKey1 == swapKey2) return
            val slot1 = slots.find { it.index == swapKey1 + 36 }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val slot2 = slots.find { it.index == swapKey2 + 36 }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val s = if (slot1.equipped) slot2.index else slot1.index

            MinecraftGui.click(player.containerMenu.containerId, s)
            last = System.currentTimeMillis()
            cancel()
            return
        }

        val slot = slots.find { it.value == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return // slot can be empty on high ping, yay!
        if (slot.equipped && preventUnequip) return

        MinecraftGui.click(player.containerMenu.containerId, slot.index)
        last = System.currentTimeMillis()
        cancel()
    }

    private fun reset() {
        open = false
        current = 0
        max = 0
        last = 0
    }

    private data class WardrobeSlot(
        val index: Int,
        val keybind: () -> InputConstants.Key
    ) {
        val hotbar by lazy {
            val index = index - 36
            client.options.keyHotbarSlots[index]
        }

        val value: InputConstants.Key
            get() = if (useHotbar) hotbar.boundKey() else keybind()

        val slot: Slot?
            get() = client.player?.containerMenu?.slots?.getOrNull(index)

        val equipped: Boolean
            //~ if >= 26.2 'Items.LIME_DYE' -> 'Items.DYE.lime()'
            get() = slot?.item?.item == Items.LIME_DYE
    }
}
