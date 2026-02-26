@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import net.minecraft.util.StringUtil
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Smoothie.play
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.sound
import xyz.aerii.athen.utils.url

class SoundElement(
    name: String,
    private var soundId: String,
    private var pitch: Double,
    private var volume: Double,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {
    private val pitchKey = "$configKey.pitch"
    private val volumeKey = "$configKey.volume"

    private var listening = false
    private var caret = soundId.length
    private var caretBlinkTime = System.currentTimeMillis()
    private var textOffset = 0f

    private var draggingPitch = false
    private var draggingVolume = false

    private val `anim$inputBg` = springValue(Mocha.Base.argb, 0.2f)
    private val `anim$inputBorder` = springValue(Mocha.Surface0.argb, 0.25f)
    private val `anim$playBg` = springValue(Mocha.Base.argb, 0.2f)
    private val `anim$playScale` = springValue(1f, 0.3f)
    private val `anim$linkBg` = springValue(Mocha.Base.argb, 0.2f)

    private val w = NVGRenderer.getTextWidth("Play", 14f, NVGRenderer.defaultFont)
    private val w0 = NVGRenderer.getTextWidth("Sounds list", 14f, NVGRenderer.defaultFont)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        var cy = y + 2f

        NVGRenderer.drawRectangle(x + 2f, cy, 2.5f, getHeight() - 4f, Mocha.Mauve.argb, 1.5f)

        drawText(name, x + 10f, cy + 2f)
        cy += 24f

        val inputX = x + 10f
        val inputW = width - 20f
        val inputH = 30f
        val inputHovered = isAreaHovered(inputX, cy, inputW, inputH)

        `anim$inputBorder`.value = if (listening) Mocha.Mauve.argb else Mocha.Surface0.argb
        `anim$inputBg`.value = when {
            listening -> Mocha.Surface2.argb
            inputHovered -> Mocha.Surface0.argb
            else -> Mocha.Base.argb
        }

        NVGRenderer.drawRectangle(inputX, cy, inputW, inputH, `anim$inputBg`.value, 5f)
        NVGRenderer.drawHollowRectangle(inputX, cy, inputW, inputH, 1f, `anim$inputBorder`.value, 5f)

        val textX = inputX + 6f
        val textY = cy + 7f
        val textAreaW = inputW - 12f

        NVGRenderer.pushScissor(textX, textY, textAreaW, 16f)
        if (soundId.isEmpty() && !listening) NVGRenderer.drawText("Sound ID...", textX, textY, 16f, Mocha.Subtext0.argb, NVGRenderer.defaultFont)
        else NVGRenderer.drawText(soundId, textX - textOffset, textY, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)

        if (listening) {
            val caretXPos = textX + NVGRenderer.getTextWidth(soundId.substring(0, caret), 16f, NVGRenderer.defaultFont) - textOffset
            caretBlinkTime = drawCaret(caretXPos, textY, 16f, caretBlinkTime)
        }

        NVGRenderer.popScissor()
        cy += inputH + 8f

        if (draggingPitch) {
            val pct = ((mouseX - (x + 10f)) / (width - 20f)).coerceIn(0f, 1f)
            pitch = pct.toDouble()
            onUpdate(pitchKey, pitch)
        }

        cy = drawSlider("Pitch", pitch, x, cy)

        if (draggingVolume) {
            val pct = ((mouseX - (x + 10f)) / (width - 20f)).coerceIn(0f, 1f)
            volume = pct.toDouble()
            onUpdate(volumeKey, volume)
        }

        cy = drawSlider("Volume", volume, x, cy)

        cy += 4f
        val btnW = (width - 26f) / 2f
        val btnH = 26f

        val playHovered = isAreaHovered(x + 10f, cy, btnW, btnH)
        `anim$playBg`.value = if (playHovered) Mocha.Surface2.argb else Mocha.Base.argb
        `anim$playScale`.value = if (playHovered) 1.03f else 1f

        val playRect = drawScaledBox(x + 10f + btnW / 2f, cy + btnH / 2f, btnW, btnH, `anim$playScale`.value, `anim$playBg`.value, Mocha.Surface0.argb, 5f, 1f)

        NVGRenderer.drawText("Play", playRect.x + playRect.w / 2f - w / 2f, playRect.y + 6f, 14f, Mocha.Text.argb, NVGRenderer.defaultFont)

        val linkBaseX = x + 16f + btnW
        val linkHovered = isAreaHovered(linkBaseX, cy, btnW, btnH)
        `anim$linkBg`.value = if (linkHovered) Mocha.Surface2.argb else Mocha.Base.argb

        drawBox(linkBaseX, cy, btnW, btnH, `anim$linkBg`.value, Mocha.Surface0.argb, 5f)
        NVGRenderer.drawText("Sounds list", linkBaseX + btnW / 2f - w0 / 2f, cy + 6f, 14f, Mocha.Text.argb, NVGRenderer.defaultFont)

        return getHeight()
    }

    private fun drawSlider(label: String, value: Double, baseX: Float, cy: Float): Float {
        val sliderX = baseX + 10f
        val sliderW = width - 20f

        drawText(label, sliderX, cy, 14f, Mocha.Subtext0.argb)
        val valText = "%.1f".format(value)
        val valW = NVGRenderer.getTextWidth(valText, 14f, NVGRenderer.defaultFont)
        NVGRenderer.drawText(valText, sliderX + sliderW - valW, cy, 14f, Mocha.Subtext0.argb, NVGRenderer.defaultFont)

        val trackY = cy + 20f
        val trackH = 6f
        NVGRenderer.drawRectangle(sliderX, trackY, sliderW, trackH, Mocha.Crust.argb, 3f)
        val pct = value.toFloat()
        if (pct > 0f) NVGRenderer.drawRectangle(sliderX, trackY, pct * sliderW, trackH, Mocha.Mauve.argb, 3f)
        NVGRenderer.drawCircle(sliderX + pct * sliderW, trackY + trackH / 2f, 6f, Mocha.Text.argb)

        return cy + 34f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        var cy = lastY + 2f + 24f

        val inputX = lastX + 10f
        val inputW = width - 20f
        val inputH = 30f

        if (isAreaHovered(inputX, cy, inputW, inputH)) {
            listening = true
            caret = soundId.length
            caretBlinkTime = System.currentTimeMillis()
            return true
        }

        listening = false

        cy += inputH + 8f

        if (isAreaHovered(lastX + 10f, cy + 20f, width - 20f, 12f)) {
            draggingPitch = true
            return true
        }

        cy += 34f

        if (isAreaHovered(lastX + 10f, cy + 20f, width - 20f, 12f)) {
            draggingVolume = true
            return true
        }

        cy += 34f + 4f

        val btnW = (width - 26f) / 2f
        val btnH = 26f

        if (isAreaHovered(lastX + 10f, cy, btnW, btnH)) {
            soundId.sound()?.play(volume.toFloat(), pitch.toFloat())
            return true
        }

        val linkX = lastX + 16f + btnW
        if (isAreaHovered(linkX, cy, btnW, btnH)) {
            "https://www.digminecraft.com/lists/sound_list_pc.php".url()
            return true
        }

        return false
    }

    override fun mouseReleased(button: Int) {
        if (button != 0) return
        draggingPitch = false
        draggingVolume = false
    }

    override fun keyTyped(typedChar: Char): Boolean {
        if (!listening) return false
        val filtered = StringUtil.filterText(typedChar.toString()).takeIf { it.isNotEmpty() } ?: return true

        soundId = soundId.substring(0, caret) + filtered + soundId.substring(caret)
        caret += filtered.length
        offset()
        onUpdate(configKey, soundId)
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int): Boolean {
        if (!listening) return false
        when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (caret > 0) {
                    soundId = soundId.removeRange(caret - 1, caret)
                    caret--
                    offset()
                    onUpdate(configKey, soundId)
                }
            }

            GLFW.GLFW_KEY_DELETE -> {
                if (caret < soundId.length) {
                    soundId = soundId.removeRange(caret, caret + 1)
                    onUpdate(configKey, soundId)
                }
            }

            GLFW.GLFW_KEY_LEFT -> if (caret > 0) caret--
            GLFW.GLFW_KEY_RIGHT -> if (caret < soundId.length) caret++
            GLFW.GLFW_KEY_HOME -> caret = 0
            GLFW.GLFW_KEY_END -> caret = soundId.length
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> listening = false

            GLFW.GLFW_KEY_V -> {
                if (OmniKeyboard.isCtrlKeyPressed) {
                    val clip = client.keyboardHandler?.clipboard ?: ""
                    val clean = StringUtil.filterText(clip)
                    if (clean.isNotEmpty()) {
                        soundId = soundId.substring(0, caret) + clean + soundId.substring(caret)
                        caret += clean.length
                        offset()
                        onUpdate(configKey, soundId)
                    }
                }
            }
        }

        offset()
        return true
    }

    private fun offset() {
        val caretPx = NVGRenderer.getTextWidth(soundId.substring(0, caret), 16f, NVGRenderer.defaultFont)
        val areaW = width - 32f

        if (caretPx - textOffset >= areaW) textOffset = caretPx - areaW
        else if (caretPx - textOffset <= 0f) textOffset = caretPx

        val totalW = NVGRenderer.getTextWidth(soundId, 16f, NVGRenderer.defaultFont)
        if (textOffset > 0 && totalW - textOffset < areaW) textOffset = (totalW - areaW).coerceAtLeast(0f)
    }

    override fun getHeight() = 165f
}
