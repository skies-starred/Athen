@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.Athen
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

class HelpTooltip {
    companion object {
        private val closeIcon = NVGRenderer.createImage("/assets/athen/elements/close.svg", Mocha.Text.argb)
        private val titleWidth = NVGRenderer.getTextWidth("Help", 16f, NVGRenderer.defaultFont)
        private val helpItems = listOf(
            "<yellow>Left click<r> to toggle features on/off",
            "<yellow>Right click<r> to expand feature options",
            "<yellow>Click and drag<r> headers to move panels",
            "<yellow>Hold Shift + scroll<r> to scroll horizontally",
            "<yellow>/${Athen.modId}, /${Athen.modId} help<r> to see all commands",
            "<yellow>/${Athen.modId} toggle help<r> to toggle this tooltip"
        )
    }

    var visible = false
    var collapsed = false

    private var x = 0f
    private var y = 0f
    private var screenWidth = 0
    private var dragging = false
    private var dragDeltaX = 0f
    private var dragDeltaY = 0f

    private val `anim$closeBg` = springValue(0x00000000, 0.2f)
    private val `anim$closeScale` = springValue(1f, 0.25f)
    private val `anim$headerHover` = springValue(Mocha.Base.withAlpha(0.5f), 0.2f)
    private val `anim$open` = timedValue(0f, 250L, ::easeOutQuad)

    private val width = 280f
    private val collapsedWidth = 100f
    private val headerHeight = 32f
    private val itemHeight = 22f
    private val padding = 8f
    private val screenPadding = 16f

    fun initialize(screenWidth: Int) {
        this.screenWidth = screenWidth
        visible = !Dev.clickUiHelperHidden
        collapsed = Dev.clickUiHelperCollapsed
        if (!visible) return

        y = screenPadding
        x = if (collapsed) screenWidth - collapsedWidth - screenPadding else screenWidth - width - screenPadding
        `anim$open`.value = if (collapsed) 0f else 1f
    }

    fun draw(mouseX: Float, mouseY: Float) {
        if (!visible) return

        if (!collapsed && dragging) {
            x = dragDeltaX + mouseX
            y = dragDeltaY + mouseY
        }

        val anim = `anim$open`.value
        val currentWidth = collapsedWidth + (width - collapsedWidth) * anim

        if (anim > 0f && anim < 1f) x = screenWidth - currentWidth - screenPadding

        val isHovered = isAreaHovered(x, y, currentWidth, headerHeight)
        `anim$headerHover`.value = if (isHovered && collapsed) Mocha.Base.withAlpha(0.7f) else Mocha.Base.withAlpha(0.5f)

        val fullContentHeight = getTotalHeight() - headerHeight
        val contentHeight = fullContentHeight * anim
        val totalHeight = headerHeight + contentHeight

        NVGRenderer.drawDropShadow(x, y, currentWidth, totalHeight, 10f, 3f, if (collapsed) 5f else 8f)
        NVGRenderer.drawRectangle(x, y, currentWidth, headerHeight, `anim$headerHover`.value, 5f, 5f, if (collapsed) 5f else 0f, if (collapsed) 5f else 0f)

        if (anim > 0f && contentHeight > 0) NVGRenderer.drawRectangle(x, y + headerHeight, currentWidth, contentHeight, Mocha.Surface0.withAlpha(0.5f), 0f, 0f, 5f, 5f)

        val titleX = if (collapsed && anim == 0f) x + (currentWidth - titleWidth) / 2f else x + padding
        NVGRenderer.drawText("Help", titleX, y + (headerHeight - 16f) / 2f, 16f, Mocha.Text.argb)

        if (anim <= 0f) return
        val closeButtonX = x + currentWidth - 24f
        val closeButtonY = y + 8f
        val closeButtonSize = 16f
        val isCloseHovered = isAreaHovered(closeButtonX, closeButtonY, closeButtonSize, closeButtonSize)

        `anim$closeBg`.value = if (isCloseHovered) Mocha.Red.argb else 0x00000000
        `anim$closeScale`.value = if (isCloseHovered) 1.15f else 1f

        val centerX = closeButtonX + closeButtonSize / 2f
        val centerY = closeButtonY + closeButtonSize / 2f
        val scaledSize = closeButtonSize * `anim$closeScale`.value
        val scaledX = centerX - scaledSize / 2f
        val scaledY = centerY - scaledSize / 2f

        NVGRenderer.globalAlpha(anim)
        if (`anim$closeBg`.value != 0x00000000) NVGRenderer.drawRectangle(scaledX - 2f, scaledY - 2f, scaledSize + 4f, scaledSize + 4f, `anim$closeBg`.value, 3f)

        NVGRenderer.drawImage(closeIcon, scaledX, scaledY, scaledSize, scaledSize)
        NVGRenderer.globalAlpha(1f)

        if (contentHeight <= 0f) return
        NVGRenderer.drawLine(x, y + headerHeight, x + currentWidth, y + headerHeight, 1f, Mocha.Surface0.argb)

        if (anim < 1f) NVGRenderer.pushScissor(x, y + headerHeight, currentWidth, contentHeight)

        var currentY = y + headerHeight + padding
        for (h in helpItems) {
            NVGRenderer.drawTextWrapped(h, x + padding, currentY, 14f, currentWidth - padding * 2, NVGRenderer.defaultFont)
            currentY += itemHeight
        }

        if (anim < 1f) NVGRenderer.popScissor()
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible) return false

        val anim = `anim$open`.value
        val currentWidth = collapsedWidth + (width - collapsedWidth) * anim

        if (collapsed) {
            if (button == 0 && isAreaHovered(x, y, currentWidth, headerHeight)) {
                expand()
                return true
            }
            return false
        }

        val closeButtonX = x + currentWidth - 24f
        val closeButtonY = y + 8f
        val closeButtonSize = 16f

        if (button == 0 && isAreaHovered(closeButtonX, closeButtonY, closeButtonSize, closeButtonSize)) {
            collapse()
            return true
        }

        if (button == 0 && isAreaHovered(x, y, currentWidth, headerHeight)) {
            dragDeltaX = x - mouseX
            dragDeltaY = y - mouseY
            dragging = true
            return true
        }

        return isAreaHovered(x, y, currentWidth, getTotalHeight())
    }

    fun mouseReleased(button: Int) {
        if (button == 0) dragging = false
    }

    private fun collapse() {
        collapsed = true
        Dev.clickUiHelperCollapsed = true
        `anim$open`.value = 0f
    }

    private fun expand() {
        collapsed = false
        Dev.clickUiHelperCollapsed = false
        `anim$open`.value = 1f
    }

    private fun getTotalHeight() = headerHeight + padding + (helpItems.size * itemHeight)
}