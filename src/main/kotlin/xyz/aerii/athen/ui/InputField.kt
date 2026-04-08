package xyz.aerii.athen.ui

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.handlers.KeyEater
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class InputField(val placeholder: String) {
    var value = ""
    var cursor = 0
    var selectionStart = -1
    var scrollOffset = 0
    var focused = false

    val selection: Pair<Int, Int>
        get() =
            if (selectionStart == -1) cursor to cursor
            else min(selectionStart, cursor) to max(selectionStart, cursor)

    val selectionBool: Boolean
        get() = selectionStart != -1 && selectionStart != cursor

    fun draw(guiGraphics: GuiGraphics, mx: Int, my: Int, x: Int, y: Int, w: Int, onZone: ((Int, Int, Int, Int) -> Unit)? = null) {
        val fh = 16
        val hovered = mx in x until x + w && my in y until y + fh
        guiGraphics.drawRectangle(x, y, w, fh, if (focused) Mocha.Surface2.argb else if (hovered) Mocha.Surface1.argb else Mocha.Surface0.argb)
        guiGraphics.drawOutline(x, y, w, fh, 1, if (focused) Mocha.Mauve.argb else Mocha.Overlay0.argb)

        guiGraphics.enableScissor(x + 2, y, x + w - 2, y + fh)

        val displayTxt = if (value.isEmpty() && !focused) placeholder else value
        val color = if (value.isEmpty() && !focused) Mocha.Subtext0.argb else Mocha.Text.argb
        val maxW = w - 6

        if (focused) {
            while (client.font.width(value.substring(0, cursor)) - scrollOffset > maxW) scrollOffset += 10
            while (client.font.width(value.substring(0, cursor)) - scrollOffset < 0) scrollOffset = max(0, scrollOffset - 10)
        } else {
            scrollOffset = 0
        }

        val tx = x + 3 - scrollOffset

        if (selectionBool && focused) {
            val (s, e) = selection
            val s1 = client.font.width(value.substring(0, s))
            val s2 = client.font.width(value.substring(0, e))
            guiGraphics.drawRectangle(tx + s1, y + 2, s2 - s1, fh - 4, Mocha.Mauve.withAlpha(0.5f))
        }

        guiGraphics.text(displayTxt, tx, y + (fh - client.font.lineHeight) / 2 + 1, false, color)

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0L) {
            val cx = client.font.width(value.substring(0, cursor))
            guiGraphics.drawRectangle(tx + cx, y + 2, 1, fh - 4, Mocha.Mauve.argb)
        }

        guiGraphics.disableScissor()
        onZone?.invoke(x, y, w, fh)
    }

    fun updateClick(mx: Int, bx: Int) {
        val dx = mx - (bx + 3) + scrollOffset
        var best = 0
        var dist = Int.MAX_VALUE

        for (i in 0..value.length) {
            val w = client.font.width(value.substring(0, i))
            val d = abs(w - dx).takeIf { it < dist } ?: continue

            dist = d
            best = i
        }

        selectionStart = if (KeyEater.shift) selectionStart.takeIf { it != -1 } ?: cursor else -1
        cursor = best
    }

    fun handleKey(keyCode: Int, modifiers: Int): Boolean {
        val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0
        val ctrl = (modifiers and GLFW.GLFW_MOD_CONTROL) != 0

        when (keyCode) {
            GLFW.GLFW_KEY_LEFT -> {
                if (shift && selectionStart == -1) selectionStart = cursor

                if (!shift && selectionBool) {
                    cursor = selection.first
                    selectionStart = -1
                    return true
                }

                cursor = max(0, cursor - 1)
                return true
            }

            GLFW.GLFW_KEY_RIGHT -> {
                if (shift && selectionStart == -1) selectionStart = cursor

                if (!shift && selectionBool) {
                    cursor = selection.second
                    selectionStart = -1
                    return true
                }

                cursor = min(value.length, cursor + 1)
                return true
            }

            GLFW.GLFW_KEY_HOME -> {
                if (shift && selectionStart == -1) selectionStart = cursor
                if (!shift) selectionStart = -1

                cursor = 0
                return true
            }

            GLFW.GLFW_KEY_END -> {
                if (shift && selectionStart == -1) selectionStart = cursor
                if (!shift) selectionStart = -1

                cursor = value.length
                return true
            }

            GLFW.GLFW_KEY_BACKSPACE -> {
                if (selectionBool) {
                    deleteSel()
                    return true
                }

                if (cursor > 0) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor)
                    cursor--
                }

                return true
            }

            GLFW.GLFW_KEY_DELETE -> {
                if (selectionBool) {
                    deleteSel()
                    return true
                }

                if (cursor < value.length) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1)
                }

                return true
            }

            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                return true
            }

            GLFW.GLFW_KEY_A -> {
                if (!ctrl) return false
                selectionStart = 0
                cursor = value.length
                return true
            }

            GLFW.GLFW_KEY_C -> {
                if (!ctrl) return false
                if (!selectionBool) return false

                val (s, e) = selection
                McClient.clipboard = value.substring(s, e)
                return true
            }

            GLFW.GLFW_KEY_X -> {
                if (!ctrl) return false
                if (!selectionBool) return false

                val (s, e) = selection
                McClient.clipboard = value.substring(s, e)
                deleteSel()
                return true
            }

            GLFW.GLFW_KEY_V -> {
                if (!ctrl) return false

                deleteSel()
                val clip = McClient.clipboard
                value = value.substring(0, cursor) + clip + value.substring(cursor)
                cursor += clip.length
                return true
            }
        }
        return false
    }

    fun handleChar(char: Char): Boolean {
        if (char.code < 32 || char.code == 127) return false

        deleteSel()
        value = value.substring(0, cursor) + char + value.substring(cursor)
        cursor++

        return true
    }

    fun reset() {
        value = ""
        cursor = 0
        selectionStart = -1
        scrollOffset = 0
        focused = false
    }

    private fun deleteSel(): Boolean {
        if (!selectionBool) return false
        val (s, e) = selection
        value = value.substring(0, s) + value.substring(e)
        cursor = s
        selectionStart = -1
        return true
    }
}