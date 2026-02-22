package xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.impl

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.ITerminalSim
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.SimulatorMenu
import xyz.aerii.athen.utils.glint
import kotlin.random.Random

class NameSimulator(
    private val targetLetter: String = listOf("A", "B", "C", "D", "G", "M", "N", "R", "S", "T", "W").random()
) : ITerminalSim(TerminalType.NAME, component = "What starts with: \'$targetLetter\'?".literal()) {

    override fun s(): Map<Int, ItemStack> {
        val result = mutableMapOf<Int, ItemStack>()
        val guaranteedSlot = (10..34).filter { it % 9 in 1..7 && it / 9 in 1..3 }.random()
        
        for (row in 1..3) {
            for (col in 1..7) {
                val index = row * 9 + col
                result[index] = pane(index == guaranteedSlot || Random.nextDouble() < 0.3)
            }
        }

        return result
    }

    override fun click(slot: Slot, button: Int) {
        val item = slot.item ?: return
        val name = item.hoverName?.string ?: return
        
        if (!name.startsWith(targetLetter, ignoreCase = true)) return "Invalid item! Does not start with <red>$targetLetter".parse().modMessage(Typo.PrefixType.ERROR)
        if (item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return
        
        mapOf(slot.containerSlot to item.apply { set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true) }).a()

        if (c()) SimulatorMenu.a()
    }

        private fun c(): Boolean {
            for (s in slots) {
                val it = s.item
                val n = it?.hoverName?.string ?: continue

                if (!n.startsWith(targetLetter, ignoreCase = true)) continue
                if (!it.glint()) return false
            }

            return true
        }

    private fun pane(match: Boolean): ItemStack {
        val items = BuiltInRegistries.ITEM.filter { item ->
            if (item == Items.AIR) return@filter false

            val name = item.name?.string ?: return@filter false
            if (name.contains("pane", ignoreCase = true)) return@filter false

            name.startsWith(targetLetter, ignoreCase = true) == match
        }

        return ItemStack(items.randomOrNull() ?: if (match) Items.STONE else Items.DIRT)
    }
}