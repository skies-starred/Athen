package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.config.ui.elements.base.ISwitch
import xyz.aerii.athen.handlers.Scurry.isAreaHovered

class SwitchElement(
    name: String,
    private var value: Boolean,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {

    private val switch = ISwitch(value)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y
        drawText(name, x + 6f, y + 8f)
        switch.draw(x + width - 45f, y + 8f, value)
        return 32f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0 || !isAreaHovered(lastX + width - 45f, lastY + 8f, 40f, 16f)) return false
        value = !value
        onUpdate(configKey, value)
        return true
    }

    override fun getHeight() = 32f
}