@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
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
import xyz.aerii.library.utils.stripped

@Load
@OnlyIn(skyblock = true)
object LoadoutKeybinds : Module(
    "Loadout keybinds",
    "Keybinds for loadout slots!",
    Category.GENERAL
) {
    private val cancelAll by config.switch("Cancel all other clicks")
    private val override by config.keybind("Key override", GLFW.GLFW_KEY_LEFT_CONTROL).dependsOn { cancelAll }
    private val cancelRender = config.switch("Cancel gui render").custom("cancelRender")
    private val ping by config.slider("Ping", 250, 10, 1000, "ms")
    private val _unused by config.textParagraph("Ping is used to estimate internal calculations.")

    private val keybindExpandable by config.expandable("Keybinds")
    private val useHotbar by config.switch("Use hotbar binds", true).childOf { keybindExpandable }

    private val swapKey by config.switch("Swap key").childOf { keybindExpandable }
    private val swapKeybind by config.keybind("Swap keybind").dependsOn { swapKey }.childOf { keybindExpandable }
    private val swapKey1 by config.dropdown("Swap slot 1", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9", "Slot 10", "Slot 11", "Slot 12")).dependsOn { swapKey }.childOf { keybindExpandable }
    private val swapKey2 by config.dropdown("Swap slot 2", listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5", "Slot 6", "Slot 7", "Slot 8", "Slot 9", "Slot 10", "Slot 11", "Slot 12")).dependsOn { swapKey }.childOf { keybindExpandable }

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
    private val key9 by config.keybind("Slot 10", GLFW.GLFW_KEY_UNKNOWN).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key10 by config.keybind("Slot 11", GLFW.GLFW_KEY_UNKNOWN).dependsOn { !useHotbar }.childOf { keybindExpandable }
    private val key11 by config.keybind("Slot 12", GLFW.GLFW_KEY_UNKNOWN).dependsOn { !useHotbar }.childOf { keybindExpandable }

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

        on<GuiEvent.Render.Container.Pre> {
            if (open) cancel()
        }.runWhen(cancelRender.state)
    }

    private fun CancellableEvent.fn(key: Int) {
        if (cancelAll && (!override.bound || !override.pressed) && key != (client.options.keyInventory as KeyMappingAccessor).boundKey.value && key != GLFW.GLFW_KEY_ESCAPE) cancel()

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
            val slot2 = slots.getOrNull(swapKey1)?.takeIf { it.slot?.item?.isEmpty == false } ?: return
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
                if (useHotbar && idx >= 41) return GLFW.GLFW_KEY_UNKNOWN
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