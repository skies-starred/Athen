@file:Suppress("PrivatePropertyName", "FunctionName", "SameParameterValue")

package xyz.aerii.athen.config.ui.elements

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.deftu.omnicore.api.client.input.OmniKeyboard
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.Gradient
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue
import kotlin.math.abs
import kotlin.math.roundToInt
import java.awt.Color as JavaColor

class ColorPickerElement(
    name: String,
    initialColor: JavaColor,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {

    companion object {
        private const val MAX_HISTORY = 4
        private const val CHANGE_THRESHOLD = 15

        private val historyStorage = Scribble("config/ColorPickerHistory")

        private data class ColorData(val r: Int, val g: Int, val b: Int, val a: Int) {
            fun toColor() = JavaColor(r, g, b, a)

            companion object {
                fun fromColor(color: JavaColor) = ColorData(color.red, color.green, color.blue, color.alpha)

                val CODEC: Codec<ColorData> = RecordCodecBuilder.create { instance ->
                    instance.group(
                        Codec.INT.fieldOf("r").forGetter(ColorData::r),
                        Codec.INT.fieldOf("g").forGetter(ColorData::g),
                        Codec.INT.fieldOf("b").forGetter(ColorData::b),
                        Codec.INT.fieldOf("a").forGetter(ColorData::a)
                    ).apply(instance, ::ColorData)
                }
            }
        }

        private val globalColorHistory: MutableList<JavaColor> by lazy {
            val history by historyStorage.list("history", ColorData.CODEC, emptyList())
            history.map { it.toColor() }.toMutableList()
        }

        private fun saveHistory() {
            @Suppress("VariableNeverRead")
            var history by historyStorage.list("history", ColorData.CODEC, emptyList())
            @Suppress("AssignedValueIsNeverRead")
            history = globalColorHistory.map { ColorData.fromColor(it) }
            historyStorage.save()
        }
    }

    private var selectedColor = initialColor
    private var lastSavedColor = initialColor
    private var expanded = false

    private val `anim$expand` = timedValue(0f, 200, ::easeOutQuad)
    private val `anim$previewColor` = springValue(initialColor.rgb, 0.2f)
    private val `anim$previewScale` = springValue(1f, 0.25f)
    private val `anim$history` = List(MAX_HISTORY) { springValue(1f, 0.3f) }
    private val `anim$hexBg` = springValue(Mocha.Base.argb, 0.2f)
    private val `anim$hexBorder` = springValue(Mocha.Surface0.argb, 0.25f)

    var currentHue: Float
    var currentSaturation: Float
    var currentBrightness: Float
    var currentAlpha = initialColor.alpha / 255f

    var draggingPicker = false
    var draggingHue = false
    var draggingAlpha = false

    private var hexValue = initialColor.hex()
    private var hexCaret = hexValue.length
    private var hexFocused = false
    private var hexCaretBlink = System.currentTimeMillis()
    private var hexColorOnFocus = initialColor

    init {
        val hsb = JavaColor.RGBtoHSB(initialColor.red, initialColor.green, initialColor.blue, null)
        currentHue = hsb[0]
        currentSaturation = hsb[1]
        currentBrightness = hsb[2]
        if (globalColorHistory.isEmpty()) {
            globalColorHistory.add(initialColor)
            saveHistory()
        }
    }

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        drawText(name, x + 6f, y + 8f)

        val previewX = x + width - 36f
        val previewY = y + 6f
        val isPreviewHovered = isAreaHovered(previewX, previewY, 30f, 20f)

        `anim$previewColor`.value = selectedColor.rgb
        `anim$previewScale`.value = if (isPreviewHovered) 1.05f else 1f

        drawScaledBox(previewX + 15f, previewY + 10f, 30f, 20f, `anim$previewScale`.value, `anim$previewColor`.value, Mocha.Surface0.argb, 3f, 1f)

        val targetHeight = if (expanded) 225f else 0f
        `anim$expand`.value = targetHeight
        val containerHeight = `anim$expand`.value

        if (expanded || `anim$expand`.active) {
            if (`anim$expand`.active) NVGRenderer.pushScissor(x, y + 32f, width, containerHeight)
            drag(mouseX, mouseY, x, y)

            val pickerX = x + 6f
            val pickerY = y + 36f
            val pickerW = width - 12f
            val pickerH = 140f

            val hueColor = JavaColor.HSBtoRGB(currentHue, 1f, 1f)
            NVGRenderer.drawGradientRectangle(pickerX, pickerY, pickerW, pickerH, Mocha.Text.argb, hueColor, Gradient.`L->R`, 5f)
            NVGRenderer.drawGradientRectangle(pickerX, pickerY, pickerW + 1f, pickerH + 1f, 0x00000000, 0xFF000000.toInt(), Gradient.`T->B`, 5f)

            val indicatorX = pickerX + currentSaturation * pickerW
            val indicatorY = pickerY + (1f - currentBrightness) * pickerH
            NVGRenderer.drawHollowRectangle(indicatorX - 3f, indicatorY - 3f, 6f, 6f, 2f, Mocha.Text.argb, 2f)

            `draw$slider$hue`(pickerX, pickerY + pickerH + 5f, pickerW)
            `draw$slider$alpha`(pickerX, pickerY + pickerH + 25f, pickerW)
            `draw$hh`(x, pickerY + pickerH + 45f)
            if (`anim$expand`.active) NVGRenderer.popScissor()
        }

        return 32f + containerHeight
    }

    private fun `draw$slider$hue`(x: Float, y: Float, w: Float) {
        val steps = (w / 1f).toInt()
        val stepWidth = w / steps

        for (i in 0 until steps) {
            val hue = i.toFloat() / steps
            val rgb = JavaColor.HSBtoRGB(hue, 1f, 1f)
            NVGRenderer.drawRectangle(x + i * stepWidth, y, stepWidth, 15f, rgb, 0f)
        }

        val indicatorX = x + currentHue * w
        NVGRenderer.drawRectangle(indicatorX - 2f, y - 2f, 4f, 19f, Mocha.Text.argb, 3f)
    }

    private fun `draw$slider$alpha`(x: Float, y: Float, w: Float) {
        val opaqueColor = JavaColor(selectedColor.red, selectedColor.green, selectedColor.blue, 255).rgb
        val transparentColor = JavaColor(selectedColor.red, selectedColor.green, selectedColor.blue, 0).rgb

        NVGRenderer.drawGradientRectangle(x, y, w, 15f, transparentColor, opaqueColor, Gradient.`L->R`, 0f)

        val indicatorX = x + currentAlpha * w
        NVGRenderer.drawRectangle(indicatorX - 2f, y - 2f, 4f, 19f, Mocha.Text.argb, 2f)
    }

    private fun `draw$hh`(x: Float, y: Float) {
        val historySize = 28f
        val historySpacing = 3f
        val totalHistoryWidth = (historySize * MAX_HISTORY) + (historySpacing * (MAX_HISTORY - 1))
        val hexWidth = 110f
        val totalWidth = hexWidth + 6f + totalHistoryWidth
        val startX = x + (width - totalWidth) / 2f

        `draw$hex`(startX + 1f, y, hexWidth)

        val historyStartX = startX + hexWidth + 6f

        for (i in 0 until MAX_HISTORY.coerceAtMost(globalColorHistory.size)) {
            val histX = historyStartX + i * (historySize + historySpacing)
            val color = globalColorHistory[globalColorHistory.size - 1 - i]

            val isHovered = isAreaHovered(histX, y, historySize, historySize)
            `anim$history`[i].value = if (isHovered) 1.1f else 1f

            drawScaledBox(histX + historySize / 2f, y + historySize / 2f, historySize, historySize, `anim$history`[i].value, color.rgb, Mocha.Mantle.argb, 3f, 1f)
        }
    }

    private fun `draw$hex`(x: Float, y: Float, w: Float) {
        val h = 28f
        val focused = hexFocused
        val hovered = isAreaHovered(x, y, w, h)

        `anim$hexBg`.value = when {
            focused -> Mocha.Surface2.argb
            hovered -> Mocha.Surface0.argb
            else -> Mocha.Base.argb
        }

        `anim$hexBorder`.value = if (focused) Mocha.Mauve.argb else Mocha.Surface0.argb

        NVGRenderer.drawRectangle(x, y, w, h, `anim$hexBg`.value, 3f)
        NVGRenderer.drawHollowRectangle(x, y, w, h, 1f, `anim$hexBorder`.value, 3f)

        val textH = 16f
        val displayText = when {
            hexValue.isEmpty() && !focused -> "RRGGBBAA"
            hexValue.length <= 8 -> hexValue
            else -> hexValue.take(8)
        }

        val textW = textWidth(displayText, textH)
        val textX = x + (w - textW) / 2f
        val textY = y + (h - textH) / 2f

        NVGRenderer.pushScissor(x + 4f, y, w - 8f, h)

        val textColor = if (hexValue.isEmpty() && !focused) Mocha.Subtext0.argb else Mocha.Text.argb
        NVGRenderer.drawText(displayText, textX, textY + 1, textH, textColor, NVGRenderer.defaultFont)

        if (focused) hexCaretBlink = drawCaret(textX + textWidth(displayText.take(hexCaret.coerceIn(0, displayText.length)), textH), textY - 1f, textH + 2f, hexCaretBlink)

        NVGRenderer.popScissor()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        val previewX = lastX + width - 36f
        val previewY = lastY + 6f
        if (isAreaHovered(previewX, previewY, 30f, 20f)) {
            expanded = !expanded
            return true
        }

        if (!expanded) return false

        val pickerY = lastY + 36f
        val hexY = pickerY + 140f + 5f + 15f + 5f + 15f + 5f

        val historySize = 28f
        val historySpacing = 3f
        val totalHistoryWidth = (historySize * MAX_HISTORY) + (historySpacing * (MAX_HISTORY - 1))
        val hexWidth = 110f
        val totalWidth = hexWidth + 6f + totalHistoryWidth
        val startX = lastX + (width - totalWidth) / 2f

        if (isAreaHovered(startX + 1f, hexY, hexWidth, 28f)) {
            if (!hexFocused) {
                hexFocused = true
                hexCaretBlink = System.currentTimeMillis()
                hexColorOnFocus = selectedColor
            }
            return true
        } else if (hexFocused) {
            hexFocused = false
            if (hexColorOnFocus.sig(selectedColor)) selectedColor.add()
        }

        val historyStartX = startX + hexWidth + 6f

        for (i in 0 until MAX_HISTORY.coerceAtMost(globalColorHistory.size)) {
            val histX = historyStartX + i * (historySize + historySpacing)
            if (!isAreaHovered(histX, hexY, historySize, historySize)) continue

            globalColorHistory[globalColorHistory.size - 1 - i].set()
            return true
        }

        val pickerX = lastX + 6f
        val pickerW = width - 12f
        val pickerH = 140f

        return when {
            isAreaHovered(pickerX, pickerY, pickerW, pickerH) -> {
                draggingPicker = true
                `update$picker`(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH)
                true
            }

            isAreaHovered(pickerX, pickerY + pickerH + 5f, pickerW, 15f) -> {
                draggingHue = true
                `update$hue`(mouseX, pickerX, pickerW)
                true
            }

            isAreaHovered(pickerX, pickerY + pickerH + 25f, pickerW, 15f) -> {
                draggingAlpha = true
                `update$alpha`(mouseX, pickerX, pickerW)
                true
            }
            else -> false
        }
    }

    override fun mouseReleased(button: Int) {
        if (button != 0) return
        if (draggingPicker || draggingHue || draggingAlpha) selectedColor.add()

        draggingPicker = false
        draggingHue = false
        draggingAlpha = false
    }

    override fun keyTyped(typedChar: Char): Boolean {
        if (!hexFocused) return false

        val filtered = typedChar.toString().uppercase()
        if (!filtered.matches(Regex("[0-9A-F]"))) return false
        if (hexValue.length >= 8) return false

        hexValue += filtered
        hexCaret = hexValue.length
        hexCaretBlink = System.currentTimeMillis()

        hexValue.java()?.set(false)

        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int): Boolean {
        if (!hexFocused) return false

        return when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (hexValue.isEmpty()) return true
                hexValue = hexValue.dropLast(1)
                hexCaret = hexValue.length
                hexCaretBlink = System.currentTimeMillis()

                hexValue.java()?.set(false)
                true
            }

            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> {
                hexFocused = false
                if (hexColorOnFocus.sig(selectedColor)) selectedColor.add()
                true
            }

            GLFW.GLFW_KEY_V -> {
                if (!OmniKeyboard.isCtrlKeyPressed) return false

                val clean = client.keyboardHandler.clipboard.trim().removePrefix("#").take(8).uppercase()
                val len = clean.length

                if (len !in 6..8) return true
                if (!clean.all { it in '0'..'9' || it in 'A'..'F' }) return true

                hexValue = clean
                hexCaret = len
                hexCaretBlink = System.currentTimeMillis()

                clean.java()?.set(false)
                true
            }

            else -> false
        }
    }

    override fun getHeight() = 32f + `anim$expand`.value

    private fun drag(mouseX: Float, mouseY: Float, x: Float, y: Float) {
        val pickerX = x + 6f
        val pickerY = y + 36f
        val pickerW = width - 12f
        val pickerH = 140f

        when {
            draggingPicker -> `update$picker`(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH)
            draggingHue -> `update$hue`(mouseX, pickerX, pickerW)
            draggingAlpha -> `update$alpha`(mouseX, pickerX, pickerW)
        }
    }

    private fun `update$picker`(mouseX: Float, mouseY: Float, pickerX: Float, pickerY: Float, pickerW: Float, pickerH: Float) {
        currentSaturation = ((mouseX - pickerX) / pickerW).coerceIn(0f, 1f)
        currentBrightness = (1f - (mouseY - pickerY) / pickerH).coerceIn(0f, 1f)
        `update$color`()
    }

    private fun `update$hue`(mouseX: Float, pickerX: Float, pickerW: Float) {
        currentHue = ((mouseX - pickerX) / pickerW).coerceIn(0f, 1f)
        `update$color`()
    }

    private fun `update$alpha`(mouseX: Float, pickerX: Float, pickerW: Float) {
        currentAlpha = ((mouseX - pickerX) / pickerW).coerceIn(0f, 1f)
        `update$color`()
    }

    private fun `update$color`() {
        val rgb = JavaColor.HSBtoRGB(currentHue, currentSaturation, currentBrightness)
        val baseColor = JavaColor(rgb)
        val alpha = (currentAlpha * 255).roundToInt().coerceIn(0, 255)
        selectedColor = JavaColor(baseColor.red, baseColor.green, baseColor.blue, alpha)
        hexValue = selectedColor.hex()
        hexCaret = hexValue.length
        onUpdate(configKey, selectedColor)
    }

    private fun JavaColor.set(updateHistory: Boolean = true) {
        val hsb = JavaColor.RGBtoHSB(red, green, blue, null)
        currentHue = hsb[0]
        currentSaturation = hsb[1]
        currentBrightness = hsb[2]
        currentAlpha = alpha / 255f
        selectedColor = this
        if (updateHistory) lastSavedColor = this
        hexValue = hex()
        hexCaret = hexValue.length
        onUpdate(configKey, this)
    }

    private fun JavaColor.add() {
        if (!lastSavedColor.sig(this)) return
        globalColorHistory.remove(this)
        globalColorHistory.add(this)
        if (globalColorHistory.size > MAX_HISTORY) globalColorHistory.removeAt(0)
        lastSavedColor = this
        saveHistory()
    }

    private fun JavaColor.sig(n: JavaColor) =
        abs(red - n.red) >= CHANGE_THRESHOLD || abs(green - n.green) >= CHANGE_THRESHOLD || abs(blue - n.blue) >= CHANGE_THRESHOLD || abs(alpha - n.alpha) >= CHANGE_THRESHOLD

    private fun JavaColor.hex() = "%02X%02X%02X%02X".format(red, green, blue, alpha)

    private fun String.java(): JavaColor? {
        val str = trim().removePrefix("#")
        return try {
            when (str.length) {
                6 -> JavaColor(
                    str.substring(0, 2).toInt(16),
                    str.substring(2, 4).toInt(16),
                    str.substring(4, 6).toInt(16),
                    255
                )
                8 -> JavaColor(
                    str.substring(0, 2).toInt(16),
                    str.substring(2, 4).toInt(16),
                    str.substring(4, 6).toInt(16),
                    str.substring(6, 8).toInt(16)
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}