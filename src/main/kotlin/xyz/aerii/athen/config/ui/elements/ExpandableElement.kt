@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.config.ui.panels.SectionButton
import xyz.aerii.athen.handlers.Roulette
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.brighten
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

class ExpandableElement(
    name: String,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {

    companion object {
        private val chevronIcon = NVGRenderer.createImage(Roulette.file("elements/chevron.svg").path, Mocha.Text.argb)
    }

    private var expanded = ConfigManager.getValue(configKey) as? Boolean ?: false
    private val `anim$chevron` = timedValue(if (expanded) 90f else 0f, 200, ::easeOutQuad)
    private val `anim$border` = springValue(Mocha.Surface0.argb, 0.2f)
    private val `anim$bg` = springValue(SectionButton.DISABLED_COLOR, 0.2f)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        val boxX = x + 6f
        val boxY = y + 5f
        val boxW = width - 12f
        val boxH = 28f
        val isHovered = isAreaHovered(boxX, boxY, boxW, boxH)

        `anim$bg`.value = if (isHovered) SectionButton.DISABLED_COLOR.brighten(2f) else SectionButton.DISABLED_COLOR
        `anim$border`.value = if (expanded) Mocha.Mauve.argb else Mocha.Surface0.argb
        `anim$chevron`.value = if (expanded) 90f else 0f

        NVGRenderer.drawRectangle(boxX, boxY, boxW, boxH, `anim$bg`.value, 4f)
        NVGRenderer.drawHollowRectangle(boxX, boxY, boxW, boxH, 1.5f, `anim$border`.value, 4f)
        drawText(name, boxX + 8f, boxY + 6f, 15f)

        val iconSize = 12f
        val iconX = boxX + boxW - iconSize - 8f
        val iconY = boxY + (boxH - iconSize) / 2f

        NVGRenderer.push()
        NVGRenderer.translate(iconX + iconSize / 2f, iconY + iconSize / 2f)
        NVGRenderer.rotate(Math.toRadians(`anim$chevron`.value.toDouble()).toFloat())
        NVGRenderer.translate(-iconSize / 2f, -iconSize / 2f)
        NVGRenderer.drawImage(chevronIcon, 0f, 0f, iconSize, iconSize)
        NVGRenderer.pop()

        return 38f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        val boxX = lastX + 6f
        val boxY = lastY + 5f
        if (!isAreaHovered(boxX, boxY, width - 12f, 28f)) return false

        expanded = !expanded
        onUpdate(configKey, expanded)
        return true
    }

    override fun getHeight() = 38f
}