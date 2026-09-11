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

object ColorsSolver : ITerminalSolver(TerminalType.COLORS) {
    private val aliases = mapOf(
        "black" to listOf("black", "ink"),
        "blue" to listOf("blue", "lapis"),
        "brown" to listOf("brown", "cocoa"),
        "white" to listOf("white", "bone", "wool"),
        "green" to listOf("green", "cactus", "lime"),
        "red" to listOf("red", "rose", "poppy"),
        "yellow" to listOf("yellow", "dandelion", "sunflower"),
        "silver" to listOf("silver", "light gray")
    )

    override fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        val color = TerminalSolvers.`colors$correct`

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

        val str = TerminalType.COLORS.regex.matchEntire(TerminalAPI.title)?.groupValues?.get(1)?.lowercase() ?: return
        val set = aliases[str] ?: listOf(str)

        for (i in items.indices) {
            val s = items[i]
            //~ if >= 26.2 'Items.BLACK_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.black()'
            if (s.item == Items.BLACK_STAINED_GLASS_PANE) continue
            if (s.glint()) continue
            if (!s.matches(set)) continue

            list.add(TerminalClick(i, 0))
        }
    }

    private fun ItemStack.matches(keywords: List<String>): Boolean {
        val hover = hoverName.stripped().lowercase()
        val name = item.getName(item.defaultInstance).stripped().lowercase()
        return keywords.any { hover.startsWith(it) || name.startsWith(it) }
    }
}
