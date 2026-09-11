package foo.starred.athen.modules.impl.dungeon.terminals.solver.base

import foo.starred.athen.api.dungeon.terminals.TerminalAPI
import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.modules.impl.dungeon.terminals.simulator.TerminalSimulator
import foo.starred.athen.modules.impl.dungeon.terminals.simulator.base.ITerminalSim
import foo.starred.athen.modules.impl.dungeon.terminals.solver.TerminalSolvers
import foo.starred.athen.modules.impl.dungeon.terminals.solver.data.TerminalClick
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.graphics.extensions.blur.blur
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.snowbird.api.client
import foo.starred.snowbird.utils.send
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import java.util.concurrent.CopyOnWriteArrayList

abstract class ITerminalSolver(val type: TerminalType) {
    protected val list = CopyOnWriteArrayList<TerminalClick>()
    protected val clicked = mutableSetOf<Int>()
    protected open val int0: Int = 7
    protected open val int1: Int = 1

    protected open val int2: Int
        get() = type.slots / 9 - 2

    protected open val float: Float
        get() = 16f + TerminalSolvers.`ui$gap`

    var pending: Boolean = false

    protected abstract fun GuiGraphicsExtractor.render(x: Float, y: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?)

    protected abstract fun compute(items: List<ItemStack>)

    protected abstract fun valid(click: TerminalClick): Boolean

    protected abstract fun find(slot: Int): TerminalClick?

    open fun open() {
        pending = false
        list.clear()
        clicked.clear()
    }

    open fun close() {
        pending = false
        list.clear()
        clicked.clear()
    }

    open fun resync() {
        clicked.clear()
    }

    open fun predict(click: TerminalClick) {
        clicked.add(click.slot)
        list.removeIf { it.slot == click.slot }
    }

    open fun click(slot: Int, button: Int) {
        if (TerminalSimulator.s.value) {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            val screen = client.screen as? ITerminalSim ?: return
            val slot0 = screen.menu.slots.getOrNull(slot) ?: return

            screen.slotClicked(slot0, slot, button, if (button == 0) ContainerInput.CLONE else ContainerInput.PICKUP)
            TerminalSolvers.last = System.currentTimeMillis()
            pending = true

            return
        }

        ServerboundContainerClickPacket(TerminalAPI.id, client.player?.containerMenu?.stateId ?: return, slot.toShort(), (if (button == 0) 2 else button).toByte(), if (button == 0) ContainerInput.CLONE else ContainerInput.PICKUP, Int2ObjectOpenHashMap(), HashedStack.create(ItemStack.EMPTY, client.connection?.decoratedHashOpsGenenerator() ?: return)).send()
        TerminalSolvers.last = System.currentTimeMillis()
        pending = true
    }

    fun main(graphics: GuiGraphicsExtractor) {
        val scale = TerminalSolvers.scale
        val width = client.window.guiScaledWidth.toFloat() / scale
        val height = client.window.guiScaledHeight.toFloat() / scale
        val grid = grid()

        val x = width / 2 - grid.width / 2
        val y = height / 2 - (grid.height + grid.head + grid.top) / 2
        val inset = (grid.spacing - 16f) / 2f

        val pose = Matrix3x2f(graphics.pose())
        val scissor = graphics.scissorStack.peek()
        val radius = CascadeGeometricRadius(TerminalSolvers.`ui$roundness` * scale)

        val x0 = x * scale
        val y0 = (y + grid.head + grid.top) * scale
        val width0 = grid.width * scale
        val height0 = grid.height * scale
        val bw = TerminalSolvers.`ui$borderWidth` * scale

        if (TerminalSolvers.`ui$blur`) {
            graphics.blur(x0, y0, width0, height0, 0, radius, 10f, pose, scissor)
        }

        graphics.roundedRectangle(x0, y0, width0, height0, TerminalSolvers.`ui$bg`, radius, pose, scissor)
        graphics.hollowRectangle(x0, y0, width0, height0, bw, TerminalSolvers.`ui$border`, radius, pose, scissor)

        graphics.header(x, y, grid.width, grid.head, scale, pose, scissor)
        graphics.render(x - int1 * grid.spacing + grid.padding + inset - 1f, y + grid.head + grid.top - grid.spacing + grid.padding + inset - 1f, 0f, scale, pose, scissor)
    }

