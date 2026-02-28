@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.config.ui.elements.base.ISwitch
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.hud.HUDEditor
import xyz.aerii.athen.hud.HUDElement
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue

class HUDElement(
    name: String,
    private val hudElement: HUDElement,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {

    companion object {
        private val moveIcon = NVGRenderer.createImage("/assets/athen/move.svg")
    }

    private val switch = ISwitch(hudElement.enabled)
    private val `anim$iconScale` = springValue(1f, 0.3f)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        drawText(name, x + 6f, y + 8f)

        val iconX = x + width - 70f
        val iconY = y + 8f
        val iconSize = 20f
        val iconHovered = isAreaHovered(iconX, iconY, iconSize, iconSize)

        `anim$iconScale`.value = if (iconHovered) 1.15f else 1f
        val scaledSize = iconSize * `anim$iconScale`.value
        val offset = (scaledSize - iconSize) / 2f

        NVGRenderer.drawImage(moveIcon, iconX - offset, iconY - offset, scaledSize, scaledSize)
        switch.draw(x + width - 45f, y + 8f, hudElement.enabled)

        return 32f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        if (isAreaHovered(lastX + width - 70f, lastY + 8f, 20f, 20f)) {
            McClient.self.setScreen(HUDEditor)
            return true
        }

        if (isAreaHovered(lastX + width - 45f, lastY + 8f, 40f, 16f)) {
            hudElement.enabled = !hudElement.enabled
            onUpdate(configKey, hudElement.enabled)
            return true
        }

        return false
    }

    override fun getHeight() = 32f
}