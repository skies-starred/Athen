package foo.starred.athen.modules.impl.dungeon.terminals.solver.impl

import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.modules.impl.dungeon.terminals.solver.TerminalSolvers
import foo.starred.athen.modules.impl.dungeon.terminals.solver.data.TerminalClick
import foo.starred.athen.modules.impl.dungeon.terminals.solver.base.ITerminalSolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix3x2f

object PanesSolver : ITerminalSolver(TerminalType.PANES) {
    private val ints = intArrayOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33)

    override val int0 = 5
    override val int1 = 2

    override fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        val color = TerminalSolvers.`panes$correct`

        for ((slot) in list) {
            val x = (slot % 9 * float + x + 1f) * scale
            val y = ((slot / 9) * float + y + height + 1f) * scale

            slot(x, y, 16f * scale, 16f * scale, color, scale, pose, scissor)
        }
    }

    override fun find(slot: Int): TerminalClick? {
        return list.find { it.slot == slot }
    }

    override fun valid(click: TerminalClick): Boolean {
        return list.any { it.slot == click.slot }
    }

    override fun compute(items: List<ItemStack>) {
        list.clear()

        for (i in ints) {
            if (i >= items.size) continue
            //~ if >= 26.2 'Items.RED_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.red()'
            if (items[i].item != Items.RED_STAINED_GLASS_PANE) continue
            list.add(TerminalClick(i, 0))
        }
    }
}
