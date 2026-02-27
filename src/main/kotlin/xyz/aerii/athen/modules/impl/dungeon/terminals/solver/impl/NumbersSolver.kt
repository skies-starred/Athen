package xyz.aerii.athen.modules.impl.dungeon.terminals.solver.impl

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.Click
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base.ITerminal
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer

object NumbersSolver : ITerminal(TerminalType.NUMBERS) {
    override fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float) {
        for ((i, c) in list.withIndex()) {
            if (i > 2) break

            val sx = (c.slot % 9 * 18f + ox + 1f) * uiScale
            val sy = ((c.slot / 9) * 18f + oy + headerH + 1f) * uiScale
            val color = i.get() ?: continue

            drawSlot(sx, sy, 16f * uiScale, 16f * uiScale, color, uiScale)

            if (!TerminalSolver.`ui$numbers$showText`) continue
            val countStr = TerminalAPI.slotCounts[c.slot]?.toString() ?: continue
            val countWidth = NVGRenderer.getTextWidth(countStr, 11f * uiScale, NVGRenderer.defaultFont)
            NVGRenderer.drawText(countStr, sx + 8f * uiScale - countWidth / 2, sy + 3f * uiScale, 11f * uiScale, Mocha.Text.rgba)
        }
    }

    override fun forSlot(slot: Int): Click? = list.firstOrNull()?.takeIf { it.slot == slot }

    override fun valid(click: Click): Boolean {
        val firstSol = list.firstOrNull()
        return firstSol != null && firstSol.slot == click.slot
    }

    override fun compute(slot: Int, item: ItemStack) {
        list.clear()

        for ((slot, stack) in TerminalAPI.currentItems.entries.sortedBy { it.value.count }) {
            if (stack.item != Items.RED_STAINED_GLASS_PANE) continue

            TerminalAPI.slotCounts[slot] = stack.count
            list.add(Click(slot, 0))
        }
    }

    private fun Int.get(): Int? = when (this) {
        0 -> TerminalSolver.`numbers$first`.rgb
        1 -> TerminalSolver.`numbers$second`.rgb
        2 -> TerminalSolver.`numbers$third`.rgb
        else -> null
    }
}