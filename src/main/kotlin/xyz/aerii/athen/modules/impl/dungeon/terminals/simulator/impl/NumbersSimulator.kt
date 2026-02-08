package xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.impl

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.ITerminalSim
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.SimulatorMenu
import xyz.aerii.athen.utils.EMPTY_COMPONENT

class NumbersSimulator : ITerminalSim(TerminalType.NUMBERS) {
    private val ints = (1..14).shuffled()

    override fun s(): Map<Int, ItemStack> {
        var i = 0
        return buildMap {
            for (row in 1..2) for (col in 1..7) put(row * 9 + col, pane(ints[i++]))
        }
    }

    override fun click(slot: Slot, button: Int) {
        if (slot.item?.item != Items.RED_STAINED_GLASS_PANE) return
        if (slot.containerSlot != min()?.containerSlot) return

        val count = slot.item?.count ?: return
        mapOf(slot.containerSlot to pane(count, true)).a()

        if (c()) SimulatorMenu.a()
    }

    private fun min(): Slot? {
        var b: Slot? = null
        var m = Int.MAX_VALUE

        for (s in slots) {
            val it = s.item
            if (it?.item != Items.RED_STAINED_GLASS_PANE) continue

            val count = it.count
            if (count < m) {
                m = count
                b = s
            }
        }

        return b
    }

    private fun c(): Boolean {
        for (s in slots) if (s.item?.item == Items.RED_STAINED_GLASS_PANE) return false
        return true
    }

    private fun pane(number: Int, completed: Boolean = false): ItemStack =
        ItemStack(if (completed) Items.LIME_STAINED_GLASS_PANE else Items.RED_STAINED_GLASS_PANE, number).apply {
            set(DataComponents.CUSTOM_NAME, if (completed) EMPTY_COMPONENT else Component.literal("§a$number"))
        }
}