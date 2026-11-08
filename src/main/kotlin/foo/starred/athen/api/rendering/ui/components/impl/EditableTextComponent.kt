@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.api.rendering.ui.components.impl

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.cascade.events.impl.FocusEvent
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.base.interfaces.IPrimitiveTypable
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.api.ZERO_PAIR
import foo.starred.kontext.api.format.extension.color.alpha
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class EditableTextComponent : IPrimitiveElement<EditableTextComponent>(), IPrimitiveTypable {
    private var initial: String = ""
    private var _cursor: Float = 0f
    private var _selection0: Float = 0f
    private var _selection1: Float = 0f

    private var commit: ((String) -> Unit)? = null

    override var x: Float = 0f
    override var y: Float = 0f
    override var width: Float = 0f
    override var height: Float = 0f
    override var color: CascadeGeometricColor = CascadeGeometricColor.WHITE

    var textSize: Float = 8.5f
    var color0: Int = MochaColorScheme.Text.argb
    var color1: Int = MochaColorScheme.Lavender.argb
    var placeholder: String = "..."

    var editing: Boolean = false
        private set

    var value: String = ""
        set(v) {
            if (field == v) return
            field = v
            fn0()
        }

    var block: Int = 0
        set(v) {
            if (field == v) return
            field = v
            fn()
        }

    var anchor: Int = -1
        set(v) {
            if (field == v) return
            field = v
            fn()
        }

    var range: Pair<Int, Int> = ZERO_PAIR
        private set

    var selected: Boolean = false
        private set

    init {
        on<FocusEvent.Lose> {
            if (!editing) return@on
            commit()
        }

        on<MouseEvent.Press> {
            if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
            cancel()

            val bool = editing
            if (!editing) {
                initial = value
                editing = true
                root.focused = self
            }

            if (!bool) {
                block = value.length
                anchor = -1
                return@on
            }

            val font = CascadeFonts.sans
            val width1 = font.width(value, textSize)
            val x1 = this@EditableTextComponent.x + (width - width1) / 2f
            val x2 = x.toFloat() - x1

            var i0 = 0
            var i1 = Float.MAX_VALUE

            for (i in 0..value.length) {
                val a = font.width(value.substring(0, i), textSize)
                val b = abs(a - x2)
                if (b >= i1) continue

                i1 = b
                i0 = i
            }

            anchor = if (KeyboardInputState.States.shift()) anchor.takeIf { it != -1 } ?: block else -1
            block = i0
        }

        on<KeyEvent.Press> {
            if (!editing) return@on

            val shift = KeyboardInputState.States.shift()
            val ctrl = KeyboardInputState.States.control()

            when (key) {
                InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                    commit()
                    cancel()
                    return@on
                }

                InputConstants.KEY_ESCAPE -> {
                    editing = false
                    root.focused = null
                    anchor = -1
                    value = initial

                    cancel()
                    return@on
                }

                InputConstants.KEY_LEFT -> {
                    if (shift && anchor == -1) {
                        anchor = block
                    }

                    if (!shift && selected) {
                        block = range.first
                        anchor = -1
                        cancel()
                        return@on
                    }

                    block = max(0, block - 1)
                    cancel()
                    return@on
                }

                InputConstants.KEY_RIGHT -> {
                    if (shift && anchor == -1) {
                        anchor = block
                    }

                    if (!shift && selected) {
                        block = range.second
                        anchor = -1
                        cancel()
                        return@on
                    }

                    block = min(value.length, block + 1)
                    cancel()
                    return@on
                }

                InputConstants.KEY_HOME -> {
                    if (shift && anchor == -1) {
                        anchor = block
                    }

                    if (!shift) {
                        anchor = -1
                    }

                    block = 0
                    cancel()
                    return@on
                }

                InputConstants.KEY_END -> {
                    if (shift && anchor == -1) {
                        anchor = block
                    }

                    if (!shift) {
                        anchor = -1
                    }

                    block = value.length
                    cancel()
                }

                InputConstants.KEY_BACKSPACE -> {
                    if (selected) {
                        delete()
                        cancel()
                        return@on
                    }

                    if (block > 0) {
                        value = value.substring(0, block - 1) + value.substring(block)
                        block--
                    }

                    cancel()
                }

                InputConstants.KEY_DELETE -> {
                    if (selected) {
                        delete()
                        cancel()
                        return@on
                    }

                    if (block < value.length) {
                        value = value.substring(0, block) + value.substring(block + 1)
                    }

                    cancel()
                }

                InputConstants.KEY_A -> {
                    if (!ctrl) return@on

                    anchor = 0
                    block = value.length
                    cancel()
                }

                InputConstants.KEY_C -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    MinecraftClient.clipboard = value.substring(range.first, range.second)
                    cancel()
                }

                InputConstants.KEY_X -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    MinecraftClient.clipboard = value.substring(range.first, range.second)
                    delete()
                    cancel()
                }

                InputConstants.KEY_V -> {
                    if (!ctrl) return@on

                    delete()
                    val clip = MinecraftClient.clipboard
                    value = value.substring(0, block) + clip + value.substring(block)
                    block += clip.length

                    cancel()
                }
            }
        }

        on<KeyEvent.Type> {
            if (!editing) return@on
            if (char.code < 32) return@on
            if (char.code == 127) return@on

            delete()
            value = value.substring(0, block) + char + value.substring(block)
            block++
            cancel()
        }
    }

    override fun draw(graphics: GuiGraphicsExtractor) {
        val font = CascadeFonts.sans
        graphics.scissor(x, y, width, height) {
            val text = if (value.isEmpty() && !editing) placeholder else value
            val width1 = font.width(text, textSize)
            val height1 = font.regular.height * textSize
            val x1 = x + (width - width1) / 2f
            val y1 = y + (height - textSize) / 2f

            if (editing && selected) {
                val i0 = min(_selection0, _selection1)
                val i1 = max(_selection0, _selection1)
                graphics.rectangle(x1 + i0, y1, i1 - i0, height1, MochaColorScheme.Lavender.argb.alpha(0.35f))
            }

            val color = if (editing) color1 else if (hovered) color1 else color0
            font.extract(graphics, text, x1, y1, color, false, textSize)

            if (editing && (System.currentTimeMillis() / 500) % 2 == 0L) {
                font.extract(graphics, "|", x1 + _cursor - 1f, y1 - 1f, MochaColorScheme.Lavender.argb, false, textSize)
            }
        }
    }

    fun commit(block: ((String) -> Unit)?) {
        commit = block
    }

    private fun fn() {
        selected = anchor != -1 && anchor != block
        range = if (anchor == -1) block to block else min(anchor, block) to max(anchor, block)
        fn0()
    }

    private fun fn0() {
        val font = CascadeFonts.sans
        _cursor = font.width(value.substring(0, min(block, value.length)), textSize)

        if (!selected) return
        _selection0 = font.width(value.substring(0, min(range.first, value.length)), textSize)
        _selection1 = font.width(value.substring(0, min(range.second, value.length)), textSize)
    }

    private fun commit() {
        editing = false
        root.focused = null
        anchor = -1
        commit?.invoke(value)
    }

    private fun delete(): Boolean {
        if (!selected) return false
        value = value.substring(0, range.first) + value.substring(range.second)
        block = range.first
        anchor = -1
        return true
    }

    companion object {
        inline fun editableText(block: EditableTextComponent.() -> Unit): EditableTextComponent {
            return EditableTextComponent().apply(block)
        }
    }
}
