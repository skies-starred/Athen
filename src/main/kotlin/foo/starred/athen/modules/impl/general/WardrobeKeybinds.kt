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
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull

@Load
@OnlyIn(skyblock = true)
object WardrobeKeybinds : Module(
    "Wardrobe keybinds",
    "Keybinds for wardrobe slots!",
    Category.GENERAL
) {
    private val preventUnequip by config.switch("Prevent unequip")
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
    private val swapKey1 by swaps.selector("Swap slot 1", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9"))
    private val swapKey2 by swaps.selector("Swap slot 2", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9"))

    private val slots by config.group("Slot keybinds")
    private val key0 by slots.keybind("Slot 1", InputConstants.KEY_1)
    private val key1 by slots.keybind("Slot 2", InputConstants.KEY_2)
    private val key2 by slots.keybind("Slot 3", InputConstants.KEY_3)
    private val key3 by slots.keybind("Slot 4", InputConstants.KEY_4)
    private val key4 by slots.keybind("Slot 5", InputConstants.KEY_5)
    private val key5 by slots.keybind("Slot 6", InputConstants.KEY_6)
    private val key6 by slots.keybind("Slot 7", InputConstants.KEY_7)
    private val key7 by slots.keybind("Slot 8", InputConstants.KEY_8)
    private val key8 by slots.keybind("Slot 9", InputConstants.KEY_9)

    private var menuRegex: Regex = Regex("^\\((?<cur>\\d)/(?<max>\\d)\\) Armor Sets$")
    private var currentPage: Int = 0
    private var maxPage: Int = 0
    private var lastClick: Long = 0
    var inMenu: Boolean = false

    val wardrobeSlots = listOf(
        WardrobeSlot(36, { acc(0) }, { key0 }),
        WardrobeSlot(37, { acc(1) }, { key1 }),
        WardrobeSlot(38, { acc(2) }, { key2 }),
        WardrobeSlot(39, { acc(3) }, { key3 }),
        WardrobeSlot(40, { acc(4) }, { key4 }),
        WardrobeSlot(41, { acc(5) }, { key5 }),
        WardrobeSlot(42, { acc(6) }, { key6 }),
        WardrobeSlot(43, { acc(7) }, { key7 }),
        WardrobeSlot(44, { acc(8) }, { key8 })
    )

    data class WardrobeSlot(
        val idx: Int,
        val acc: () -> KeyMappingAccessor,
        val keybind: () -> Int
    ) {
        val hotbar by lazy(acc)

        val value: Int
            get() = if (useHotbar) hotbar.boundKey.value else keybind()

        val slot: Slot?
            get() = client.player?.containerMenu?.slots?.getOrNull(idx)

        val equipped: Boolean
            //~ if >= 26.2 'Items.LIME_DYE' -> 'Items.DYE.pick(net.minecraft.world.item.DyeColor.LIME)'
            get() = slot?.item?.item == Items.LIME_DYE
    }

    init {
        on<GuiEvent.Open.Container> {
            menuRegex.findOrNull(stripped, "cur", "max") { (cur, max) ->
                inMenu = true
                currentPage = cur.toInt()
                maxPage = max.toInt()
            }
        }

        on<GuiEvent.Close.Container> {
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (inMenu) fn(keyEvent.key)
        }

        on<GuiEvent.Input.Mouse.Press> {
            if (inMenu) fn(keyEvent.button())
        }

        on<GuiEvent.Render.Screen.Pre> {
            if (inMenu) cancel()
        }.runWhen(cancelRender.state)
    }

    private fun CancellableEvent.fn(key: Int) {
        if (cancelAll && (!override.bound || !override.pressed) && key != (client.options.keyInventory as KeyMappingAccessor).boundKey.value && key != InputConstants.KEY_ESCAPE) cancel()

        if (System.currentTimeMillis() - lastClick < ping) return
        val player = client.player ?: return

        if (key == prevPage) {
            if (currentPage > 1) guiClick(player.containerMenu.containerId, 45)
            return
        }

        if (key == nextPage) {
            if (currentPage < maxPage) guiClick(player.containerMenu.containerId, 53)
            return
        }

        if (swapKey && key == swapKeybind) {
            if (swapKey1 == swapKey2) return
            val slot1 = wardrobeSlots.find { it.idx == swapKey1 + 36 }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val slot2 = wardrobeSlots.find { it.idx == swapKey2 + 36 }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val s = if (slot1.equipped) slot2.idx else slot1.idx

            guiClick(player.containerMenu.containerId, s)
            lastClick = System.currentTimeMillis()
            cancel()
            return
        }

        val slot = wardrobeSlots.find { it.value == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return // slot can be empty on high ping, yay!
        if (slot.equipped && preventUnequip) return

        guiClick(player.containerMenu.containerId, slot.idx)
        lastClick = System.currentTimeMillis()
        cancel()
    }

    private fun acc(idx: Int): KeyMappingAccessor =
        client.options.keyHotbarSlots[idx] as KeyMappingAccessor

    private fun reset() {
        inMenu = false
        currentPage = 0
        maxPage = 0
        lastClick = 0
    }
}
