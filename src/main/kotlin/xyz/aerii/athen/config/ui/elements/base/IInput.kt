/*
BSD 3-Clause License

Copyright (c) 2025, odtheking

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements.base

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import net.minecraft.util.StringUtil
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue
import kotlin.math.max
import kotlin.math.min

open class IInput(
    name: String,
    initialValue: String,
    configKey: String,
    onUpdate: (String, Any) -> Unit,
    protected val placeholder: String = "",
    protected val textColor: Int = TextColor.WHITE,
    protected val placeholderColor: Int = Mocha.Subtext0.argb,
    protected val caretColor: Int = TextColor.WHITE,
    protected val selectionColor: Int = TextColor.BLUE
) : IBaseUI(name, configKey, onUpdate) {

    var value = initialValue
    var caret = value.length
        set(value) {
            if (field == value) return
            field = value.coerceIn(0, this.value.length)
            caretBlinkTime = System.currentTimeMillis()
        }

    var selection = value.length
    var listening = false

    private val `anim$hover` = springValue(Mocha.Base.argb, 0.15f)
    private var selectionWidth = 0f
    private var textOffset = 0f
    private var caretX = 0f
    private var caretBlinkTime = System.currentTimeMillis()
    private var lastClickTime = 0L
    private var dragging = false
    private var clickCount = 1
    private val history = mutableListOf<String>()
    private var historyIndex = -1
    private var lastSavedText = ""
    private var previousMousePos = 0f to 0f

    protected var inputX = 0f
    protected var inputY = 0f
    protected var inputWidth = 0f
    protected var inputHeight = 24f

    init {
        saveState()
    }

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        drawText(name, x + 6f, y + 6f)

        val isHovered = isAreaHovered(x + 10f, y + 28f, width - 20f, inputHeight)
        `anim$hover`.value = if (isHovered) Mocha.Surface0.argb else Mocha.Base.argb

        NVGRenderer.drawRectangle(x + 10f, y + 28f, width - 20f, inputHeight, `anim$hover`.value, 5f)
        NVGRenderer.drawHollowRectangle(x + 10f, y + 28f, width - 20f, inputHeight, 1f, Mocha.Surface0.argb, 5f)

        inputX = x + 14f
        inputY = y + 30f
        inputWidth = width - 28f
        inputHeight = 20f

        drawInput(mouseX, mouseY)
        return 58f
    }

    protected open fun drawInput(mouseX: Float, mouseY: Float) {
        if (previousMousePos != mouseX to mouseY) mouseDragged(mouseX)
        previousMousePos = mouseX to mouseY

        NVGRenderer.pushScissor(inputX, inputY, inputWidth, inputHeight)

        if (selectionWidth != 0f) NVGRenderer.drawRectangle(inputX + caretX - textOffset, inputY, selectionWidth, inputHeight, selectionColor, 4f)
        if (listening) caretBlinkTime = drawCaret(inputX + caretX - textOffset, inputY, inputHeight, caretBlinkTime, caretColor)

        if (value.isEmpty() && placeholder.isNotEmpty() && !listening) NVGRenderer.drawText(placeholder, inputX, inputY + 2f, inputHeight - 2, placeholderColor, NVGRenderer.defaultFont)
        else NVGRenderer.drawText(value, inputX - textOffset, inputY + 2f, inputHeight - 2, textColor, NVGRenderer.defaultFont)

        NVGRenderer.popScissor()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isAreaHovered(inputX, inputY - 2f, inputWidth, inputHeight + 4f)) {
            resetState()
            return false
        }

        if (button != 0) return false

        listening = true
        dragging = true

        val current = System.currentTimeMillis()
        if (current - lastClickTime < 200) clickCount++ else clickCount = 1
        lastClickTime = current

        when (clickCount) {
            1 -> {
                caretFromMouse(mouseX)
                clearSelection()
            }
            2 -> selectWord()
            3 -> selectAll()
            4 -> clickCount = 0
        }
        return true
    }

    override fun mouseReleased(button: Int) {
        if (button == 0) dragging = false
    }

    private fun mouseDragged(mouseX: Float) {
        if (dragging) caretFromMouse(mouseX)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int): Boolean {
        if (!listening) return false

        val handled = when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> handleBackspace()
            GLFW.GLFW_KEY_DELETE -> handleDelete()
            GLFW.GLFW_KEY_RIGHT -> handleRight()
            GLFW.GLFW_KEY_LEFT -> handleLeft()
            GLFW.GLFW_KEY_HOME -> handleHome()
            GLFW.GLFW_KEY_END -> handleEnd()
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> {
                listening = false
                true
            }
            else -> handleControlKeys(keyCode)
        }

        if (handled) updateCaretPosition()
        return handled
    }

    override fun keyTyped(typedChar: Char): Boolean {
        if (!listening) return false
        insert(StringUtil.filterText(typedChar.toString()))
        return true
    }

    private fun handleBackspace(): Boolean {
        val hadContent = selection != caret || caret != 0

        if (selection != caret) {
            deleteSelection()
        } else if (OmniKeyboard.isShiftKeyPressed) {
            val previousSpace = getPreviousSpace()
            value = value.remove(previousSpace, caret)
            caret = previousSpace
        } else if (caret != 0) {
            value = value.dropAt(caret, -1)
            caret--
        }

        clearSelection()
        updateCaretPosition()
        onUpdate(configKey, value)
        return hadContent
    }

    private fun handleDelete(): Boolean {
        val hadContent = selection != caret || caret != value.length

        if (selection != caret) deleteSelection()
        else if (OmniKeyboard.isCtrlKeyPressed) value = value.remove(caret, getNextSpace())
        else if (caret != value.length) value = value.dropAt(caret, 1)

        clearSelection()
        updateCaretPosition()
        onUpdate(configKey, value)
        return hadContent
    }

    private fun handleRight(): Boolean {
        if (caret == value.length) return false
        caret = if (OmniKeyboard.isCtrlKeyPressed) getNextSpace() else caret + 1
        if (!OmniKeyboard.isShiftKeyPressed) selection = caret
        return true
    }

    private fun handleLeft(): Boolean {
        if (caret == 0) return false
        caret = if (OmniKeyboard.isCtrlKeyPressed) getPreviousSpace() else caret - 1
        if (!OmniKeyboard.isShiftKeyPressed) selection = caret
        return true
    }

    private fun handleHome(): Boolean {
        caret = 0
        if (!OmniKeyboard.isShiftKeyPressed) selection = caret
        return true
    }

    private fun handleEnd(): Boolean {
        caret = value.length
        if (!OmniKeyboard.isShiftKeyPressed) selection = caret
        return true
    }

    private fun handleControlKeys(keyCode: Int): Boolean {
        if (!OmniKeyboard.isCtrlKeyPressed || OmniKeyboard.isShiftKeyPressed) return false

        return when (keyCode) {
            GLFW.GLFW_KEY_V -> {
                client.keyboardHandler?.clipboard?.let { insert(it) }
                true
            }
            GLFW.GLFW_KEY_C -> {
                if (caret != selection) {
                    client.keyboardHandler?.clipboard = value.sub(caret, selection)
                    true
                } else false
            }
            GLFW.GLFW_KEY_X -> {
                if (caret != selection) {
                    client.keyboardHandler?.clipboard = value.sub(caret, selection)
                    deleteSelection()
                    true
                } else false
            }
            GLFW.GLFW_KEY_A -> {
                selectAll()
                true
            }
            GLFW.GLFW_KEY_W -> {
                selectWord()
                true
            }
            GLFW.GLFW_KEY_Z -> {
                undo()
                true
            }
            GLFW.GLFW_KEY_Y -> {
                redo()
                true
            }
            else -> false
        }
    }

    private fun insert(string: String) {
        if (caret != selection) {
            value = value.remove(caret, selection)
            caret = if (selection > caret) caret else selection
        }
        val tl = value.length
        value = value.sub(0, caret) + string + value.substring(caret)
        if (value.length != tl) caret += string.length
        clearSelection()
        updateCaretPosition()
        saveState()
        onUpdate(configKey, value)
    }

    private fun deleteSelection() {
        if (caret == selection) return
        value = value.remove(caret, selection)
        caret = if (selection > caret) caret else selection
        saveState()
        onUpdate(configKey, value)
    }

    private fun caretFromMouse(mouseX: Float) {
        val mx = mouseX - inputX + textOffset
        var currWidth = 0f
        var newCaret = 0

        for (index in value.indices) {
            val charWidth = textWidth(value[index].toString(), inputHeight - 2)
            if ((currWidth + charWidth / 2) > mx) break
            currWidth += charWidth
            newCaret = index + 1
        }
        caret = newCaret
        updateCaretPosition()
    }

    private fun updateCaretPosition() {
        if (selection != caret) {
            selectionWidth = textWidth(value.sub(selection, caret), inputHeight - 2)
            if (selection <= caret) selectionWidth *= -1
        } else {
            selectionWidth = 0f
        }

        if (caret != 0) {
            val previousX = caretX
            caretX = textWidth(value.sub(0, caret), inputHeight - 2)

            if (previousX < caretX) if (caretX - textOffset >= inputWidth) textOffset = caretX - inputWidth
            else if (caretX - textOffset <= 0f) textOffset = caretX

            if (textOffset > 0 && textWidth(value, inputHeight - 2) - textOffset < inputWidth) textOffset = (textWidth(value, inputHeight - 2) - inputWidth).coerceAtLeast(0f)
        } else {
            caretX = 0f
            textOffset = 0f
        }
    }

    private fun clearSelection() {
        selection = caret
        selectionWidth = 0f
    }

    private fun selectWord() {
        var start = caret
        var end = caret
        while (start > 0 && !value[start - 1].isWhitespace()) start--
        while (end < value.length && !value[end].isWhitespace()) end++
        selection = start
        caret = end
        updateCaretPosition()
    }

    private fun selectAll() {
        selection = 0
        caret = value.length
        updateCaretPosition()
    }

    private fun getPreviousSpace(): Int {
        var start = caret
        while (start > 0) {
            if (start != caret && value[start - 1].isWhitespace()) break
            start--
        }
        return start
    }

    private fun getNextSpace(): Int {
        var end = caret
        while (end < value.length) {
            if (end != caret && value[end].isWhitespace()) break
            end++
        }
        return end
    }

    private fun resetState() {
        listening = false
        textOffset = 0f
        clearSelection()
    }

    private fun saveState() {
        if (value == lastSavedText) return
        if (historyIndex < history.size - 1) history.subList(historyIndex + 1, history.size).clear()
        history.add(value)
        historyIndex = history.size - 1
        lastSavedText = value
    }

    private fun undo() {
        if (historyIndex <= 0) return
        historyIndex--
        value = history[historyIndex]
        caret = value.length
        selection = caret
        lastSavedText = value
        onUpdate(configKey, value)
    }

    private fun redo() {
        if (historyIndex >= history.size - 1) return
        historyIndex++
        value = history[historyIndex]
        caret = value.length
        selection = caret
        lastSavedText = value
        onUpdate(configKey, value)
    }

    override fun getHeight() = 58f

    private fun String.sub(from: Int, to: Int): String {
        val f = min(from, to).coerceAtLeast(0)
        val t = max(to, from)
        return if (t > length) substring(f) else substring(f, t)
    }

    private fun String.remove(from: Int, to: Int): String = removeRange(min(from, to), max(to, from))

    private fun String.dropAt(at: Int, amount: Int): String = remove(at, at + amount)
}