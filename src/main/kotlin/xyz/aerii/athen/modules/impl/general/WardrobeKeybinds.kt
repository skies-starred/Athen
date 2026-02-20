@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.mixin.accessors.KeyMappingAccessor
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.isPressed

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

    private var menuRegex: Regex = Regex("^Wardrobe \\((?<cur>\\d)/(?<max>\\d)\\)$")
    private var currentPage: Int = 0
    private var maxPage: Int = 0
    private var inMenu: Boolean = false
    private var lastClick: Long = 0

    private val wardrobeSlots = listOf(
        WardrobeSlot(36, { acc(0) }, key0),
        WardrobeSlot(37, { acc(1) }, key1),
        WardrobeSlot(38, { acc(2) }, key2),
        WardrobeSlot(39, { acc(3) }, key3),
        WardrobeSlot(40, { acc(4) }, key4),
        WardrobeSlot(41, { acc(5) }, key5),
        WardrobeSlot(42, { acc(6) }, key6),
        WardrobeSlot(43, { acc(7) }, key7),
        WardrobeSlot(44, { acc(8) }, key8)
    )

    private data class WardrobeSlot(
        val idx: Int,
        val supp: () -> Int,
        val keybind: Int
    ) {
        val hotbar by lazy(supp)

        val value: Int
            get() = if (useHotbar) hotbar else keybind

        val slot: Slot?
            get() = client.player?.containerMenu?.slots?.getOrNull(idx)

        val equipped: Boolean
            get() = slot?.item?.item == Items.LIME_DYE
    }

    init {
        on<PacketEvent.Receive, ClientboundOpenScreenPacket> {
            menuRegex.findOrNull(title?.stripped() ?: return@on, "cur", "max") { (cur, max) ->
                inMenu = true
                currentPage = cur.toInt()
                maxPage = max.toInt()
            }
        }

        on<PacketEvent.Receive, ClientboundContainerClosePacket> {
            reset()
        }

        on<PacketEvent.Send, ServerboundContainerClosePacket> {
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (inMenu) fn(keyEvent.key)
        }

        on<GuiEvent.Input.Mouse.Press> {
            if (inMenu) fn(keyEvent.button())
        }

        on<GuiEvent.Container.Render.Pre> {
            if (inMenu) cancel()
        }.runWhen(cancelRender.state)
    }

    private fun CancellableEvent.fn(key: Int) {
        if (cancelAll && (override == -1 || !override.isPressed()) && key != (client.options.keyInventory as KeyMappingAccessor).boundKey.value && key != GLFW.GLFW_KEY_ESCAPE) cancel()

        if (System.currentTimeMillis() - lastClick < ping) return
        val player = client.player ?: return
        val container = player.containerMenu ?: return

        if (key == prevPage) {
            if (currentPage > 1) cl(container.containerId, 45, player)
            return
        }

        if (key == nextPage) {
            if (currentPage < maxPage) cl(container.containerId, 53, player)
            return
        }

        val slot = wardrobeSlots.find { it.value == key }?.takeIf { it.slot?.item?.isEmpty == false } ?: return // slot can be empty on high ping, yay!
        if (slot.equipped && preventUnequip) return

        cl(container.containerId, slot.idx, player)
        lastClick = System.currentTimeMillis()
        cancel()
    }

    private fun cl(id: Int, idx: Int, player: LocalPlayer) =
        client.gameMode?.handleInventoryMouseClick(id, idx, 0, ClickType.PICKUP, player)

    private fun acc(idx: Int): Int =
        (client.options.keyHotbarSlots[idx] as KeyMappingAccessor).boundKey.value

    private fun reset() {
        inMenu = false
        currentPage = 0
        maxPage = 0
        lastClick = 0
    }
}