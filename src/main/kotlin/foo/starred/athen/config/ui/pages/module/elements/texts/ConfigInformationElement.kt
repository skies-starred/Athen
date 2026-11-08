package foo.starred.athen.config.ui.pages.module.elements.texts

import foo.starred.athen.config.data.impl.ConfigInformationElementData
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.config.ui.ConfigUI
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.kontext.api.text.parser.impl.parse

class ConfigInformationElement(
    private val config: ConfigInformationElementData
) : RoundedRectanglePrimitive() {
    init {
        position = CenterPositionConstraint()
        size = FixedSizeConstraint(264f, 20f)
        color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.35f))
        radius = CascadeGeometricRadius(4f)

        effect(OutlineEffect {
            color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
            inset = false
        })

        adopt(text {
            wrapper = CascadeTextWrapper
            text = "<#89B4FA>! <dark_gray>| <#CDD6F4>${config.text}".parse(true)
            textSize = 9.5f
            color = CascadeGeometricColor(MochaColorScheme.Text.argb)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(6f, 0f)
        })

        val text0 = config.description.takeIf { !it.isNullOrEmpty() }
        if (text0 != null) {
            on<MouseEvent.Move.Enter> {
                ConfigUI.show(text0)
            }

            on<MouseEvent.Move.Exit> {
                ConfigUI.hide()
            }
        }
    }

    companion object {
        fun of(parent: IPrimitiveElement<*>, config: ConfigInformationElementData): ConfigInformationElement {
            return ConfigInformationElement(config).apply {
                attach(parent)
            }
        }
    }
}
