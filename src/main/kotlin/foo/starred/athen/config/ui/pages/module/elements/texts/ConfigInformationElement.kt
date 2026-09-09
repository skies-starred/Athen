package foo.starred.athen.config.ui.pages.module.elements.texts

import foo.starred.athen.config.data.impl.ConfigInformationElementData
import foo.starred.athen.config.ui.ConfigUI
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.api.text.parser.impl.parse

class ConfigInformationElement(
    private val config: ConfigInformationElementData
) : RoundedRectanglePrimitive() {
    init {
        position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.CENTER)
        size = FixedSizeConstraint(466f, 20f)
        color = Catppuccin.Mocha.Surface0.argb
        radius = CascadeGeometricRadius(4f)

        effect(OutlineEffect {
            color = Catppuccin.Mocha.Surface1.argb
            inset = false
        })

        adopt(text {
            wrapper = CascadeTextWrapper
            text = "<#89B4FA>! <dark_gray>| <#CDD6F4>${config.text}".parse(true)
            textSize = 9.5f
            color = Catppuccin.Mocha.Text.argb
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 6f, 0f)
        })

        val text0 = config.description.takeIf { !it.isNullOrEmpty() }
        if (text0 != null) {
            on<MouseEvent.Move.Any> {
                if (!hovered) return@on
                ConfigUI.show(text0, x, y)
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
