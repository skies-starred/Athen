package xyz.aerii.athen.config.ui.panels

import xyz.aerii.athen.config.Category
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer

class Panel(
    private val category: Category,
    features: List<ConfigManager.Feature>,
    initialX: Float,
    initialY: Float,
    private val onUpdate: (String, Any) -> Unit
) {
    companion object {
        const val WIDTH = 240f
        const val HEADER_HEIGHT = 32f
    }

    var x = initialX
    var y = initialY
    var visible = true
    var scrollOffset = 0f

    private var dragging = false
    private var dragDeltaX = 0f
    private var dragDeltaY = 0f

    val sections = features.sortedBy { it.name }.mapIndexed { index, feature ->
        SectionButton(feature, this, onUpdate, index == features.size - 1)
    }

    private val titleWidth = NVGRenderer.getTextWidth(category.displayName, 22f, NVGRenderer.defaultFont)

    fun draw(mouseX: Float, mouseY: Float) {
        if (!visible) return

        if (dragging) {
            x = dragDeltaX + mouseX
            y = dragDeltaY + mouseY
        }

        val visibleSections = sections.filter { it.visible }
        val fullContentHeight = visibleSections.sumOf { it.getHeight().toDouble() }.toFloat()
        val visibleContentHeight = (fullContentHeight + scrollOffset).coerceAtLeast(0f)
        val displayHeight = (HEADER_HEIGHT + visibleContentHeight).coerceAtLeast(HEADER_HEIGHT)

        NVGRenderer.drawDropShadow(x, y, WIDTH, displayHeight, 10f, 3f, 5f)
        NVGRenderer.drawRectangle(x, y + HEADER_HEIGHT, WIDTH, displayHeight - HEADER_HEIGHT, Mocha.Surface0.withAlpha(0.04f), 0f, 0f, 5f, 5f)
        NVGRenderer.drawRectangle(x, y, WIDTH, HEADER_HEIGHT, Mocha.Base.argb, 5f, 5f, 0f, 0f)
        NVGRenderer.drawText(category.displayName, x + WIDTH / 2f - titleWidth / 2f, y + HEADER_HEIGHT / 2f - 11f, 22f, Mocha.Text.argb, NVGRenderer.defaultFont)

        if (scrollOffset != 0f) NVGRenderer.pushScissor(x - 2, y + HEADER_HEIGHT, WIDTH + 4, visibleContentHeight)

        var currentY = y + HEADER_HEIGHT + scrollOffset
        for (s in visibleSections) currentY += s.draw(x, currentY, mouseX, mouseY)

        if (scrollOffset != 0f) NVGRenderer.popScissor()
    }

    fun handleScroll(amount: Int): Boolean {
        val contentHeight = sections.filter { it.visible }.sumOf { it.getHeight().toDouble() }.toFloat()
        if (!isAreaHovered(x, y, WIDTH, HEADER_HEIGHT + contentHeight)) return false

        val maxScroll = -(contentHeight - 72f).coerceAtLeast(0f)
        scrollOffset = (scrollOffset + amount).coerceIn(maxScroll, 0f)
        return true
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible) return false

        if (isAreaHovered(x, y, WIDTH, HEADER_HEIGHT) && button == 0) {
            dragDeltaX = x - mouseX
            dragDeltaY = y - mouseY
            dragging = true
            return true
        }

        return sections.reversed().any { it.visible && it.mouseClicked(mouseX, mouseY, button) }
    }

    fun mouseReleased(button: Int) {
        if (button == 0) dragging = false
        for (s in sections) if (s.visible) s.mouseReleased(button)
    }

    fun keyTyped(char: Char) = sections.reversed().any { it.visible && it.keyTyped(char) }

    fun keyPressed(keyCode: Int, scanCode: Int) = sections.reversed().any { it.visible && it.keyPressed(keyCode, scanCode) }

    fun applySearchFilter(query: String) {
        if (query.isEmpty()) {
            sections.forEach { it.visible = true }
            visible = true
            return
        }

        sections.forEach { it.visible = it.matchesSearch(query) }
        visible = sections.any { it.visible }
    }
}