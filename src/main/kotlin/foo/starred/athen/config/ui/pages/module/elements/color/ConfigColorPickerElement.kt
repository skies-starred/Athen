package foo.starred.athen.config.ui.pages.module.elements.color

import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.impl.ConfigColorPickerElementData
import foo.starred.athen.config.ui.ConfigUI
import foo.starred.athen.api.rendering.ui.components.impl.EditableTextComponent.Companion.editableText
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.cascade.animation.data.AnimatableColor.Companion.animateColor
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.FocusEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.circle.circle
import foo.starred.cascade.graphics.extensions.rectangle.gradient.gradientRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.snowbird.utils.alpha
import foo.starred.snowbird.utils.blue
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.green
import foo.starred.snowbird.utils.red
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f
import java.awt.Color

class ConfigColorPickerElement(
    private val config: ConfigColorPickerElementData
) : RoundedRectanglePrimitive() {
    private var value: Int = (ConfigManager.get(config.key) as? Number)?.toInt() ?: config.default
    private var expanded = false

    private var drag: Int = 0
    private var hue: Float = 0f
    private var saturation: Float = 1f
    private var brightness: Float = 1f
    private var alpha: Float = 1f

    private var outline: OutlineEffect

    private val swatch = roundedRectangle {
        position = FixedPositionConstraint(0f, 0f)
        size = FixedSizeConstraint(28f, 14f)
        radius = CascadeGeometricRadius(4f, 0f, 4f, 0f)
        color = value
        interact = false
    }

    private val hex = editableText {
        position = FixedPositionConstraint(29f, 0f)
        size = FixedSizeConstraint(55f, 14f)
        textSize = 8f
        color0 = Catppuccin.Mocha.Text.argb
        color1 = Catppuccin.Mocha.Lavender.argb
        value = this@ConfigColorPickerElement.value.hex()

        on<MouseEvent.Press> {
            if (button != 0) return@on
            if (!expanded) fn()
        }

        commit {
            parse(it)?.let { c ->
                this@ConfigColorPickerElement.value = c
                color(c)
                swatch.color = c
                ConfigManager.update(config.key, c)
            }

            value = this@ConfigColorPickerElement.value.hex()
        }
    }

    private val box = object : RoundedRectanglePrimitive() {
        override fun draw(graphics: GuiGraphicsExtractor) {
            graphics.nextStratum()
            super.draw(graphics)
            pop(graphics)
        }
    }.apply {
        size = FixedSizeConstraint(140f, 128f)
        radius = CascadeGeometricRadius(6f)
        color = Catppuccin.Mocha.Base.argb
        visible = false
        unfocus = false

        effect(OutlineEffect {
            color = Catppuccin.Mocha.Surface2.argb
            inset = false
        })

        on<MouseEvent.Press> {
            if (button != 0) return@on
            cancel()
            root.focused = this@apply

            val x1 = (x - this@apply.x).toFloat()
            val y1 = (y - this@apply.y).toFloat()

            when (x1) {
                in 8f..132f if y1 in 8f..72f -> {
                    drag = 1
                    fn(x1, y1)
                }

                in 8f..132f if y1 in 76f..84f -> {
                    drag = 2
                    hue(x1)
                }

                in 8f..132f if y1 in 88f..96f -> {
                    drag = 3
                    alpha(x1)
                }
            }
        }

        on<MouseEvent.Release> {
            if (button != 0) return@on

            if (root.focused == this@apply) {
                root.focused = null
            }

            drag = 0
            cancel()
        }

        on<MouseEvent.Move.Any> {
            if (drag == 0) return@on
            cancel()

            val x1 = (x - this@apply.x).toFloat()
            val y1 = (y - this@apply.y).toFloat()

            when (drag) {
                1 -> fn(x1, y1)
                2 -> hue(x1)
                3 -> alpha(x1)
            }
        }

        on<FocusEvent.Lose> {
            drag = 0
        }

        adopt(container {
            position = FixedPositionConstraint(8f, 104f)
            size = FixedSizeConstraint(124f, 16f)
            interact = false

            adopt(roundedRectangle {
                position = FixedPositionConstraint(0f, 0f)
                size = FixedSizeConstraint(124f, 16f)
                radius = CascadeGeometricRadius(4f)
                color = 0
                interact = false

                effect(OutlineEffect {
                    color = Catppuccin.Mocha.Surface2.argb
                    inset = false
                })
            })

            for (k in 0..3) {
                val color0 = PRESETS[k]

                adopt(roundedRectangle {
                    position = FixedPositionConstraint(k * 31f, 0f)
                    size = FixedSizeConstraint(31f, 16f)
                    radius = if (k == 0) CascadeGeometricRadius(4f, 0f, 4f, 0f) else if (k == 3) CascadeGeometricRadius(0f, 4f, 0f, 4f) else CascadeGeometricRadius.ZERO
                    color = color0

                    on<MouseEvent.Press> {
                        if (button != 0) return@on

                        cancel()
                        value = color0
                        color(color0)
                        swatch.color = value
                        hex.value = value.hex()
                        ConfigManager.update(config.key, value)
                    }

                    on<MouseEvent.Move.Enter> {
                        animateColor(color0.brighten(1.2f), 0.15f)
                    }

                    on<MouseEvent.Move.Exit> {
                        animateColor(color0, 0.15f)
                    }
                })

                if (k >= 3) continue
                adopt(rectangle {
                    position = FixedPositionConstraint((k + 1) * 31f, 0f)
                    size = FixedSizeConstraint(1f, 16f)
                    color = Catppuccin.Mocha.Surface2.argb
                    interact = false
                })
            }
        })
    }

    init {
        position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -8f, 0f)
        size = FixedSizeConstraint(84f, 14f)
        radius = CascadeGeometricRadius(4f)
        color = Catppuccin.Mocha.Surface0.argb

        effect(OutlineEffect {
            color = Catppuccin.Mocha.Surface1.argb
            inset = false
        }.also { outline = it })

        on<MouseEvent.Press> {
            if (button != 0) return@on
            cancel()
            fn()
        }

        on<MouseEvent.Move.Enter> {
            if (expanded) return@on
            animateColor(Catppuccin.Mocha.Surface1.argb, 0.15f)
        }

        on<MouseEvent.Move.Exit> {
            if (expanded) return@on
            animateColor(Catppuccin.Mocha.Surface0.argb, 0.15f)
        }

        color(value)
        adopt(swatch)

        adopt(rectangle {
            position = FixedPositionConstraint(28f, 0f)
            size = FixedSizeConstraint(1f, 14f)
            color = Catppuccin.Mocha.Surface1.argb
            interact = false
        })

        adopt(hex)
    }

    fun close() {
        if (!expanded) return
        expanded = false
        box.visible = false
        box.detach()
        animateColor(if (hovered) Catppuccin.Mocha.Surface1.argb else Catppuccin.Mocha.Surface0.argb, 0.15f)
        outline.color = Catppuccin.Mocha.Surface1.argb
        if (active === this) active = null
    }

    override fun detach(): RoundedRectanglePrimitive {
        close()
        return super.detach()
    }

    private fun fn() {
        if (!expanded) {
            if (active !== this) active?.close()
            active = this
            expanded = true
            box.visible = true
            box.attach(ConfigUI.scene)
            animateColor(Catppuccin.Mocha.Surface1.argb, 0.15f)
            outline.color = Catppuccin.Mocha.Lavender.argb

            val y0 = y - ConfigUI.right.scroll
            box.position = FixedPositionConstraint(x + width - 140f, if (y0 + 146f > ConfigUI.scene.height) y0 - 132f else y0 + 18f)
            return
        }

        close()
    }

    private fun fn(x: Float, y: Float) {
        saturation = ((x - 8f) / 124f).coerceIn(0f, 1f)
        brightness = (1f - (y - 8f) / 64f).coerceIn(0f, 1f)
        commit()
    }

    private fun hue(x: Float) {
        hue = ((x - 8f) / 124f).coerceIn(0f, 1f)
        commit()
    }

    private fun alpha(x: Float) {
        alpha = ((x - 8f) / 124f).coerceIn(0f, 1f)
        commit()
    }

    private fun color(argb: Int) {
        val hsb = Color.RGBtoHSB(argb.red, argb.green, argb.blue, null)
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]
        alpha = argb.alpha / 255f
    }

    private fun commit() {
        val rgb = Color.HSBtoRGB(hue, saturation, brightness) and 0x00FFFFFF
        val a = (alpha * 255f).toInt().coerceIn(0, 255)
        value = (a shl 24) or rgb

        swatch.color = value
        hex.value = value.hex()
        ConfigManager.update(config.key, value)
    }

    private fun pop(graphics: GuiGraphicsExtractor) {
        val x1 = box.x
        val y1 = box.y
        val pose = Matrix3x2f(graphics.pose())
        val scissor = graphics.scissorStack.peek()

        val hue1 = Color.HSBtoRGB(hue, 1f, 1f) or (0xFF shl 24)
        graphics.gradientRectangle(x1 + 8f, y1 + 8f, 124f, 64f, -1, hue1, -1, hue1, pose, scissor)
        graphics.gradientRectangle(x1 + 8f, y1 + 8f, 124f, 64f, 0, 0, Int.MIN_VALUE, Int.MIN_VALUE, pose, scissor)

        val x2 = x1 + 8f + saturation * 124f
        val y2 = y1 + 8f + (1f - brightness) * 64f
        graphics.circle(x2, y2, 3.5f, Int.MIN_VALUE, pose, scissor)
        graphics.circle(x2, y2, 2.5f, -1, pose, scissor)

        val x3 = x1 + 8f
        val y3 = y1 + 76f
        for (s in 0..5) {
            val c0 = Color.HSBtoRGB(s / 6f, 1f, 1f) or (0xFF shl 24)
            val c1 = Color.HSBtoRGB((s + 1) / 6f, 1f, 1f) or (0xFF shl 24)
            graphics.gradientRectangle(x3 + s * 20.66f, y3, 20.66f, 8f, c0, c1, c0, c1, pose, scissor)
        }

        val x4 = x3 + hue * 124f
        graphics.rectangle(x4 - 1.5f, y3 - 1f, 3f, 10f, -1, pose, scissor)
        graphics.rectangle(x4 - 0.5f, y3, 1f, 8f, Int.MIN_VALUE, pose, scissor)

        val x5 = x1 + 8f
        val y5 = y1 + 88f
        val rgb = value and 0x00FFFFFF
        val rgba = rgb or (0xFF shl 24)

        graphics.rectangle(x5, y5, 124f, 8f, Catppuccin.Mocha.Surface0.argb, pose, scissor)
        graphics.gradientRectangle(x5, y5, 124f, 8f, rgb, rgba, rgb, rgba, pose, scissor)

        val x6 = x5 + alpha * 124f
        graphics.rectangle(x6 - 1.5f, y5 - 1f, 3f, 10f, -1, pose, scissor)
        graphics.rectangle(x6 - 0.5f, y5, 1f, 8f, Int.MIN_VALUE, pose, scissor)
    }

    companion object {
        private val PRESETS = listOf(
            Catppuccin.Mocha.Red.argb,
            Catppuccin.Mocha.Green.argb,
            Catppuccin.Mocha.Lavender.argb,
            Catppuccin.Mocha.Peach.argb
        )

        var active: ConfigColorPickerElement? = null
            private set

        fun of(parent: IPrimitiveElement<*>, config: ConfigColorPickerElementData): ConfigColorPickerElement {
            return ConfigColorPickerElement(config).apply {
                attach(parent)
            }
        }

        private fun Int.hex(): String {
            return if (alpha == 255) String.format("#%02X%02X%02X", red, green, blue) else String.format("#%02X%02X%02X%02X", red, green, blue, alpha)
        }

        private fun parse(hex: String): Int? {
            val s = hex.trim().removePrefix("#")
            return try {
                when (s.length) {
                    3 -> {
                        val r = s.substring(0, 1).toInt(16) * 17
                        val g = s.substring(1, 2).toInt(16) * 17
                        val b = s.substring(2, 3).toInt(16) * 17
                        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    6 -> {
                        val rgb = s.toLong(16).toInt()
                        (0xFF shl 24) or (rgb and 0xFFFFFF)
                    }
                    8 -> {
                        val rgba = s.toLong(16).toInt()
                        val r = (rgba ushr 24) and 0xFF
                        val g = (rgba ushr 16) and 0xFF
                        val b = (rgba ushr 8) and 0xFF
                        val a = rgba and 0xFF
                        (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
