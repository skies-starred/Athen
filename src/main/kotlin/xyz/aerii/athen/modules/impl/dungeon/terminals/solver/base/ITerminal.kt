package xyz.aerii.athen.modules.impl.dungeon.terminals.solver.base

import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.TerminalSimulator
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.ITerminalSim
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer

abstract class ITerminal(val terminalType: TerminalType) {
    protected val list = mutableListOf<Click>()

    open fun onOpen() {}

    open fun onClose() {}

    protected abstract fun compute(slot: Int = 0, item: ItemStack = ItemStack.EMPTY)

    protected abstract fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float)

    protected abstract fun valid(click: Click): Boolean

    protected abstract fun forSlot(slot: Int): Click?

    fun main() {
        val uiScale = 3f * TerminalSolver.`ui$scale`
        val w = client.window.width / uiScale
        val h = client.window.height / uiScale

        val gridW = 9 * 18f
        val gridH = (terminalType.slots / 9) * 18f
        val headerH = 20f
        val padding = 6f
        val totalH = gridH + headerH + padding

        val ox = w / 2 - gridW / 2
        val oy = h / 2 - totalH / 2

        main(ox, oy, gridW, headerH, uiScale)
        render(ox, oy + headerH + padding, 0f, uiScale)
    }

    fun click(mx: Float, my: Float, width: Float, height: Float, mouseButton: Int) {
        val slots = terminalType.slots
        val gridW = 9 * 18f
        val gridH = (slots / 9) * 18f
        val headerH = 20f
        val padding = 6f

        val ox = width / 2 - gridW / 2
        val oy = height / 2 - (gridH + headerH + padding) / 2

        val x = ((mx - ox) / 18).toInt()
        val y = ((my - (oy + headerH + padding)) / 18).toInt()
        if (x !in 0..8 || y < 0) return

        val slot = x + y * 9
        if (slot >= slots) return

        val c = forSlot(slot) ?: return
        if (c.button != mouseButton && !(terminalType == TerminalType.RUBIX && TerminalSolver.`rubix$left`)) return
        c.click()
    }

    fun update(slot: Int, item: ItemStack) {
        compute(slot, item)
    }

    protected fun Click.click() {
        click(slot, button)
    }

    protected fun drawSlot(x: Float, y: Float, w: Float, h: Float, color: Int, uiScale: Float, radius: Float = TerminalSolver.`ui$slots$roundness` * uiScale) =
        if (TerminalSolver.`ui$slots$fill`) NVGRenderer.drawRectangle(x, y, w, h, color, radius) else NVGRenderer.drawHollowRectangle(x, y, w, h, uiScale, color, radius)

    protected fun click(slot: Int, button: Int) {
        if (TerminalSimulator.s.value) {
            val screen = client.screen as? ITerminalSim ?: return
            val slot0 = screen.menu?.slots?.getOrNull(slot) ?: return
            screen.slotClicked(slot0, slot, button, if (button == 0) ClickType.CLONE else ClickType.PICKUP)
            if (TerminalSolver.`sound$enabled`) TerminalSolver.clickSound.play()
            return
        }

        if (TerminalSolver.`sound$enabled`) TerminalSolver.clickSound.play()
        client.gameMode?.handleInventoryMouseClick(
            TerminalAPI.lastId,
            slot,
            if (button == 0) 2 else button,
            if (button == 0) ClickType.CLONE else ClickType.PICKUP,
            client.player ?: return
        )
    }

    private fun main(ox: Float, oy: Float, gridW: Float, headerH: Float, uiScale: Float) {
        val titleText = terminalType.name.lowercase().replaceFirstChar { it.uppercase() }
        val gridH = (terminalType.slots / 9) * 18f
        val padding = 6f

        NVGRenderer.drawOutlinedRectangle(ox * uiScale, oy * uiScale, gridW * uiScale, headerH * uiScale, TerminalSolver.`ui$header`.rgb, TerminalSolver.`ui$border`.rgb, uiScale / 2f, TerminalSolver.`ui$roundness` * uiScale)
        NVGRenderer.drawOutlinedRectangle(ox * uiScale, (oy + headerH + padding) * uiScale, gridW * uiScale, gridH * uiScale, TerminalSolver.`ui$bg`.rgb, TerminalSolver.`ui$border`.rgb, uiScale / 2f, TerminalSolver.`ui$roundness` * uiScale)

        val titleWidth = NVGRenderer.getTextWidth(titleText, 11f * uiScale, NVGRenderer.defaultFont)
        val titleX = ox + gridW / 2 - titleWidth / uiScale / 2
        NVGRenderer.drawText(titleText, titleX * uiScale, (oy + headerH / 2 - 5.5f) * uiScale, 11f * uiScale, Mocha.Text.rgba)
    }
}