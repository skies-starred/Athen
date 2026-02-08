package xyz.aerii.athen.modules.common.carry

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.brighten
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.plural
import xyz.aerii.athen.utils.render.animations.SpringValue
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

abstract class ICarryGUI<T : ITrackedCarry>(val screenName: String) : Scram(screenName) {
    private var scrollOffset = 0f
    private val entries = mutableListOf<CarryEntry>()
    private val tooltips = mutableListOf<TooltipEntry>()
    private val open = timedValue(0.8f, 300L, ::easeOutQuad)
    private val text0 = "No carries being tracked"
    private var width0: Float = 0f

    data class TooltipEntry(val player: String, var amount: Int, val action: ActionType, var timestamp: Long = System.currentTimeMillis()) {
        enum class ActionType(val group: Int) {
            COUNT_INCREASE(0),
            COUNT_DECREASE(0),
            TOTAL_INCREASE(1),
            TOTAL_DECREASE(1),
            CARRY_REMOVED(-1);
        }

        fun display(): String {
            val carry = amount.plural("carry", "carries")

            return when (action) {
                ActionType.COUNT_INCREASE -> "Added $amount completed $carry for <aqua>$player"
                ActionType.COUNT_DECREASE -> "Decreased $amount completed $carry for <aqua>$player"
                ActionType.TOTAL_INCREASE -> "Added $amount $carry for <aqua>$player"
                ActionType.TOTAL_DECREASE -> "Decreased $amount $carry for <aqua>$player"
                ActionType.CARRY_REMOVED -> "Removed <aqua>$player<r> from tracking"
            }
        }
    }

    protected abstract fun carries(): Map<String, T>
    protected abstract fun persist()
    protected abstract fun remove(player: String)

    override fun onScramInit() {
        width0 = NVGRenderer.getTextWidth(text0, 18f, NVGRenderer.defaultFont)
        entries.clear()
        for ((_, c) in carries()) entries.add(CarryEntry(c))
        open.value = 1f
    }

    override fun onScramClose() = persist()

    override fun isPauseScreen() = false

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        tooltips.removeIf { System.currentTimeMillis() - it.timestamp > 5000 }

        NVGSpecialRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
            val width = client.window.width
            val height = client.window.height
            val progress = open.value
            val scale = 0.8f + (progress - 0.8f) * (1f - 0.8f) / (1f - 0.8f)
            val alpha = (progress - 0.8f) / (1f - 0.8f)

            NVGRenderer.push()
            NVGRenderer.translate(width / 2f, height / 2f)
            NVGRenderer.scale(scale.coerceIn(0.8f, 1f), scale.coerceIn(0.8f, 1f))
            NVGRenderer.translate(-width / 2f, -height / 2f)
            NVGRenderer.globalAlpha(alpha.coerceIn(0f, 1f))

            drawPanel(width, height)

