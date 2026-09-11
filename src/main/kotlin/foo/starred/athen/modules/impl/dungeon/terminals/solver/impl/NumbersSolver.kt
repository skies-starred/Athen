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

object NumbersSolver : ITerminalSolver(TerminalType.NUMBERS) {
    // override val int0 = 5
    // override val int1 = 2
    override val int0 = 7
    override val int1 = 1

    private val counts = mutableMapOf<Int, Int>()

    override fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        val font = CascadeFonts.arial
        for ((i, c) in list.withIndex()) {
            if (i > 2) break

            val x = (c.slot % 9 * float + x + 1f) * scale
            val y = ((c.slot / 9) * float + y + height + 1f) * scale
            val color = i.get() ?: continue

            slot(x, y, 16f * scale, 16f * scale, color, scale, pose, scissor)

            if (!TerminalSolvers.`ui$numbers$showText`) continue
            val a = counts[c.slot]?.toString() ?: continue
            val b = 11f * scale
            val d = font.width(a, b)
            font.extract(this, a, x + 8f * scale - d / 2, y + 3f * scale, Mocha.Text.rgba, false, b)
        }
    }

    override fun find(slot: Int): TerminalClick? {
        return list.firstOrNull()?.takeIf { it.slot == slot }
    }

    override fun valid(click: TerminalClick): Boolean {
        val a = list.firstOrNull()
        return a != null && a.slot == click.slot
    }

    override fun close() {
        counts.clear()
        super.close()
    }

    override fun compute(items: List<ItemStack>) {
        list.clear()

        //~ if >= 26.2 'Items.RED_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.red()'
        val a = items.indices.filter { items[it].item == Items.RED_STAINED_GLASS_PANE }.sortedBy { items[it].count }
        for (b in a) {
            val c = items[b]
            counts[b] = c.count
            list.add(TerminalClick(b, 0))
        }
    }

    private fun Int.get(): Int? = when (this) {
        0 -> TerminalSolvers.`numbers$first`
        1 -> TerminalSolvers.`numbers$second`
        2 -> TerminalSolvers.`numbers$third`
        else -> null
    }
}
