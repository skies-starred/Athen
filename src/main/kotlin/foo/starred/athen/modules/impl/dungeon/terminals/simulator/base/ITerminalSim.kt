package foo.starred.athen.modules.impl.dungeon.terminals.simulator.base

import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.events.PacketEvent
import foo.starred.athen.modules.impl.dungeon.terminals.simulator.TerminalSimulator
import foo.starred.snowbird.api.EMPTY_COMPONENT
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.mainThread
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.open
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.PlayerEquipment
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.milliseconds

abstract class ITerminalSim(
    val type: TerminalType,
    val num: Int = type.slots,
    var component: Component = type.actual?.literal() ?: type.name.lowercase().replaceFirstChar { it.uppercase() }.literal(),
    val inv: SimpleContainer = SimpleContainer(num)
) : ContainerScreen(
    ChestMenu(type.g(), 0, Inventory(client.player!!, PlayerEquipment(client.player!!)), inv, num / 9),
    Inventory(client.player!!, PlayerEquipment(client.player!!)),
    component
) {
    protected val slots: List<Slot> get() = menu.slots.take(num)
    //~ if >= 26.2 'Items.BLACK_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.black()'
    protected val pane = ItemStack(Items.BLACK_STAINED_GLASS_PANE).apply { set(DataComponents.CUSTOM_NAME, EMPTY_COMPONENT) }
    var id = 0
    var c = true

    override fun init() {
        super.init()
        PacketEvent.Process.Pre(ClientboundOpenScreenPacket(id++, type.g(), component)).post()
        critter()
    }

    open fun a() {
        mainThread {
            this@ITerminalSim.open()
            TerminalSimulator.s.value = true
        }
    }

    override fun onClose() {
        c = true
        PacketEvent.Process.Pre(ClientboundContainerClosePacket(id - 1)).post()
        TerminalSimulator.s.value = false
        super.onClose()
    }

    abstract fun s(): Map<Int, ItemStack>

    open fun click(slot: Slot, button: Int) {}

    open fun critter() {
        for (a in slots) a.set(pane)
        for ((a, b) in s()) slots.getOrNull(a)?.set(b)
        val i = slots.map { it.item }
        PacketEvent.Process.Pre(ClientboundContainerSetContentPacket(id - 1, 0, i, ItemStack.EMPTY)).post()
        //~ if >= 26.2 'Items.BLACK_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.black()'
        for ((a, b) in i.withIndex()) if (b.item != Items.BLACK_STAINED_GLASS_PANE) PacketEvent.Process.Pre(ClientboundContainerSetSlotPacket(id - 1, 0, a, b)).post()
    }

    public override fun slotClicked(slot: Slot, slotId: Int, mouseButton: Int, type: ContainerInput) {
        if (slot.container != inv) return
        //~ if >= 26.2 'Items.BLACK_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.black()'
        if (slot.item.item == Items.BLACK_STAINED_GLASS_PANE) return
        if (!c) return
        if (TerminalSimulator.ping <= 0) return click(slot, mouseButton)

        c = false
        Scheduler.schedule(TerminalSimulator.ping.milliseconds) {
            c = true
            click(slot, mouseButton)
        }
    }

    protected fun Map<Int, ItemStack>.a() {
        for ((i, it) in this) {
            slots.getOrNull(i)?.set(it)
            PacketEvent.Process.Pre(ClientboundContainerSetSlotPacket(id - 1, 0, i, it)).post()
        }
    }
}

private fun TerminalType.g(): MenuType<*> = when {
    slots <= 9 -> MenuType.GENERIC_9x1
    slots <= 18 -> MenuType.GENERIC_9x2
    slots <= 27 -> MenuType.GENERIC_9x3
    slots <= 36 -> MenuType.GENERIC_9x4
    slots <= 45 -> MenuType.GENERIC_9x5
    else -> MenuType.GENERIC_9x6
}
