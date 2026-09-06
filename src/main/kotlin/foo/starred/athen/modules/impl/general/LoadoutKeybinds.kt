@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.core.CancellableEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.mixin.accessors.KeyMappingAccessor
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.guiClick
import foo.starred.snowbird.api.bound
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.pressed
import foo.starred.snowbird.utils.stripped
import net.minecraft.world.inventory.Slot
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull

@Load
@OnlyIn(skyblock = true)
object LoadoutKeybinds : Module(
    "Loadout keybinds",
    "Keybinds for loadout slots!",
    Category.GENERAL
) {
    private val cancelAll by config.switch("Cancel all other clicks")
    private val override by config.keybind("Key override", InputConstants.KEY_LCONTROL)
    private val cancelRender = config.switch("Cancel gui render").unique("cancelRender")
    private val ping by config.slider("Ping", 250, 10, 1000, "ms")
    private val _unused by config.information("Ping is used to estimate internal calculations.")

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

    private val menuRegex: Regex = Regex("^\\((?<cur>\\d)/(?<max>\\d)\\) Loadouts$")
    private var currentPage: Int = 0
    private var maxPage: Int = 0
    private var lastClick: Long = 0
    var open: Boolean = false

    val slots = listOf(
        LoadoutSlot(0, 14, { acc(0) }, { key0 }),
        LoadoutSlot(1, 15, { acc(1) }, { key1 }),
        LoadoutSlot(2, 16, { acc(2) }, { key2 }),
        LoadoutSlot(3, 23, { acc(3) }, { key3 }),
        LoadoutSlot(4, 24, { acc(4) }, { key4 }),
        LoadoutSlot(5, 25, { acc(5) }, { key5 }),
        LoadoutSlot(6, 32, { acc(6) }, { key6 }),
        LoadoutSlot(7, 33, { acc(7) }, { key7 }),
        LoadoutSlot(8, 34, { acc(8) }, { key8 }),
        LoadoutSlot(9, 41, { acc(8) }, { key9 }),
        LoadoutSlot(10, 42, { acc(8) }, { key10 }),
        LoadoutSlot(11, 43, { acc(8) }, { key11 })
    )

    init {
        on<GuiEvent.Open.Container> {
            menuRegex.findOrNull(stripped, "cur", "max") { (cur, max) ->
                open = true
                currentPage = cur.toInt()
                maxPage = max.toInt()
            }
        }

        on<GuiEvent.Close.Container> {
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (open) fn(keyEvent.key)
        }

        on<GuiEvent.Input.Mouse.Press> {
            if (open) fn(keyEvent.button())
        }

        on<GuiEvent.Render.Screen.Pre> {
            if (open) cancel()
        }.runWhen(cancelRender.state)
    }

    private fun CancellableEvent.fn(key: Int) {
        if (cancelAll && (!override.bound || !override.pressed) && key != (client.options.keyInventory as KeyMappingAccessor).boundKey.value && key != InputConstants.KEY_ESCAPE) cancel()

        if (System.currentTimeMillis() - lastClick < ping) return
        val player = client.player ?: return
        val container = player.containerMenu

        if (key == prevPage) {
            if (currentPage > 1) guiClick(container.containerId, 17)
            return
        }

        if (key == nextPage) {
            if (currentPage < maxPage) guiClick(container.containerId, 44)
            return
        }

        if (swapKey && key == swapKeybind) {
            if (swapKey1 == swapKey2) return
            val slot1 = slots.getOrNull(swapKey1)?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val slot2 = slots.getOrNull(swapKey2)?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val s = if (slot1.equipped) slot2.idx else slot1.idx

            guiClick(container.containerId, s)
            lastClick = System.currentTimeMillis()
            cancel()
            return
        }

        val slot = slots.find { it.value == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return // slot can be empty on high ping, yay!

        guiClick(container.containerId, slot.idx)
        lastClick = System.currentTimeMillis()
        cancel()
    }

    private fun acc(idx: Int): KeyMappingAccessor =
        client.options.keyHotbarSlots[idx] as KeyMappingAccessor

    private fun reset() {
        open = false
        currentPage = 0
        maxPage = 0
        lastClick = 0
    }

    data class LoadoutSlot(
        val index: Int,
        val idx: Int,
        val acc: () -> KeyMappingAccessor,
        val keybind: () -> Int
    ) {
        val hotbar by lazy(acc)

        val value: Int
            get() {
                if (useHotbar && idx >= 41) return InputConstants.UNKNOWN.value
                return if (useHotbar) hotbar.boundKey.value else keybind()
            }

        val slot: Slot?
            get() = client.player?.containerMenu?.slots?.getOrNull(idx)

        val equipped: Boolean
            get() {
                val a = slot?.item?.getLore() ?: return false
                return a.getOrNull(a.lastIndex - 1)?.stripped()?.isEmpty() ?: false
            }
    }
}
