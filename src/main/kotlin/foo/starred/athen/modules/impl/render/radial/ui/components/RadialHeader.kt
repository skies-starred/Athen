@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.render.radial.ui.components

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.rendering.ui.components.impl.EditableTextComponent
import foo.starred.athen.api.rendering.ui.components.impl.EditableTextComponent.Companion.editableText
import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.ImagePrimitive.Companion.image
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.kontext.api.format.extension.component.literal

class RadialHeader(side: IPrimitiveElement<*>) {
    var title: EditableTextComponent
        private set

    init {
        val head = roundedRectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 20))
            position = FixedPositionConstraint(0, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            radius = CascadeGeometricRadius.ZERO

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(side)
        }

        roundedRectangle {
            size = FixedSizeConstraint(12, 12)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(4f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            radius = CascadeGeometricRadius(2.5f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            on<MouseEvent.Press> {
                button != InputConstants.MOUSE_BUTTON_LEFT

                RadialEditor.save()
                val list0 = RadialEditor.names
                RadialEditor.switch(list0[(list0.indexOf(RadialMenu.store.active) - 1 + list0.size) % list0.size])
                cancel()
            }

            attach(head)
            adopt(image {
                location = ResourceAPI.identify("textures/gui/chevron.png")
                rotation = -90f
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = CenterPositionConstraint()
                size = FixedSizeConstraint(6f, 6f)
                interact = false
            })
        }

        val box0 = container {
            size = FixedSizeConstraint(70, 20)
            position = FixedPositionConstraint(16, 0)

            attach(head)
        }

        title = editableText {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            textSize = 8.5f
            color0 = MochaColorScheme.Lavender.argb
            value = RadialMenu.store.active

            commit {
                RadialEditor.rename()
            }

            attach(box0)
        }

        val box1 = container {
            size = FixedSizeConstraint(40, 14)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(-4f, 0f)

            attach(head)
        }

        roundedRectangle {
            size = FixedSizeConstraint(12, 12)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            radius = CascadeGeometricRadius(2.5f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                RadialEditor.save()
                val list0 = RadialEditor.names
                RadialEditor.switch(list0[(list0.indexOf(RadialMenu.store.active) + 1) % list0.size])
                cancel()
            }

            attach(box1)
            adopt(image {
                location = ResourceAPI.identify("textures/gui/chevron.png")
                rotation = 90f
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = CenterPositionConstraint()
                size = FixedSizeConstraint(6f, 6f)
                interact = false
            })
        }

        roundedRectangle {
            size = FixedSizeConstraint(12, 12)
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.CENTER)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            radius = CascadeGeometricRadius(2.5f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                RadialEditor.save()

                var s0 = "New"
                var i0 = 1
                val list0 = RadialEditor.names
                while (list0.contains(s0)) {
                    s0 = "New ${++i0}"
                }

                RadialMenu.add(s0)
                RadialEditor.working.clear()
                RadialEditor.working.addAll(RadialMenu.slots)
                RadialEditor.reload(0, -1)
                cancel()
            }

            attach(box1)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "+".literal()
                textSize = 9f
                color = CascadeGeometricColor(MochaColorScheme.Green.argb)
                position = CenterPositionConstraint()
            })
        }

        roundedRectangle {
            size = FixedSizeConstraint(12, 12)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            radius = CascadeGeometricRadius(2.5f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                if (RadialEditor.names.size <= 1) return@on

                RadialEditor.commit()
                RadialMenu.slots.clear()
                RadialMenu.slots.addAll(RadialEditor.working)
                RadialMenu.delete(RadialMenu.store.active)
                RadialEditor.working.clear()
                RadialEditor.working.addAll(RadialMenu.slots)
                RadialEditor.reload(0, -1)
                cancel()
            }

            attach(box1)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "×".literal()
                textSize = 8.5f
                color = CascadeGeometricColor(MochaColorScheme.Red.argb)
                position = CenterPositionConstraint()
            })
        }
    }

    fun fn() {
        if (title.editing) return
        title.value = RadialMenu.store.active
    }
}
