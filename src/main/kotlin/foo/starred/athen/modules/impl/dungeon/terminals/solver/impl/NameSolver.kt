package foo.starred.athen.modules.impl.dungeon.terminals.solver.impl

import foo.starred.athen.api.dungeon.terminals.TerminalAPI
import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.modules.impl.dungeon.terminals.solver.TerminalSolvers
import foo.starred.athen.modules.impl.dungeon.terminals.solver.data.TerminalClick
import foo.starred.athen.modules.impl.dungeon.terminals.solver.base.ITerminalSolver
import foo.starred.athen.utils.glint
import foo.starred.snowbird.utils.stripped
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix3x2f

object NameSolver : ITerminalSolver(TerminalType.NAME) {
    override fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        val color = TerminalSolvers.`names$correct`

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
        return list.any { it.button == click.button }
    }

    override fun compute(items: List<ItemStack>) {
        list.clear()

        val match = TerminalType.NAME.regex.matchEntire(TerminalAPI.title)
        val targetLetter = match?.groupValues?.get(1)?.lowercase() ?: return

        for (i0 in items.indices) {
            val s = items[i0]
            if (s.isEmpty) continue
            //~ if >= 26.2 'Items.BLACK_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.black()'
            if (s.item == Items.BLACK_STAINED_GLASS_PANE) continue
            if (s.glint() && s.item != Items.GOLDEN_APPLE && !s.item.defaultInstance.glint()) continue
            if (!s.hoverName.stripped().lowercase().startsWith(targetLetter, true)) continue

            list.add(TerminalClick(i0, 0))
        }
    }
}
