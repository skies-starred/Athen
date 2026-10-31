package foo.starred.athen.config.ui.pages.module.elements.slider

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.impl.ConfigSliderElementData
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.cascade.animation.extension.impl.animate
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.FocusEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.kontext.api.format.extension.color.brighten
import foo.starred.kontext.api.format.extension.component.literal
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

open class ConfigSliderElement : RoundedRectanglePrimitive() {
    private var update: (Double) -> Unit = {}
    private var _component = "".literal()
    private var _width = 0f

    var min: Double = 0.0
    var max: Double = 100.0

    var double: Boolean = false
        set(v) {
            if (field == v) return
            field = v
            fn()
        }

    var unit: String = ""
        set(v) {
            if (field == v) return
            field = v
            fn()
        }

    var value: Double = 0.0
        set(v) {
            val clamped = min(max(v, min), max)
            if (field == clamped) return
            field = clamped
            fn()
            update(clamped)
        }

    init {
        fn()

        color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
        radius = CascadeGeometricRadius(4f)
        unfocus = false

        effect(OutlineEffect {
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            inset = false
        })

        on<FocusEvent.Gain> {
            animate(150.milliseconds) {
                ::color to CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            }

            effect<OutlineEffect>()?.color = CascadeGeometricColor(MochaColorScheme.Lavender.argb)
        }

        on<FocusEvent.Lose> {
            animate(150.milliseconds) {
                ::color to CascadeGeometricColor(if (hovered) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)
            }

            effect<OutlineEffect>()?.color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
        }

        on<MouseEvent.Press> {
            if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
            root.focused = self
            value(x)
            cancel()
        }

        on<MouseEvent.Release> {
            if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
            if (root.focused != self) return@on
            root.focused = null
            cancel()
        }

        on<MouseEvent.Move.Any> {
            if (root.focused != self) return@on
            value(x)
            cancel()
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
    }

    override fun draw(graphics: GuiGraphicsExtractor) {
        if (root.focused == self) value(mouseX.toDouble())
        super.draw(graphics)

        val value0 = if (max > min) ((value - min) / (max - min)).toFloat() else 0f
        if (value0 > 0f) {
            val pose = Matrix3x2f(graphics.pose())
            val scissor = graphics.scissorStack.peek()
            graphics.roundedRectangle(x, y, width * value0, height, MochaColorScheme.Lavender.argb.brighten(0.75f), radius, pose, scissor)
        }

        val font = CascadeFonts.sans
        val height0 = font.regular.height * 10f

        val x1 = x + (width / 2f) - (_width / 2f)
        val y1 = y + (height / 2f) - (height0 / 2f)

        font.extract(graphics, _component, x1, y1, MochaColorScheme.Text.argb, true, 10f)
    }

    fun update(block: (Double) -> Unit) {
        update = block
    }

    private fun value(x0: Double) {
        val value1 = min + ((x0 - x) / width).coerceIn(0.0, 1.0) * (max - min)
        value = if (double) (value1 * 100.0).roundToInt() / 100.0 else value1.roundToInt().toDouble()
    }

    private fun fn() {
        val display = if (double) String.format("%.2f", value) else value.roundToInt().toString()
        val suffix = if (unit.isNotEmpty()) " $unit" else ""

        _component = (display + suffix).literal()
        _width = CascadeFonts.sans.width(_component, 10f)
    }

    companion object {
        fun of(parent: IPrimitiveElement<*>, config: ConfigSliderElementData) {
            ConfigSliderElement().apply {
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(-8f, 0f)
                size = FixedSizeConstraint(100f, 14f)
                min = config.min
                max = config.max
                double = config.double
                unit = config.unit
                value = (ConfigManager.get(config.key) as? Number)?.toDouble() ?: config.default

                attach(parent)
                update {
                    ConfigManager.update(config.key, it)
                }
            }
        }
    }
}
