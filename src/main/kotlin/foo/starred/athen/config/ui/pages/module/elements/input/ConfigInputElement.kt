package foo.starred.athen.config.ui.pages.module.elements.input

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.impl.ConfigTextInputElementData
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputExpandElement.Companion.configExpandButtonElement
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputPreviewElement.Companion.configPreviewButtonElement
import foo.starred.cascade.animation.extension.impl.animate
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.FocusEvent
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.base.interfaces.IPrimitiveTypable
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.api.ZERO_PAIR
import foo.starred.kontext.api.format.extension.color.alpha
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

open class ConfigInputElement : RoundedRectanglePrimitive(), IPrimitiveTypable {
    private var update: ((String) -> Unit)? = null
    private var _cursor: Float = 0f
    private var _selection0: Float = 0f
    private var _selection1: Float = 0f

    var placeholder: String = ""
    var scroll: Int = 0

    var value: String = ""
        set(v) {
            if (field == v) return
            field = v
            fn0()
            update?.invoke(v)
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
        color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
        radius = CascadeGeometricRadius(4f)

        effect(OutlineEffect {
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            inset = false
        })

        on<FocusEvent.Gain> {
            animate(150.milliseconds) {
                ::color to CascadeGeometricColor(MochaColorScheme.Surface1.argb)

                effect<OutlineEffect> {
                    ::color to CascadeGeometricColor(MochaColorScheme.Lavender.argb)
                }
            }
        }

        on<FocusEvent.Lose> {
            animate(150.milliseconds) {
                ::color to CascadeGeometricColor(if (hovered) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)

                effect<OutlineEffect> {
                    ::color to CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                }
            }
        }

        on<MouseEvent.Move.Enter> {
            if (root.focused == self) return@on

            animate(150.milliseconds) {
                ::color to CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            }
        }

        on<MouseEvent.Move.Exit> {
            if (root.focused == self) return@on

            animate(150.milliseconds) {
                ::color to CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            }
        }

        on<MouseEvent.Press> {
            val font = CascadeFonts.sans

            root.focused = self
            cancel()

            val mx = x.toInt() - (self.x + 3) + scroll
            var i0 = 0
            var i1 = Int.MAX_VALUE

            for (i in 0..value.length) {
                val a = font.width(value.substring(0, i), 12f)
                val b = abs(a - mx).takeIf { it < i1 }?.toInt() ?: continue

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

                    MinecraftClient.clipboard = value.substring(range.first, range.second)
                    cancel()
                    return@on
                }

                InputConstants.KEY_X -> {
                    if (!ctrl) return@on
                    if (!selected) return@on

                    MinecraftClient.clipboard = value.substring(range.first, range.second)
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
        val bool0 = root.focused != this
        if (bool0) scroll = 0

        super.draw(graphics)

        val font = CascadeFonts.sans
        val x0 = x.toInt()
        val y0 = y.toInt()

        graphics.scissor(x0 + 2, y0, width - 2, height) {
            val height0 = font.regular.height * 12f
            val x1 = x0 + 3 - scroll
            val y1 = y + (height - font.regular.height * 12f) / 2f

            if (selected && !bool0) {
                val i0 = min(_selection0, _selection1)
                val i1 = max(_selection0, _selection1)
                val width = i1 - i0

                graphics.rectangle(x1 + i0, y1, width, height0, MochaColorScheme.Base.argb.alpha(0.5f))
            }

            val bool1 = value.isEmpty() && bool0
            font.extract(graphics, if (bool1) placeholder else value, x1, y1, if (bool1) MochaColorScheme.Overlay0.argb else MochaColorScheme.Text.argb, false)

            if (!bool0 && (System.currentTimeMillis() / 500) % 2 == 0L) {
                font.extract(graphics, "|", x1 + _cursor - 1f, y1 - 1f, MochaColorScheme.Lavender.argb, false)
            }
        }
    }

    fun update(block: ((String) -> Unit)?) {
        update = block
    }

    private fun fn() {
        selected = anchor != -1 && anchor != block
        range = if (anchor == -1) block to block else min(anchor, block) to max(anchor, block)
        fn0()
    }

    private fun fn0() {
        val font = CascadeFonts.sans
        _cursor = font.width(value.substring(0, min(block, value.length)), 12f)

        val width = width.toInt()
        if (width > 0) {
            val i0 = max(10, width - 6)
            while (_cursor - scroll > i0) scroll += 10
            while (_cursor - scroll < 0 && scroll > 0) scroll = max(0, scroll - 10)

            val totalWidth = font.width(value, 12f)
            val maxScroll = max(0, (totalWidth - i0).toInt())
            if (scroll > maxScroll) scroll = maxScroll
        }

        if (!selected) return
        _selection0 = font.width(value.substring(0, min(range.first, value.length)), 12f)
        _selection1 = font.width(value.substring(0, min(range.second, value.length)), 12f)
    }

    private fun delete(): Boolean {
        if (!selected) return false

        value = value.substring(0, range.first) + value.substring(range.second)
        block = range.first
        anchor = -1
        return true
    }

    companion object {
        fun configInputElement(block: ConfigInputElement.() -> Unit): ConfigInputElement {
            return ConfigInputElement().apply(block)
        }

        fun of(parent: IPrimitiveElement<*>, config: ConfigTextInputElementData): ConfigInputElement {
            return ConfigInputElement().apply {
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(-44f, 0f)
                size = FixedSizeConstraint(84f, 14f)
                placeholder = config.placeholder
                value = ConfigManager.get(config.key) as? String ?: config.default

                on<KeyEvent.Type> {
                    val value0 = value
                    if (value0.length > config.max) {
                        value = value0.substring(0, config.max)
                        block = min(block, config.max)
                    }

                    ConfigManager.update(config.key, value)
                }

                on<KeyEvent.Press> {
                    ConfigManager.update(config.key, value)
                }

                attach(parent)
                parent.adopt(configExpandButtonElement(this, config) {
                    position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(-26f, 0f)
                })

                parent.adopt(configPreviewButtonElement({ value }) {
                    position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(-8f, 0f)
                })
            }
        }
    }
}
