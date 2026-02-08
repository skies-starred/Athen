package xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base

import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.impl.*

object SimulatorMenu : ITerminalSim(TerminalType.PANES, 27, "Simulator".literal()) {
    val s = mapOf(
        10 to Items.LIME_STAINED_GLASS_PANE.pane("§aPanes"),
        11 to Items.RED_STAINED_GLASS_PANE.pane("§cRubix"),
        12 to Items.CYAN_STAINED_GLASS_PANE.pane("§bNumbers"),
        14 to Items.PINK_STAINED_GLASS_PANE.pane("§dStarts With"),
        15 to Items.BROWN_STAINED_GLASS_PANE.pane("§6Select All"),
        16 to Items.PURPLE_STAINED_GLASS_PANE.pane("§5Melody"),
        13 to Items.WHITE_STAINED_GLASS_PANE.pane("§7Random")
    )

    override fun s(): Map<Int, ItemStack> = s

    override fun click(slot: Slot, button: Int) {
        when (slot.containerSlot) {
            10 -> PanesSimulator()
            11 -> RubixSimulator()
            12 -> NumbersSimulator()
            13 -> listOf(
                PanesSimulator(),
                RubixSimulator(),
                NumbersSimulator(),
                NameSimulator(),
                ColorSimulator(),
                MelodySimulator()
            ).random()
            14 -> NameSimulator()
            15 -> ColorSimulator()
            16 -> MelodySimulator()
            else -> return
        }.a()
    }

    private fun Item.pane(name: String): ItemStack =
        ItemStack(this).apply { set(DataComponents.CUSTOM_NAME, name.literal()) }
}