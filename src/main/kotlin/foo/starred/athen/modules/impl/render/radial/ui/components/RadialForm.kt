@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.render.radial.ui.components

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputElement
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputElement.Companion.configInputElement
import foo.starred.athen.modules.impl.render.radial.actions.IAction
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.literal

class RadialForm(mid: ContainerPrimitive) {
    private val list = mutableListOf<RoundedRectanglePrimitive>()
    private val outlines = mutableListOf<OutlineEffect>()
    private val texts = mutableListOf<TextPrimitive>()

    var name: ConfigInputElement
        private set
    var item: ConfigInputElement
        private set
    var value: ConfigInputElement
        private set
    var texture: ConfigInputElement
        private set

    var box0: ContainerPrimitive
        private set
    var box1: ContainerPrimitive
        private set
    var text0: TextPrimitive
        private set

    init {
        val main = rectangle {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            color = Mocha.Base.argb
            interact = false

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            attach(mid)

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Edit Slot".literal()
                textSize = 10.5f
                color = Mocha.Text.argb
                position = FixedPositionConstraint(12, 10)
            })

            adopt(rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 1))
                position = FixedPositionConstraint(12, 26)
                color = Mocha.Surface0.argb
                interact = false
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Name".literal()
                textSize = 9f
                color = Mocha.Subtext0.argb
                position = FixedPositionConstraint(12, 34)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Item ID".literal()
                textSize = 9f
                color = Mocha.Subtext0.argb
                position = FixedPositionConstraint(12, 74)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Action".literal()
                textSize = 9f
                color = Mocha.Subtext0.argb
                position = FixedPositionConstraint(12, 114)
            })
        }

        name = configInputElement {
            size = MixedSizeConstraint(PercentSizeConstraint(94f, 0f), FixedSizeConstraint(0, 20))
            position = FixedPositionConstraint(12, 48)
            placeholder = "Slot name..."

            on<KeyEvent.Type> {
                RadialEditor.sync()
            }

            on<KeyEvent.Press> {
                if (key == InputConstants.KEY_RETURN) {
                    RadialEditor.commit()
                    RadialEditor.unfocus()
                    cancel()
                    return@on
                }

                if (key == InputConstants.KEY_BACKSPACE) {
                    RadialEditor.sync()
                }
            }

            attach(main)
        }

        item = configInputElement {
            size = MixedSizeConstraint(PercentSizeConstraint(94f, 0f), FixedSizeConstraint(0, 20))
            position = FixedPositionConstraint(12, 88)
            placeholder = "minecraft:barrier..."

            on<KeyEvent.Type> {
                RadialEditor.sync()
            }

            on<KeyEvent.Press> {
                if (key == InputConstants.KEY_RETURN) {
                    RadialEditor.commit()
                    RadialEditor.unfocus()
                    cancel()
                    return@on
                }

                if (key == InputConstants.KEY_BACKSPACE) {
                    RadialEditor.sync()
                }
            }

            attach(main)
        }

        val box2 = container {
            size = MixedSizeConstraint(PercentSizeConstraint(94f, 0f), FixedSizeConstraint(0, 20))
            position = FixedPositionConstraint(12, 128)

            attach(main)
        }

        val list1 = IAction.all()
        for ((i0, act0) in list1.withIndex()) {
            list.add(roundedRectangle {
                size = FixedSizeConstraint(58, 18)
                position = FixedPositionConstraint(i0 * 62, 0)
                color = Mocha.Surface1.argb
                radius = CascadeGeometricRadius(4f)

                effect(OutlineEffect {
                    color = Mocha.Overlay0.argb
                }.also { outlines.add(it) })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    RadialEditor.type = act0.id
                    RadialEditor.commit()
                    buttons()
                    fn()
                }

                attach(box2)
                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = act0.name.literal()
                    textSize = 8.5f
                    color = Mocha.Text.argb
                    position = CenterPositionConstraint()
                }.also { texts.add(it) })
            })
        }

        box0 = container {
            size = MixedSizeConstraint(PercentSizeConstraint(94f, 0f), FixedSizeConstraint(0, 36))
            position = FixedPositionConstraint(12, 154)

            attach(main)
        }

        text0 = text {
            wrapper = CascadeTextWrapper
            text = "Value".literal()
            textSize = 9f
            color = Mocha.Subtext0.argb
            position = FixedPositionConstraint(0, 0)

            attach(box0)
        }

        value = configInputElement {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 20))
            position = FixedPositionConstraint(0, 14)
            placeholder = "Action value..."

            on<KeyEvent.Type> {
                RadialEditor.commit()
            }

            on<KeyEvent.Press> {
                if (key == InputConstants.KEY_RETURN) {
                    RadialEditor.commit()
                    RadialEditor.unfocus()
                    cancel()
                    return@on
                }

                if (key == InputConstants.KEY_BACKSPACE) {
                    RadialEditor.commit()
                }
            }

            attach(box0)
        }

        box1 = container {
            size = MixedSizeConstraint(PercentSizeConstraint(94f, 0f), FixedSizeConstraint(0, 72))
            position = FixedPositionConstraint(12, 194)

            attach(main)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Texture".literal()
                textSize = 9f
                color = Mocha.Subtext0.argb
                position = FixedPositionConstraint(0, 0)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "- Run /sbapi inventory".literal()
                textSize = 8f
                color = Mocha.Overlay0.argb
                position = FixedPositionConstraint(0, 38)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "- Hover over the skull you want the texture of".literal()
                textSize = 8f
                color = Mocha.Overlay0.argb
                position = FixedPositionConstraint(0, 48)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "- Press S to copy to clipboard".literal()
                textSize = 8f
                color = Mocha.Overlay0.argb
                position = FixedPositionConstraint(0, 58)
            })
        }

        texture = configInputElement {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 20))
            position = FixedPositionConstraint(0, 14)
            placeholder = "Skin texture value..."

            on<KeyEvent.Type> {
                item.value = "player_head"
                RadialEditor.sync()
            }

            on<KeyEvent.Press> {
                if (key == InputConstants.KEY_RETURN) {
                    RadialEditor.commit()
                    RadialEditor.unfocus()
                    cancel()
                    return@on
                }

                if (key == InputConstants.KEY_BACKSPACE) {
                    RadialEditor.sync()
                }
            }

            attach(box1)
        }
    }

    fun buttons() {
        val list1 = IAction.all()
        val i1 = RadialEditor.type

        for ((k, v) in list.withIndex()) {
            val b0 = i1 == (list1.getOrNull(k)?.id ?: continue)

            v.color = if (b0) Mocha.Lavender.argb.brighten(0.9f) else Mocha.Surface1.argb
            outlines.getOrNull(k)?.color = if (b0) Mocha.Lavender.argb.brighten(0.6f) else Mocha.Overlay0.argb
            texts.getOrNull(k)?.color = if (b0) Mocha.Crust.argb else Mocha.Text.argb
        }
    }

    fun fn() {
        buttons()
        val i0 = RadialEditor.type
        text0.text = (IAction.all().firstOrNull { it.id == i0 }?.name?.takeIf { it != "None" } ?: "Value").literal()
    }
}
