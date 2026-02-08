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

object MelodySolver : ITerminal(TerminalType.MELODY) {
    override fun render(ox: Float, oy: Float, headerH: Float, uiScale: Float) {
        val correct = TerminalAPI.`melody$correct` ?: return
        val button = TerminalAPI.`melody$button` ?: return
        val current = TerminalAPI.`melody$current` ?: return

        val row = button + 1
        val rowY = (row * 18f + oy + headerH + 1f) * uiScale
        val rowX = (18f + ox + 1f) * uiScale
        val size = 16f * uiScale
        val spacing = 18f * uiScale

        for (i in 0 until 5) {
            val x = rowX + i * spacing
            when (i) {
                current -> NVGRenderer.drawOutlinedRectangle(x, rowY, size, size, TerminalSolver.`melody$fill`.rgb, if (i == correct) TerminalSolver.`melody$correct`.rgb else TerminalSolver.`melody$wrong`.rgb, uiScale, TerminalSolver.`ui$slots$roundness` * uiScale)
                correct -> NVGRenderer.drawHollowRectangle(x, rowY, size, size, uiScale, TerminalSolver.`melody$correct`.rgb, TerminalSolver.`ui$slots$roundness` * uiScale)
                else -> NVGRenderer.drawHollowRectangle(x, rowY, size, size, uiScale, TerminalSolver.`melody$wrong`.rgb, TerminalSolver.`ui$slots$roundness` * uiScale)
            }
        }

        val buttonSlot = button * 9 + 16
        val wrongSlots = setOf(16, 25, 34, 43)

        for (slot in 0 until terminalType.slots) {
            val r = slot / 9
            val c = slot % 9

            val x = (c * 18f + ox + 1f) * uiScale
            val y = (r * 18f + oy + headerH + 1f) * uiScale

            when {
                slot == buttonSlot -> drawSlot(x, y, size, size, TerminalSolver.`melody$correct`.rgb, uiScale)
                slot in wrongSlots -> drawSlot(x, y, size, size, TerminalSolver.`melody$wrong`.rgb, uiScale)
                r == 0 && c == correct + 1 -> {
                    val w = NVGRenderer.getTextWidth("!", 12f * uiScale, NVGRenderer.defaultFont) / 2f
                    NVGRenderer.drawText("!", x + size / 2f - w, y + size / 2f - 12f, 12f * uiScale, Mocha.Pink.argb)
                }
                r in 1..4 && r != row -> {
                    if (c !in 1..5) continue
                    drawSlot(x, y, size, size, TerminalSolver.`melody$other`.rgb, uiScale)
                }
            }
        }
    }

    override fun forSlot(slot: Int): Click? = (slot in listOf(16, 25, 34, 43)).also { if (it) click(slot, 0) }.let { null }

    override fun valid(click: Click): Boolean = false

    override fun compute(slot: Int, item: ItemStack) {
        if (item.item != Items.LIME_STAINED_GLASS_PANE) return

        TerminalAPI.currentItems.entries.firstOrNull { it.value.item == Items.MAGENTA_STAINED_GLASS_PANE }?.key?.let { TerminalAPI.`melody$correct` = it - 1 }
        TerminalAPI.`melody$button` = slot / 9 - 1
        TerminalAPI.`melody$current` = slot % 9 - 1
    }
}