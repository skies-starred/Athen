package xyz.aerii.athen.modules.impl.dungeon.terminals.solver.impl

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.Click
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.ITerminal

object PanesSolver : ITerminal(TerminalType.PANES) {
    override fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float) {
        val l = list.toList()
        for (c in l) {
            val sx = (c.slot % 9 * 18f + ox + 1f) * uiScale
            val sy = ((c.slot / 9) * 18f + oy + headerH + 1f) * uiScale
            drawSlot(sx, sy, 16f * uiScale, 16f * uiScale, TerminalSolver.`panes$correct`.rgb, uiScale)
        }
    }

    override fun forSlot(slot: Int): Click? = list.find { it.slot == slot }

    override fun valid(click: Click): Boolean = list.any { it.button == click.button }

    override fun compute(slot: Int, item: ItemStack) {
        list.clear()

        for (i in TerminalAPI.currentItems) if (i.value.item == Items.RED_STAINED_GLASS_PANE) list.add(Click(i.key, 0))
    }
}