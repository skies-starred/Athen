@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.mixin.accessors.KeyMappingAccessor
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.guiClick
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.client
import xyz.aerii.library.api.pressed

@Load
@OnlyIn(skyblock = true)
object WardrobeKeybinds : Module(
    "Wardrobe keybinds",
    "Keybinds for wardrobe slots!",
    Category.GENERAL
) {
    private val preventUnequip by config.switch("Prevent unequip")
    private val cancelAll by config.switch("Cancel all other clicks")
    private val override by config.keybind("Key override", GLFW.GLFW_KEY_LEFT_CONTROL).dependsOn { cancelAll }
    private val cancelRender = config.switch("Cancel gui render").custom("cancelRender")
    private val ping by config.slider("Ping", 250, 10, 1000, "ms")
    private val _unused by config.textParagraph("Ping is used to estimate internal calculations.")

    private val keybindExpandable by config.expandable("Keybinds")
    private val useHotbar by config.switch("Use hotbar binds", true).childOf { keybindExpandable }

    private val swapKey by config.switch("Swap key").childOf { keybindExpandable }
    private val swapKeybind by config.keybind("Swap keybind").dependsOn { swapKey }.childOf { keybindExpandable }
    private val swapKey1 by config.dropdown("Swap slot 1", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9")).dependsOn { swapKey }.childOf { keybindExpandable }
    private val swapKey2 by config.dropdown("Swap slot 2", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9")).dependsOn { swapKey }.childOf { keybindExpandable }

    private val prevPage by config.keybind("Previous page").childOf { keybindExpandable }
    private val nextPage by config.keybind("Next page").childOf { keybindExpandable }

    private val key0 by config.keybind("Slot 1", GLFW.GLFW_KEY_1).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key1 by config.keybind("Slot 2", GLFW.GLFW_KEY_2).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key2 by config.keybind("Slot 3", GLFW.GLFW_KEY_3).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key3 by config.keybind("Slot 4", GLFW.GLFW_KEY_4).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key4 by config.keybind("Slot 5", GLFW.GLFW_KEY_5).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key5 by config.keybind("Slot 6", GLFW.GLFW_KEY_6).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key6 by config.keybind("Slot 7", GLFW.GLFW_KEY_7).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key7 by config.keybind("Slot 8", GLFW.GLFW_KEY_8).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key8 by config.keybind("Slot 9", GLFW.GLFW_KEY_9).dependsOn { !useHotbar }.childOf { keybindExpandable }

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

        on<GuiEvent.Render.Container.Pre> {
            if (inMenu) cancel()
        }.runWhen(cancelRender.state)
    }

    private fun CancellableEvent.fn(key: Int) {
        if (cancelAll && (!override.bound || !override.pressed) && key != (client.options.keyInventory as KeyMappingAccessor).boundKey.value && key != GLFW.GLFW_KEY_ESCAPE) cancel()

        if (System.currentTimeMillis() - lastClick < ping) return
        val player = client.player ?: return
        val container = player.containerMenu ?: return

        if (key == prevPage) {
            if (currentPage > 1) guiClick(container.containerId, 45)
            return
        }

        if (key == nextPage) {
            if (currentPage < maxPage) guiClick(container.containerId, 53)
            return
        }

        if (swapKey && key == swapKeybind) {
            if (swapKey1 == swapKey2) return
            val slot1 = wardrobeSlots.find { it.idx == swapKey1 + 36 }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val slot2 = wardrobeSlots.find { it.idx == swapKey2 + 36 }?.takeIf { it.slot?.item?.isEmpty == false } ?: return
            val s = if (slot1.equipped) slot2.idx else slot1.idx

            guiClick(container.containerId, s)
            lastClick = System.currentTimeMillis()
            cancel()
            return
        }

        val slot = wardrobeSlots.find { it.value == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return // slot can be empty on high ping, yay!
        if (slot.equipped && preventUnequip) return

        guiClick(container.containerId, slot.idx)
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