            NVGRenderer.pop()
        }
    }

    private fun drawPanel(width: Int, height: Int) {
        val panelWidth = 600f
        val panelHeight = 500f
        val panelX = (width - panelWidth) / 2f
        val panelY = (height - panelHeight) / 2f

        NVGRenderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 15f, 4f, 10f)
        NVGRenderer.drawRectangle(panelX, panelY, panelWidth, 50f, Mocha.Base.argb, 10f, 10f, 0f, 0f)
        NVGRenderer.drawRectangle(panelX, panelY + 50f, panelWidth, panelHeight - 50f, Mocha.Surface0.withAlpha(0.04f), 0f, 0f, 10f, 10f)

        NVGRenderer.drawText(screenName, panelX + 15f, panelY + 15f, 22f, Mocha.Text.argb)

        if (entries.isEmpty()) {
            NVGRenderer.drawText(text0, panelX + (panelWidth - width0) / 2f, panelY + panelHeight / 2f - 9f, 18f, Mocha.Subtext0.argb)
        } else {
            val contentHeight = entries.size * 80f
            val maxScroll = -(contentHeight - (panelHeight - 60f)).coerceAtLeast(0f)
            scrollOffset = scrollOffset.coerceIn(maxScroll, 0f)

            NVGRenderer.pushScissor(panelX + 10f, panelY + 60f, panelWidth - 20f, panelHeight - 70f)

            var currentY = panelY + 60f + scrollOffset
            for (e in entries) {
                e.draw(panelX + 10f, currentY, panelWidth - 20f)
                currentY += 80f
            }

            NVGRenderer.popScissor()
        }

        drawTooltips(panelX, panelY + panelHeight + 10f, panelWidth)
    }

    private fun drawTooltips(x: Float, y: Float, width: Float) {
        val t = tooltips.takeLast(3)

        if (t.isEmpty()) {
            val defaultText = "<yellow>Left click<r> to change count | <yellow>Right click<r> to change total"
            val textWidth = NVGRenderer.getWrappedTextWidth(defaultText, 14f, width - 20f)
            val boxPadding = 10f
            val boxWidth = textWidth + boxPadding * 2
            val boxHeight = 24f
            val boxX = x + (width - boxWidth) / 2f

            NVGRenderer.drawRectangle(boxX, y, boxWidth, boxHeight, Mocha.Base.withAlpha(0.8f), 5f)
            NVGRenderer.drawHollowRectangle(boxX, y, boxWidth, boxHeight, 1f, Mocha.Surface0.argb, 5f)
            NVGRenderer.drawTextWrapped(defaultText, boxX + boxPadding, y + 5f, 14f, width - 20f)
        } else {
            var currentY = y
            for (d in t) {
                val text = d.display()
                val textWidth = NVGRenderer.getWrappedTextWidth(text, 14f, width - 20f)
                val boxWidth = textWidth + 10f * 2
                val boxHeight = 24f
                val boxX = x + (width - boxWidth) / 2f

                NVGRenderer.drawRectangle(boxX, currentY, boxWidth, boxHeight, Mocha.Base.withAlpha(0.8f), 5f)
                NVGRenderer.drawHollowRectangle(boxX, currentY, boxWidth, boxHeight, 1f, Mocha.Surface0.argb, 5f)
                NVGRenderer.drawTextWrapped(text, boxX + 10f, currentY + 5f, 14f, width - 20f)
                currentY += boxHeight + 5f
            }
        }
    }

    protected fun addTooltip(player: String, amount: Int, action: TooltipEntry.ActionType) {
        val now = System.currentTimeMillis()
        val existing = tooltips.lastOrNull { it.player == player && it.action == action && it.action.group == action.group && it.action.group != -1 && now - it.timestamp < 2000 }

        if (existing != null && action.group != -1) {
            existing.amount += amount
            existing.timestamp = now
        } else {
            if (tooltips.size >= 3) tooltips.removeAt(0)
            tooltips.add(TooltipEntry(player, amount, action))
        }
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        if (entries.isEmpty()) return false

        val amount = (vertical * 20).toFloat()
        val contentHeight = entries.size * 80f
        val panelHeight = 500f
        val maxScroll = -(contentHeight - (panelHeight - 60f)).coerceAtLeast(0f)

        scrollOffset = (scrollOffset + amount).coerceIn(maxScroll, 0f)
        return true
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        for (e in entries) if (e.mouseClicked(button)) return true
        return super.onScramMouseClick(mouseX, mouseY, button)
    }

    private inner class CarryEntry(val carry: T) {
        private val buttonSize = 32f
        private val buttonSpacing = 8f

        private val addButtonAnim = springValue(Mocha.Green.argb, 0.2f)
        private val subButtonAnim = springValue(Mocha.Peach.argb, 0.2f)
        private val removeButtonAnim = springValue(Mocha.Red.argb, 0.2f)

        private val addScaleAnim = springValue(1f, 0.25f)
        private val subScaleAnim = springValue(1f, 0.25f)
        private val removeScaleAnim = springValue(1f, 0.25f)

        private var lastX = 0f
        private var lastY = 0f
        private var lastWidth = 0f

        fun draw(x: Float, y: Float, width: Float) {
            lastX = x
            lastY = y
            lastWidth = width

            NVGRenderer.drawRectangle(x, y, width, 70f, Mocha.Base.withAlpha(0.5f), 8f)
            NVGRenderer.drawHollowRectangle(x, y, width, 70f, 1f, Mocha.Surface0.argb, 8f)

            NVGRenderer.drawText(carry.player, x + 15f, y + 8f, 18f, Mocha.Text.argb, NVGRenderer.defaultFont)

            val typeText = carry.getShortType()
            NVGRenderer.drawText(typeText, x + 15f, y + 32f, 14f, Mocha.Subtext0.argb, NVGRenderer.defaultFont)

            val progressText = "${carry.completed}/${carry.total}"
            NVGRenderer.drawText(progressText, x + 15f, y + 48f, 14f, Mocha.Text.argb, NVGRenderer.defaultFont)

            val buttonsStartX = x + width - (buttonSize * 3 + buttonSpacing * 2 + 15f)

            drawButton(buttonsStartX, y + 19f, "+1", addButtonAnim, addScaleAnim)
            drawButton(buttonsStartX + buttonSize + buttonSpacing, y + 19f, "-1", subButtonAnim, subScaleAnim)
            drawButton(buttonsStartX + (buttonSize + buttonSpacing) * 2, y + 19f, "×", removeButtonAnim, removeScaleAnim)
        }

        private fun drawButton(x: Float, y: Float, text: String, colorAnim: SpringValue<Int>, scaleAnim: SpringValue<Float>) {
            val isHovered = isAreaHovered(x, y, buttonSize, buttonSize)

            val baseColor = when (colorAnim) {
                addButtonAnim -> Mocha.Base.withAlpha(0.5f)
                subButtonAnim -> Mocha.Base.withAlpha(0.5f)
                else -> Mocha.Red.withAlpha(0.49f)
            }

            colorAnim.value = if (isHovered) baseColor.brighten(2f) else baseColor
            val color = colorAnim.value

            scaleAnim.value = if (isHovered) 1.08f else 1f
            val scale = scaleAnim.value

            val centerX = x + buttonSize / 2f
            val centerY = y + buttonSize / 2f
            val scaledSize = buttonSize * scale
            val scaledX = centerX - scaledSize / 2f
            val scaledY = centerY - scaledSize / 2f

            NVGRenderer.drawRectangle(scaledX, scaledY, scaledSize, scaledSize, color, 6f)

            val textWidth = NVGRenderer.getTextWidth(text, 16f, NVGRenderer.defaultFont)
            NVGRenderer.drawText(text, centerX - textWidth / 2f, centerY - 8f, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)
        }

        fun mouseClicked(button: Int): Boolean {
            val startX = lastX + lastWidth - (buttonSize * 3 + buttonSpacing * 2 + 15f)

            when {
                isAreaHovered(startX, lastY + 19f, buttonSize, buttonSize) -> {
                    when (button) {
                        0 -> {
                            if (carry.completed >= carry.total - 1) return false
                            carry.completed++
                            persist()
                            addTooltip(carry.player, 1, TooltipEntry.ActionType.COUNT_INCREASE)
                        }
                        1 -> {
                            carry.total++
                            persist()
                            addTooltip(carry.player, 1, TooltipEntry.ActionType.TOTAL_INCREASE)
                        }
                    }
                    return true
                }

                isAreaHovered(startX + buttonSize + buttonSpacing, lastY + 19f, buttonSize, buttonSize) -> {
                    when (button) {
                        0 -> if (carry.completed > 0) {
                            carry.completed--
                            persist()
                            addTooltip(carry.player, 1, TooltipEntry.ActionType.COUNT_DECREASE)
                        }
                        1 -> if (carry.total > 1) {
                            carry.total--
                            persist()
                            addTooltip(carry.player, 1, TooltipEntry.ActionType.TOTAL_DECREASE)
                        }
                    }
                    return true
                }

                isAreaHovered(startX + (buttonSize + buttonSpacing) * 2, lastY + 19f, buttonSize, buttonSize) -> {
                    remove(carry.player)
                    entries.removeIf { it.carry.player == carry.player }
                    addTooltip(carry.player, 0, TooltipEntry.ActionType.CARRY_REMOVED)
                    return true
                }
            }

            return false
        }
    }
}