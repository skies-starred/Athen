package xyz.aerii.athen.api.rendering.ui.dsl.elements.components.impl

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.KeyEvent
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.MouseEvent
import xyz.aerii.athen.api.rendering.ui.effects.outline.outline
import xyz.aerii.athen.api.rendering.ui.shapes.rectangle.rectangle
import xyz.aerii.athen.api.rendering.ui.text.vanilla.extensions.extractText
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.library.api.ZERO_PAIR
import xyz.aerii.library.api.client
import xyz.aerii.library.api.ctrl
import xyz.aerii.library.api.shift
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class TextFieldComponent : IPrimitiveElement<TextFieldComponent>() {
    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0
    override var color: Int = -1

    var placeholder: String = ""
    var value: String = ""
    var scroll: Int = 0

    var cursor: Int = 0
        set(value) {
            if (field == value) return
            field = value
            fn()
        }

    var anchor: Int = -1
        set(value) {
            if (field == value) return
            field = value
            fn()
        }

    var range: Pair<Int, Int> = ZERO_PAIR
        private set

    var selected: Boolean = false
        private set

    init {
        on<MouseEvent.Press> {
            val font = client.font ?: return@on

            root.focused = self
            cancel()

            val x = x.toInt() - (self.x + 3) + scroll
            var i0 = 0
            var i1 = Int.MAX_VALUE

            for (i in 0..value.length) {
                val a = font.width(value.substring(0, i))
                val b = abs(a - x).takeIf { it < i1 } ?: continue

                i1 = b
                i0 = i
            }

            anchor = if (shift) anchor.takeIf { it != -1 } ?: cursor else -1
            cursor = i0
        }

        on<KeyEvent.Press> {
            val shift = shift
            val ctrl = ctrl

            when (key) {
                GLFW.GLFW_KEY_LEFT -> {
                    if (shift && anchor == -1) {
                        anchor = cursor
                    }

                    if (!shift && selected) {
                        cursor = range.first
                        anchor = -1
                        cancel()
                        return@on
                    }

                    cursor = max(0, cursor - 1)
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_RIGHT -> {
                    if (shift && anchor == -1) {
                        anchor = cursor
                    }

                    if (!shift && selected) {
                        cursor = range.second
                        anchor = -1
                        cancel()
                        return@on
                    }

                    cursor = min(value.length, cursor + 1)
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_HOME -> {
                    if (shift && anchor == -1) anchor = cursor
                    if (!shift) anchor = -1

                    cursor = 0
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_END -> {
                    if (shift && anchor == -1) anchor = cursor
                    if (!shift) anchor = -1

                    cursor = value.length
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (selected) {
                        delete()
                        cancel()
                        return@on
                    }

                    if (cursor > 0) {
                        value = value.substring(0, cursor - 1) + value.substring(cursor)
                        cursor--
                    }

                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_DELETE -> {
                    if (selected) {
                        delete()
                        cancel()
                        return@on
                    }

                    if (cursor < value.length) {
                        value = value.substring(0, cursor) + value.substring(cursor + 1)
                    }

                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_ESCAPE -> {
                    root.focused = null
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_A -> {
                    if (!ctrl) return@on

                    anchor = 0
                    cursor = value.length
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_C -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    val (s, e) = range
                    client.keyboardHandler.clipboard = value.substring(s, e)
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_X -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    val (s, e) = range
                    client.keyboardHandler.clipboard = value.substring(s, e)
                    delete()
                    cancel()
                    return@on
                }

                GLFW.GLFW_KEY_V -> {
                    if (!ctrl) return@on

                    delete()
                    val clip = client.keyboardHandler.clipboard
                    value = value.substring(0, cursor) + clip + value.substring(cursor)
                    cursor += clip.length
                    cancel()
                    return@on
                }
            }
        }

        on<KeyEvent.Type> {
            if (char.code < 32) return@on
            if (char.code == 127) return@on

            delete()
            value = value.substring(0, cursor) + char + value.substring(cursor)
            cursor++

            cancel()
        }
    }

    override fun render(graphics: GuiGraphics) {
        if (!visible) return
        val f = client.font ?: return
        val b = root.focused == this

        graphics.rectangle(x, y, width, height, if (b) Catppuccin.Mocha.Surface2.argb else if (hovered) Catppuccin.Mocha.Surface1.argb else Catppuccin.Mocha.Surface0.argb)
        graphics.outline(x, y, width, height, 1, if (b) Catppuccin.Mocha.Mauve.argb else Catppuccin.Mocha.Overlay0.argb)

        graphics.enableScissor(x + 2, y, x + width - 2, y + height)

        run {
            if (!b) return@run ::scroll.set(0)
            val i0 = width - 6
            while (f.width(value.substring(0, cursor)) - scroll > i0) scroll += 10
            while (f.width(value.substring(0, cursor)) - scroll < 0) scroll = max(0, scroll - 10)
        }

        val x = x + 3 - scroll

        if (selected && b) {
            val (s, e) = range
            val s1 = f.width(value.substring(0, s))
            val s2 = f.width(value.substring(0, e))
            graphics.rectangle(x + s1, y + 2, s2 - s1, height - 4, Catppuccin.Mocha.Mauve.withAlpha(0.5f))
        }

        val c = value.isEmpty() && !b
        val str = if (c) placeholder else value
        val color = if (c) Catppuccin.Mocha.Subtext0.argb else Catppuccin.Mocha.Text.argb
        graphics.extractText(str, x, y + (height - f.lineHeight) / 2 + 1, false, color)

        graphics.disableScissor()
        super.render(graphics)
    }

    fun reset(v: Boolean = false) {
        if (v) value = ""
        cursor = 0
        anchor = -1
        scroll = 0
        root.focused = null
    }

    private fun fn() {
        selected = anchor != -1 && anchor != cursor
        range = if (anchor == -1) cursor to cursor else min(anchor, cursor) to max(anchor, cursor)
    }

    private fun delete(): Boolean {
        if (!selected) return false
        val (s, e) = range
        value = value.substring(0, s) + value.substring(e)
        cursor = s
        anchor = -1
        return true
    }

    companion object {
        inline fun textField(block: TextFieldComponent.() -> Unit): TextFieldComponent {
            return TextFieldComponent().apply(block)
        }
    }
}