package xyz.aerii.athen.modules.impl.dungeon.terminals.solver.impl

import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.Click
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.ITerminal
import xyz.aerii.athen.utils.hasGlint

object NameSolver : ITerminal(TerminalType.NAME) {
    override fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float) {
        val l = list.toList()
        for (c in l) {
            val sx = (c.slot % 9 * 18f + ox + 1f) * uiScale
            val sy = ((c.slot / 9) * 18f + oy + headerH + 1f) * uiScale
            drawSlot(sx, sy, 16f * uiScale, 16f * uiScale, TerminalSolver.`names$correct`.rgb, uiScale)
        }
    }

    override fun forSlot(slot: Int): Click? = list.find { it.slot == slot }

    override fun valid(click: Click): Boolean = list.any { it.button == click.button }

    override fun compute(slot: Int, item: ItemStack) {
        list.clear()

        val match = TerminalType.NAME.regex.matchEntire(TerminalAPI.currentTitle)
        val targetLetter = match?.groupValues?.get(1)?.lowercase() ?: return

        for ((s, i) in TerminalAPI.currentItems) if (i.hoverName.stripped().lowercase().startsWith(targetLetter) && !i.hasGlint()) list.add(Click(s, 0))
    }
}