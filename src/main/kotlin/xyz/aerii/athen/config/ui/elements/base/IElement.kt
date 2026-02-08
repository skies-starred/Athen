@file:Suppress("unchecked_cast")

package xyz.aerii.athen.config.ui.elements.base

import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.config.ui.elements.*
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue
import java.awt.Color

class IElement(private val data: ConfigManager.ElementData, onUpdate: (String, Any) -> Unit) {
    private val heightAnim = timedValue(0f, 200, ::easeOutQuad)
    private val opacityAnim = springValue(if (isVisible) 1f else 0f, 0.25f)
    private var wasVisible = false

    val isVisible: Boolean
        get() = data.visibilityDependency?.invoke() ?: true

    private val uiElement = when (data) {
        is ConfigManager.ElementData.Switch -> SwitchElement(data.name, ConfigManager.getValue(data.key) as? Boolean ?: data.default, data.key, onUpdate)
        is ConfigManager.ElementData.Slider -> SliderElement(data.name, (ConfigManager.getValue(data.key) as? Number)?.toDouble() ?: data.default, data.min, data.max, data.showDouble, data.key, onUpdate)
        is ConfigManager.ElementData.Dropdown -> DropdownElement(data.name, data.options, ConfigManager.getValue(data.key) as? Int ?: data.default, data.key, onUpdate)
        is ConfigManager.ElementData.TextInput -> TextInputElement(data.name, ConfigManager.getValue(data.key) as? String ?: data.default, data.maxLength, data.key, onUpdate, data.placeholder)
        is ConfigManager.ElementData.ColorPicker -> ColorPickerElement(data.name, ConfigManager.getValue(data.key) as? Color ?: data.default, data.key, onUpdate)
        is ConfigManager.ElementData.Keybind -> KeybindElement(data.name, ConfigManager.getValue(data.key) as? Int ?: data.default, data.key, onUpdate)
        is ConfigManager.ElementData.MultiCheckbox -> MultiCheckboxElement(data.name, data.options, ConfigManager.getValue(data.key) as? List<Int> ?: data.default, data.key, onUpdate)
        is ConfigManager.ElementData.Button -> ButtonElement(data.name, data.onClick)
        is ConfigManager.ElementData.TextParagraph -> TextParagraphElement(data.text)
        is ConfigManager.ElementData.HUDElement -> HUDElement(data.name, data.hudElement, data.key, onUpdate)
        is ConfigManager.ElementData.Expandable -> ExpandableElement(data.name, data.key, onUpdate)
    }

    fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        if (isVisible != wasVisible) {
            val endHeight = if (isVisible) uiElement.getHeight() else 0f
            heightAnim.value = endHeight
            wasVisible = isVisible
        }

        if (!isVisible && !heightAnim.active) return 0f

        val displayHeight = getHeight()
        if (displayHeight <= 0f) return 0f

        opacityAnim.value = if (isVisible) 1f else 0f
        val opacity = opacityAnim.value

        if (heightAnim.active || opacity < 1f) {
            NVGRenderer.pushScissor(x, y, 240f, displayHeight)
            NVGRenderer.globalAlpha(opacity)
        }

        uiElement.draw(x, y, mouseX, mouseY)

        if (heightAnim.active || opacity < 1f) {
            NVGRenderer.globalAlpha(1f)
            NVGRenderer.popScissor()
        }

        return displayHeight
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int) =
        if (isVisible || heightAnim.active) uiElement.mouseClicked(mouseX, mouseY, button) else false

    fun mouseReleased(button: Int) =
        if (isVisible || heightAnim.active) uiElement.mouseReleased(button) else {}

    fun keyTyped(char: Char) =
        if (isVisible || heightAnim.active) uiElement.keyTyped(char) else false

    fun keyPressed(keyCode: Int, scanCode: Int) =
        if (isVisible || heightAnim.active) uiElement.keyPressed(keyCode, scanCode) else false

    fun getHeight() = when {
        heightAnim.active -> heightAnim.value
        wasVisible -> uiElement.getHeight()
        else -> 0f
    }
}
