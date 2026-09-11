@file:Suppress("EmptyRange")

package foo.starred.athen.modules.impl.dungeon.terminals.solver.impl

import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.modules.impl.dungeon.terminals.solver.TerminalSolvers
import foo.starred.athen.modules.impl.dungeon.terminals.solver.data.TerminalClick
import foo.starred.athen.modules.impl.dungeon.terminals.solver.base.ITerminalSolver
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.graphics.font.CascadeFonts
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix3x2f
import kotlin.math.abs

object RubixSolver : ITerminalSolver(TerminalType.RUBIX) {
    private val ints = intArrayOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
    //? if <= 26.1 {
    private val colors = listOf(Items.RED_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE)
    //? } else {
    /*private val colors = listOf(Items.STAINED_GLASS_PANE.red(), Items.STAINED_GLASS_PANE.orange(), Items.STAINED_GLASS_PANE.yellow(), Items.STAINED_GLASS_PANE.green(), Items.STAINED_GLASS_PANE.blue())
    *///? }

    override val int0 = 3
    override val int1 = 3

    private var last: Int? = null

    override fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        val font = CascadeFonts.arial

        for ((slot, button) in list) {
            val x = (slot % 9 * float + x + 1f) * scale
            val y = ((slot / 9) * float + y + height + 1f) * scale

            val color = if (button > 0) TerminalSolvers.`rubix$positive` else TerminalSolvers.`rubix$negative`
            slot(x, y, 16f * scale, 16f * scale, color, scale, pose, scissor)

            val string = button.toString()
            val size = 11f * scale
            val width = font.width(string, size)
            font.extract(this, string, x + 8f * scale - width / 2, y + 3f * scale, Mocha.Text.rgba, false, size)
        }
    }

    override fun find(slot: Int): TerminalClick? {
        return list.find { it.slot == slot }?.button?.let { TerminalClick(slot, if (it > 0) 0 else 1) }
    }

    override fun valid(click: TerminalClick): Boolean {
        val click0 = list.find { it.slot == click.slot } ?: return false
        return TerminalSolvers.`rubix$left` || (click0.button > 0 && click.button == 0) || (click0.button < 0 && click.button == 1)
    }

    override fun predict(click: TerminalClick) {
        val i0 = list.find { it.slot == click.slot } ?: return
        val i1 = if (i0.button > 0) i0.button - 1 else i0.button + 1

        if (i1 == 0) {
            list.remove(i0)
            clicked.add(click.slot)
            return
        }

        list[list.indexOf(i0)] = TerminalClick(click.slot, i1)
    }

    override fun open() {
        last = null
        super.open()
    }

    override fun close() {
        last = null
        super.close()
    }

    override fun resync() {
        last = null
        super.resync()
    }

    override fun compute(items: List<ItemStack>) {
        list.clear()

        val allowed = BooleanArray(54)
        for (s in ints) allowed[s] = true

        val slots = IntArray(9)
        val ides = IntArray(9)
        var count = 0

        for (i in items.indices) {
            val s = items[i]

            if (i >= allowed.size) continue
            if (!allowed[i]) continue

            val idx = colors.indexOf(s.item).takeIf { it != -1 } ?: continue
            slots[count] = i
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

        val o = last?.takeIf { costs[it] != 0 } ?: best.also { last = it }
        for (i in 0 until count) {
            val idx = ides[i]
            if (idx == o) continue

            var diff = o - idx
            if (diff > 2) diff -= 5 else if (diff < -2) diff += 5

            list.add(TerminalClick(slots[i], diff))
        }
    }
}
