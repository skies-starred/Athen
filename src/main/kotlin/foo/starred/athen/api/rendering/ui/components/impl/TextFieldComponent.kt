package foo.starred.athen.api.rendering.ui.components.impl

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.ZERO_PAIR
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class TextFieldComponent : IPrimitiveElement<TextFieldComponent>() {
    override var x: Float = 0f
    override var y: Float = 0f
    override var width: Float = 0f
    override var height: Float = 0f
    override var color: CascadeGeometricColor = CascadeGeometricColor.WHITE

    var placeholder: String = ""
    var value: String = ""
    var scroll: Int = 0

    var block: Int = 0
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
            root.focused = self
            cancel()

            val x = x.toInt() - (self.x + 3) + scroll
            var i0 = 0
            var i1 = Int.MAX_VALUE

            for (i in 0..value.length) {
                val a = MinecraftFontMeasurer.width(value.substring(0, i))
                val b = abs(a - x).takeIf { it < i1 }?.toInt() ?: continue

                i1 = b
                i0 = i
            }

            anchor = if (KeyboardInputState.States.shift()) anchor.takeIf { it != -1 } ?: block else -1
            block = i0
        }

        on<KeyEvent.Press> {
            val shift = KeyboardInputState.States.shift()
            val ctrl = KeyboardInputState.States.control()

            when (key) {
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
                    if (shift && anchor == -1) anchor = block
                    if (!shift) anchor = -1

                    block = 0
                    cancel()
                    return@on
                }

                InputConstants.KEY_END -> {
                    if (shift && anchor == -1) anchor = block
                    if (!shift) anchor = -1

                    block = value.length
                    cancel()
                    return@on
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
                    return@on
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
                    return@on
                }

                InputConstants.KEY_ESCAPE -> {
                    root.focused = null
                    cancel()
                    return@on
                }

                InputConstants.KEY_A -> {
                    if (!ctrl) return@on

                    anchor = 0
                    block = value.length
                    cancel()
                    return@on
                }

                InputConstants.KEY_C -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    val (s, e) = range
                    MinecraftClient.clipboard = value.substring(s, e)
                    cancel()
                    return@on
                }

                InputConstants.KEY_X -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    val (s, e) = range
                    MinecraftClient.clipboard = value.substring(s, e)
                    delete()
                    cancel()
                    return@on
                }

                InputConstants.KEY_V -> {
                    if (!ctrl) return@on

                    delete()
                    val clip = MinecraftClient.clipboard
                    value = value.substring(0, block) + clip + value.substring(block)
                    block += clip.length
                    cancel()
                    return@on
                }
            }
        }

        on<KeyEvent.Type> {
            if (char.code < 32) return@on
            if (char.code == 127) return@on

            delete()
            value = value.substring(0, block) + char + value.substring(block)
            block++

            cancel()
        }
    }

    override fun draw(graphics: GuiGraphicsExtractor) {
        val b = root.focused == this

        graphics.rectangle(x, y, width, height, if (b) MochaColorScheme.Surface2.argb else if (hovered) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)
        graphics.hollowRectangle(x, y, width, height, 1f, if (b) MochaColorScheme.Lavender.argb else MochaColorScheme.Overlay0.argb, inset = false)

        graphics.scissor(x + 2, y, width - 2, height) {
            run {
                if (!b) return@run ::scroll.set(0)
                val i0 = width - 6
                while (MinecraftFontMeasurer.width(value.substring(0, block)) - scroll > i0) scroll += 10
                while (MinecraftFontMeasurer.width(value.substring(0, block)) - scroll < 0) scroll = max(0, scroll - 10)
            }

            val x0 = x + 3 - scroll

            if (selected && b) {
                val start = range.first.coerceIn(0, value.length)
                val end = range.second.coerceIn(0, value.length)

                val s1 = MinecraftFontMeasurer.width(value.substring(0, start))
                val s2 = MinecraftFontMeasurer.width(value.substring(0, end))
                graphics.rectangle(x0 + s1, y + 2, (s2 - s1).toFloat(), height - 4, MochaColorScheme.Lavender.alpha(0.5f))
            }

            val c = value.isEmpty() && !b
            val str = if (c) placeholder else value
            val color = if (c) MochaColorScheme.Subtext0.argb else MochaColorScheme.Text.argb
            MinecraftFontRenderer.extract(graphics, str, x0, y + (height - MinecraftFontMeasurer.height) / 2 + 1, false, color)

            if (b && (System.currentTimeMillis() / 500) % 2 == 0L) {
                val x1 = MinecraftFontMeasurer.width(value.substring(0, block))
                graphics.rectangle(x0 + x1, y + 2, 1f, height - 4, MochaColorScheme.Lavender.argb)
            }
        }
    }

    fun reset(v: Boolean = false) {
        if (v) value = ""
        block = 0
        anchor = -1
        scroll = 0
        root.focused = null
    }

    private fun fn() {
        selected = anchor != -1 && anchor != block
        range = if (anchor == -1) block to block else min(anchor, block) to max(anchor, block)
    }

    private fun delete(): Boolean {
        if (!selected) return false
        val (s, e) = range
        value = value.substring(0, s) + value.substring(e)
        block = s
        anchor = -1
        return true
    }

    companion object {
        inline fun textField(block: TextFieldComponent.() -> Unit): TextFieldComponent {
            return TextFieldComponent().apply(block)
        }
    }
}
