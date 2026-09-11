package foo.starred.athen.modules.impl.dungeon.terminals.solver.impl

import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.modules.impl.dungeon.terminals.solver.TerminalSolvers
import foo.starred.athen.modules.impl.dungeon.terminals.solver.data.TerminalClick
import foo.starred.athen.modules.impl.dungeon.terminals.solver.base.ITerminalSolver
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix3x2f

object MelodySolver : ITerminalSolver(TerminalType.MELODY) {
    // private val slots = setOf(16, 25, 34)
    // override val int2: Int = 3
    private val slots = setOf(16, 25, 34, 43)
    override val int2: Int = 4

    override val float: Float
        get() = 16f + TerminalSolvers.`ui$melodyGap`

    var button: Int? = null
    var current: Int? = null
    var correct: Int? = null

    fun click(int: Int) {
        // if (int !in 1..3) return
        if (int !in 1..4) return
        val b = button ?: return
        if (TerminalSolvers.`melody$prevent` && (b != int - 1 || current == null || current != correct)) return
        click(16 + (int - 1) * 9, 0)
    }

    override fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        val button = button ?: return
        val current = current ?: return
        val correct = correct ?: return
        val float = float

        val row = button + 1
        val x0 = (float + x + 1f) * scale
        val y0 = (row * float + y + height + 1f) * scale
        val size = 16f * scale
        val spacing = float * scale
        val radius = CascadeGeometricRadius(TerminalSolvers.`ui$slots$roundness` * scale)

        for (i in 0 until 5) {
            val x1 = x0 + i * spacing
            val color = if (i == correct) TerminalSolvers.`melody$correct` else TerminalSolvers.`melody$wrong`

            when (i) {
                current -> {
                    roundedRectangle(x1, y0, size, size, TerminalSolvers.`melody$fill`, radius, pose, scissor)
                    hollowRectangle(x1, y0, size, size, scale, color, radius, pose, scissor)
                }

                correct -> {
                    hollowRectangle(x1, y0, size, size, scale, TerminalSolvers.`melody$correct`, radius, pose, scissor)
                }

                else -> {
                    hollowRectangle(x1, y0, size, size, scale, TerminalSolvers.`melody$wrong`, radius, pose, scissor)
                }
            }
        }

        for (slot in 0 until type.slots) {
            val i0 = slot / 9
            val i1 = slot % 9

            // if (i0 !in 1..3) continue
            if (i0 !in 1..4) continue
            if (i1 == 0) continue
            if (i1 == 8) continue

            val x = (i1 * float + x + 1f) * scale
            val y = (i0 * float + y + height + 1f) * scale

            when {
                slot == button * 9 + 16 -> {
                    slot(x, y, size, size, TerminalSolvers.`melody$correct`, scale, pose, scissor)
                }

                slot in slots -> {
                    slot(x, y, size, size, TerminalSolvers.`melody$wrong`, scale, pose, scissor)
                }

                i0 != row -> {
                    if (i1 !in 1..5) continue
                    slot(x, y, size, size, TerminalSolvers.`melody$other`, scale, pose, scissor)
                }
            }
        }
    }

    override fun find(slot: Int): TerminalClick? {
        if (slot !in slots) return null
        val b = button ?: return null
        if (TerminalSolvers.`melody$prevent` && (slot != b * 9 + 16 || current == null || current != correct)) return null
        click(slot, 0)
        return null
    }

    override fun valid(click: TerminalClick): Boolean {
        return false
    }

    override fun close() {
        button = null
        correct = null
        current = null
        super.close()
    }

    override fun compute(items: List<ItemStack>) {
        var a = -1
        var b = -1

        for (i in items.indices) {
            val s = items[i].item
            //~ if >= 26.2 'Items.LIME_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.lime()'
            if (a == -1 && s == Items.LIME_STAINED_GLASS_PANE) a = i
            //~ if >= 26.2 'Items.MAGENTA_STAINED_GLASS_PANE' -> 'Items.STAINED_GLASS_PANE.magenta()'
            if (b == -1 && s == Items.MAGENTA_STAINED_GLASS_PANE) b = i
            if (a != -1 && b != -1) break
        }

        if (a == -1) return
        if (b != -1) correct = b - 1

        button = a / 9 - 1
        current = a % 9 - 1
    }
}
