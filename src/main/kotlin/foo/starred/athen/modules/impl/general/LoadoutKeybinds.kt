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
import foo.starred.kontext.abstraction.input.impl.MouseInputState
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object LoadoutKeybinds : Module(
    "Loadout keybinds",
    "Keybinds for loadout slots!",
    ConfigCategory.GENERAL
) {
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
    private val swapKey1 by swaps.selector("Swap slot 1", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9", "Slot 10", "Slot 11", "Slot 12"))
    private val swapKey2 by swaps.selector("Swap slot 2", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9", "Slot 10", "Slot 11", "Slot 12"))

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
    private val key9 by slots0.keybind("Slot 10", InputConstants.UNKNOWN.value)
    private val key10 by slots0.keybind("Slot 11", InputConstants.UNKNOWN.value)
    private val key11 by slots0.keybind("Slot 12", InputConstants.UNKNOWN.value)

    private val slots = listOf(
        LoadoutSlot(0, 14) { key0 },
        LoadoutSlot(1, 15) { key1 },
        LoadoutSlot(2, 16) { key2 },
        LoadoutSlot(3, 23) { key3 },
        LoadoutSlot(4, 24) { key4 },
        LoadoutSlot(5, 25) { key5 },
        LoadoutSlot(6, 32) { key6 },
        LoadoutSlot(7, 33) { key7 },
        LoadoutSlot(8, 34) { key8 },
        LoadoutSlot(9, 41) { key9 },
        LoadoutSlot(10, 42) { key10 },
        LoadoutSlot(11, 43) { key11 }
    )

    private val menuRegex: Regex = Regex("^\\((?<cur>\\d)/(?<max>\\d)\\) Loadouts$")
    private var current: Int = 0
    private var max: Int = 0
    private var last: Long = 0
    private var open: Boolean = false

    init {
        on<GuiEvent.Open.Container> {
            val (current1, max1) = menuRegex.find(stripped)?.destructured ?: return@on

            open = true
            current = current1.toInt()
            max = max1.toInt()
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
            fn(MouseInputState.vanilla(keyEvent.button()))
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
        val container = player.containerMenu

        if (key == prevPage) {
            if (current > 1) MinecraftGui.click(container.containerId, 17)
            return
        }

        if (key == nextPage) {
            if (current < max) MinecraftGui.click(container.containerId, 44)
            return
        }

        if (swapKey && key == swapKeybind) {
            if (swapKey1 == swapKey2) return
            val slot1 = slots.getOrNull(swapKey1)?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val slot2 = slots.getOrNull(swapKey2)?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val s = if (slot1.equipped) slot2.idx else slot1.idx

            MinecraftGui.click(container.containerId, s)
            last = System.currentTimeMillis()
            cancel()
            return
        }

        val slot = slots.find { it.value == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return // slot can be empty on high ping, yay!

        MinecraftGui.click(container.containerId, slot.idx)
        last = System.currentTimeMillis()
        cancel()
    }

    private fun reset() {
        open = false
        current = 0
        max = 0
        last = 0
    }

    private data class LoadoutSlot(
        val index: Int,
        val idx: Int,
        val keybind: () -> InputConstants.Key
    ) {
        val hotbar by lazy {
            client.options.keyHotbarSlots[index.coerceAtMost(8)]
        }

        val value: InputConstants.Key
            get() {
                if (useHotbar && idx >= 41) return InputConstants.UNKNOWN
                return if (useHotbar) hotbar.boundKey() else keybind()
            }

        val slot: Slot?
            get() = client.player?.containerMenu?.slots?.getOrNull(idx)

        val equipped: Boolean
            get() {
                val a = slot?.item?.get<GenericKontextItemDataResolver.LORE, List<Component>>() ?: return false
                return a.getOrNull(a.lastIndex - 1)?.stripped()?.isEmpty() ?: false
            }
    }
}
