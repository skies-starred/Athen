@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.animations.springValue
import java.awt.Color

class KeybindElement(
    name: String,
    private var selectedKey: Int,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {

    private var listening = false
    private val `anim$bg` = springValue(Mocha.Base.argb, 0.2f)
    private val `anim$border` = springValue(Mocha.Surface0.argb, 0.25f)
    private val `anim$borderWidth` = springValue(1.5f, 0.3f)
    private val `anim$color` = springValue(Mocha.Text.argb, 0.2f)
    private val `anim$scale` = springValue(1f, 0.25f)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        drawText(name, x + 6f, y + 8f)

        val keyName = getKeyName(selectedKey)
        val buttonW = textWidth(keyName) + 12f
        val buttonX = x + width - 20f - textWidth(keyName)
        val isHovered = isAreaHovered(buttonX, y + 8f, buttonW, 20f)

        `anim$bg`.value = when {
            listening -> Color(Mocha.Peach.argb).darker().rgb
            isHovered -> Mocha.Surface2.argb
            else -> Mocha.Base.argb
        }
        `anim$border`.value = if (listening) Mocha.Peach.argb else Mocha.Surface0.argb
        `anim$borderWidth`.value = if (listening) 2.5f else 1.5f
        `anim$color`.value = if (listening) Mocha.Crust.argb else Mocha.Text.argb
        `anim$scale`.value = if (listening) 1.05f else 1f

        val rect = drawScaledBox(buttonX + buttonW / 2f, y + 8f + 10f, buttonW, 20f, `anim$scale`.value, `anim$bg`.value, `anim$border`.value, 5f, `anim$borderWidth`.value)
        val textX = rect.x + (rect.w - textWidth(keyName)) / 2f
        drawText(keyName, textX, rect.y + if (listening) 4f else 2f, color = `anim$color`.value)

        return 32f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val keyName = getKeyName(selectedKey)
        val buttonW = textWidth(keyName) + 12f
        val buttonX = lastX + width - 20f - textWidth(keyName)

        if (listening) {
            setKey(-100 + button)
            return false
        } else if (button == 0 && isAreaHovered(buttonX, lastY + 8f, buttonW, 20f)) {
            listening = true
            return true
        }

        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int): Boolean {
        if (!listening) return false

        when (keyCode) {
            GLFW.GLFW_KEY_ESCAPE -> listening = false
            GLFW.GLFW_KEY_BACKSPACE -> setKey(0)
            GLFW.GLFW_KEY_ENTER -> listening = false
            else -> setKey(keyCode)
        }

        return true
    }

    override fun getHeight() = 32f

    private fun setKey(keyCode: Int) {
        selectedKey = keyCode
        onUpdate(configKey, selectedKey)
        listening = false
    }

    private fun getKeyName(keyCode: Int) = when (keyCode) {
        0 -> "None"
        in -100..-1 -> "Mouse ${keyCode + 100}"
        else -> InputConstants.Type.KEYSYM.getOrCreate(keyCode).displayName.string.let {
            if (it.length == 1) it.uppercase() else it
        }
    }
}