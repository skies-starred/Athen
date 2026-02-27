@file:Suppress("EmptyRange")

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
import kotlin.math.abs

object RubixSolver : ITerminal(TerminalType.RUBIX) {
    private val colorOrder = listOf(
        Items.RED_STAINED_GLASS_PANE,
        Items.ORANGE_STAINED_GLASS_PANE,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.GREEN_STAINED_GLASS_PANE,
        Items.BLUE_STAINED_GLASS_PANE,
    )

    override fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float) {
        for (c in list) {
            val sx = (c.slot % 9 * 18f + ox + 1f) * uiScale
            val sy = ((c.slot / 9) * 18f + oy + headerH + 1f) * uiScale

            val color = if (c.button > 0) TerminalSolver.`rubix$positive`.rgb else TerminalSolver.`rubix$negative`.rgb
            drawSlot(sx, sy, 16f * uiScale, 16f * uiScale, color, uiScale)

            val btnStr = c.button.toString()
            val btnWidth = NVGRenderer.getTextWidth(btnStr, 11f * uiScale, NVGRenderer.defaultFont)
            NVGRenderer.drawText(btnStr, sx + 8f * uiScale - btnWidth / 2, sy + 3f * uiScale, 11f * uiScale, Mocha.Text.rgba)
        }
    }

    override fun forSlot(slot: Int): Click? = list.find { it.slot == slot }?.button?.let { Click(slot, if (it > 0) 0 else 1) }

    override fun valid(click: Click): Boolean {
        val sol = list.find { it.slot == click.slot }
        return sol != null && ((sol.button > 0 && click.button == 0) || (sol.button < 0 && click.button == 1))
    }

    override fun compute(slot: Int, item: ItemStack) {
        list.clear()

        val allowed = BooleanArray(54)
        for (s in intArrayOf(12, 13, 14, 21, 22, 23, 30, 31, 32)) allowed[s] = true

        val slots = IntArray(9)
        val ides = IntArray(9)
        var count = 0

        for ((id, stack) in TerminalAPI.currentItems) {
            if (id >= allowed.size || !allowed[id]) continue
            val idx = colorOrder.indexOf(stack.item).takeIf { it != -1 } ?: continue
            slots[count] = id
            ides[count] = idx
            count++
        }

        val costs = IntArray(5)
        for (t in 0 until 5) {
            var c = 0

            for (i in 0 until count) {
                val d = abs(t - ides[i])
                c += if (d > 2) 5 - d else d
            }

            costs[t] = c
        }

        var best = 0
        for (i in 1 until 5) if (costs[i] < costs[best]) best = i

        val o = TerminalAPI.`rubix$lastTarget`?.takeIf { costs[it] != 0 } ?: best.also { TerminalAPI.`rubix$lastTarget` = it }
        for (i in 0 until count) {
            val idx = ides[i]
            if (idx == o) continue

            var diff = o - idx
            if (diff > 2) diff -= 5 else if (diff < -2) diff += 5

            list.add(Click(slots[i], diff))
        }
    }
}