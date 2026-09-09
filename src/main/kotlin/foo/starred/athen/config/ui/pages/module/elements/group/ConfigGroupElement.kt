package foo.starred.athen.config.ui.pages.module.elements.group

import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.impl.ConfigGroupElementData
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.cascade.animation.data.AnimatableFloat
import foo.starred.cascade.animation.enums.CascadeAnimations
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ImagePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import net.minecraft.client.gui.GuiGraphicsExtractor

object ConfigGroupElement {
    fun of(parent: IPrimitiveElement<*>, config: ConfigGroupElementData, function: (Boolean) -> Unit) {
        var expanded = ConfigManager.get(config.key) as? Boolean ?: !config.collapsed

        val image = object : ImagePrimitive() {
            init {
                `animation$float` = AnimatableFloat(if (expanded) -180f else -90f)
            }

            override fun draw(graphics: GuiGraphicsExtractor) {
                rotation = `animation$float`?.value ?: 0f
                super.draw(graphics)
            }
        }.apply {
            location = ResourceAPI.identify("textures/gui/chevron.png")
            color = Catppuccin.Mocha.Subtext0.argb
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -12f, 0f)
            size = FixedSizeConstraint(8f, 8f)

            attach(parent)
        }

        rectangle {
            position = FixedPositionConstraint(0f, 0f)
            size = FixedSizeConstraint(482f, 32f)
            color = 0

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on

                expanded = !expanded
                ConfigManager.update(config.key, expanded)

                val manager = image.root.animations
                if (manager != null) image.`animation$float`?.animate(manager, if (expanded) -180f else -90f, 0.25f, CascadeAnimations.EASE_OUT)
                else image.`animation$float`?.snap(if (expanded) -180f else -90f)

                function(expanded)
            }

            attach(parent)
        }
    }
}
