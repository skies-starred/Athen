package xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.impl

import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.ITerminalSim
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.SimulatorMenu
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import kotlin.random.Random

class PanesSimulator : ITerminalSim(TerminalType.PANES) {
    override fun s(): Map<Int, ItemStack> = buildMap {
        for (row in 1..3) for (col in 2..6) put(row * 9 + col, pane(Random.nextDouble() < 0.25))
    }

    override fun click(slot: Slot, button: Int) {
        mapOf(slot.containerSlot to pane(slot.item?.item != Items.LIME_STAINED_GLASS_PANE)).a()
        if (c()) SimulatorMenu.a()
    }

    private fun c(): Boolean {
        for (s in slots) {
            val it = s.item?.item ?: continue
            if (it != Items.BLACK_STAINED_GLASS_PANE && it == Items.RED_STAINED_GLASS_PANE) return false
        }

        return true
    }

    private fun pane(bool: Boolean): ItemStack =
        ItemStack(if (bool) Items.LIME_STAINED_GLASS_PANE else Items.RED_STAINED_GLASS_PANE).apply {
            set(DataComponents.CUSTOM_NAME, EMPTY_COMPONENT)
        }
}