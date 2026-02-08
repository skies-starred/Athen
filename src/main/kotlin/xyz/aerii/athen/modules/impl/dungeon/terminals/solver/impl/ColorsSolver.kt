package xyz.aerii.athen.modules.impl.dungeon.terminals.solver.impl

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.Click
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.ITerminal
import xyz.aerii.athen.utils.hasGlint

object ColorsSolver : ITerminal(TerminalType.COLORS) {
    override fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float) {
        val l = list.toList()
        for (c in l) {
            val sx = (c.slot % 9 * 18f + ox + 1f) * uiScale
            val sy = ((c.slot / 9) * 18f + oy + headerH + 1f) * uiScale
            drawSlot(sx, sy, 16f * uiScale, 16f * uiScale, TerminalSolver.`colors$correct`.rgb, uiScale)
        }
    }

    override fun forSlot(slot: Int): Click? = list.find { it.slot == slot }

    override fun valid(click: Click): Boolean = list.any { it.button == click.button }

    override fun compute(slot: Int, item: ItemStack) {
        list.clear()

        val str = TerminalType.COLORS.regex.matchEntire(TerminalAPI.currentTitle)?.groupValues?.get(1)?.replace("SILVER", "LIGHT GRAY")?.lowercase() ?: return
        for ((s, stack) in TerminalAPI.currentItems) {
            if (stack.hasGlint() || stack.item == Items.BLACK_STAINED_GLASS_PANE) continue
            if (stack.matches(str)) list.add(Click(s, 0))
        }
    }

    private fun ItemStack.matches(str: String): Boolean {
        val n = item.name.stripped().lowercase()
        return n.startsWith(str) || when (str) {
            "black" -> item == Items.INK_SAC
            "blue" -> item == Items.LAPIS_LAZULI
            "brown" -> item == Items.COCOA_BEANS
            "white" -> item == Items.BONE_MEAL
            else -> false
        }
    }
}