    fun click(x: Float, y: Float, width: Float, height: Float, button: Int) {
        if (type == TerminalType.PANES && System.currentTimeMillis() - TerminalSolvers.last < TerminalSolvers.clickDelay) return

        val grid = grid()
        val slots = type.slots

        val x0 = width / 2 - grid.width / 2
        val y0 = height / 2 - (grid.height + grid.head + grid.top) / 2

        val x1 = ((x - x0 - grid.padding) / grid.spacing).toInt() + int1
        if (x1 !in int1 until int1 + int0) return

        val y1 = ((y - (y0 + grid.head + grid.top) - grid.padding) / grid.spacing).toInt() + 1
        if (y1 !in 1..int2) return

        val slot = x1 + y1 * 9
        if (slot >= slots) return

        val click = find(slot) ?: return
        if (click.button != button && !(type == TerminalType.RUBIX && TerminalSolvers.`rubix$left`)) return

        val b = if (type == TerminalType.RUBIX && TerminalSolvers.`rubix$left`) click.button else button
        click(slot, b)
        predict(click)
    }

    fun update(items: List<ItemStack>) {
        compute(items)
        list.removeIf { it.slot in clicked }
    }

    fun GuiGraphicsExtractor.slot(x: Float, y: Float, width: Float, height: Float, color: Int, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?, radius: CascadeGeometricRadius = CascadeGeometricRadius(TerminalSolvers.`ui$slots$roundness` * scale)) {
        if (TerminalSolvers.`ui$slots$fill`) roundedRectangle(x, y, width, height, color, radius, pose, scissor)
        else hollowRectangle(x, y, width, height, scale, color, radius, pose, scissor)
    }

    private fun GuiGraphicsExtractor.header( x: Float, y: Float, width: Float, height: Float, scale: Float, pose: Matrix3x2f, scissor: ScreenRectangle?) {
        if (TerminalSolvers.`ui$hideHeader`) return

        val title = type.name.lowercase().replaceFirstChar { it.uppercase() }
        val font = CascadeFonts.arial

        val radius = CascadeGeometricRadius(TerminalSolvers.`ui$roundness` * scale)
        val x1 = x * scale
        val y1 = y * scale
        val width1 = width * scale
        val height1 = height * scale
        val thickness = TerminalSolvers.`ui$borderWidth` * scale

        if (TerminalSolvers.`ui$blur`) {
            blur(x1, y1, width1, height1, -1, radius, 10f, pose, scissor)
        }

        roundedRectangle(x1, y1, width1, height1, TerminalSolvers.`ui$header`, radius, pose, scissor)
        hollowRectangle(x1, y1, width1, height1, thickness, TerminalSolvers.`ui$border`, radius, pose, scissor)

        val size = 11f * scale
        font.extract(this, title, (x + width / 2) * scale - font.width(title, size) / 2, (y + height / 2) * scale - (font.regular.height * size) / 2, Mocha.Text.rgba, false, size)
    }

    private fun grid(): Grid {
        val spacing = float
        val padding = TerminalSolvers.`ui$padding`
        val head = if (TerminalSolvers.`ui$hideHeader`) 0f else 20f
        val top = if (TerminalSolvers.`ui$hideHeader`) 0f else 6f

        return Grid(spacing, padding, int0 * spacing + 2 * padding, int2 * spacing + 2 * padding, head, top)
    }

    companion object {
        private data class Grid(
            val spacing: Float,
            val padding: Float,
            val width: Float,
            val height: Float,
            val head: Float,
            val top: Float
        )
    }
}
