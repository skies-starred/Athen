package foo.starred.athen.modules.impl.render.radial.ui.components

import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.ImagePrimitive.Companion.image
import foo.starred.cascade.primitives.impl.ItemPrimitive.Companion.item
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.utils.literal

class RadialTree(private val side0: ScrollablePrimitive) {
    fun fn() {
        side0.children.clear()
        val main = RadialEditor.main
        val sub = RadialEditor.sub
        val m0 = RadialEditor.max
        val list0 = RadialEditor.working
        val list1 = RadialEditor.collapsed

        var i0 = 4
        for (i1 in list0.indices) {
            val b0 = i1 == main && sub < 0
            val b1 = list0[i1].sub.isNotEmpty()
            val b2 = b1 && i1 !in list1

            roundedRectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 22))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, i0)
                color = if (b0) Mocha.Surface1.argb else Mocha.Mantle.argb
                radius = CascadeGeometricRadius(4f)

                effect(OutlineEffect {
                    color = if (b0) Mocha.Lavender.argb else Mocha.Surface0.argb
                })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    RadialEditor.commit()
                    RadialEditor.reload(i1, -1)
                }

                on<MouseEvent.Move.Enter> {
                    if (RadialEditor.main == i1 && RadialEditor.sub < 0) return@on
                    color = Mocha.Surface0.argb
                }

                on<MouseEvent.Move.Exit> {
                    color = if (RadialEditor.main == i1 && RadialEditor.sub < 0) Mocha.Surface1.argb else Mocha.Mantle.argb
                }

                attach(side0)
                adopt(item {
                    item = list0[i1].item
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4, 0)
                })

                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = CascadeFonts.arial.truncate(list0[i1].name.ifBlank { "..." }, 9.5f, 54f).literal()
                    textSize = 9.5f
                    color = if (b0) Mocha.Text.argb else Mocha.Subtext0.argb
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 24, 0)
                })

                if (!b0) return@roundedRectangle
                adopt(container {
                    size = FixedSizeConstraint(40, 14)
                    position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -4, 0)

                    adopt(roundedRectangle {
                        size = FixedSizeConstraint(12, 12)
                        position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 0, 0)
                        color = Mocha.Surface1.argb
                        radius = CascadeGeometricRadius(2.5f)

                        effect(OutlineEffect {
                            color = Mocha.Overlay0.argb
                        })

                        on<MouseEvent.Press> {
                            cancel()
                            if (button != 0) return@on
                            if (i1 <= 0) return@on

                            RadialEditor.commit()
                            val tmp0 = list0[i1]
                            list0[i1] = list0[i1 - 1]
                            list0[i1 - 1] = tmp0
                            RadialEditor.reload(i1 - 1, -1)
                        }

                        adopt(image {
                            location = ResourceAPI.identify("textures/gui/chevron.png")
                            rotation = 0f
                            size = FixedSizeConstraint(6f, 6f)
                            color = Mocha.Text.argb
                            position = CenterPositionConstraint()
                            interact = false
                        })
                    })

                    adopt(roundedRectangle {
                        size = FixedSizeConstraint(12, 12)
                        position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.CENTER, 0, 0)
                        color = Mocha.Surface1.argb
                        radius = CascadeGeometricRadius(2.5f)

                        effect(OutlineEffect {
                            color = Mocha.Overlay0.argb
                        })

                        on<MouseEvent.Press> {
                            cancel()
                            if (button != 0) return@on
                            if (i1 >= list0.lastIndex) return@on

                            RadialEditor.commit()
                            val tmp0 = list0[i1]
                            list0[i1] = list0[i1 + 1]
                            list0[i1 + 1] = tmp0
                            RadialEditor.reload(i1 + 1, -1)
                        }

                        adopt(image {
                            location = ResourceAPI.identify("textures/gui/chevron.png")
                            rotation = 180f
                            size = FixedSizeConstraint(6f, 6f)
                            color = Mocha.Text.argb
                            position = CenterPositionConstraint()
                            interact = false
                        })
                    })

                    adopt(roundedRectangle {
                        size = FixedSizeConstraint(12, 12)
                        position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, 0, 0)
                        color = Mocha.Surface1.argb
                        radius = CascadeGeometricRadius(2.5f)

                        effect(OutlineEffect {
                            color = Mocha.Overlay0.argb
                        })

                        on<MouseEvent.Press> {
                            cancel()
                            if (button != 0) return@on
                            RadialEditor.commit()
                            list0.removeAt(i1)
                            RadialEditor.reload(maxOf(0, i1 - 1), -1)
                        }

                        adopt(text {
                            wrapper = CascadeTextWrapper
                            text = "×".literal()
                            textSize = 8.5f
                            color = Mocha.Red.argb
                            position = CenterPositionConstraint()
                        })
                    })
                })
            }

            i0 += 24

            if (b2) {
                for (i2 in list0[i1].sub.indices) {
                    val b3 = i1 == main && i2 == sub

                    roundedRectangle {
                        size = MixedSizeConstraint(PercentSizeConstraint(84f, 0f), FixedSizeConstraint(0, 20))
                        position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.START, -4, i0)
                        color = if (b3) Mocha.Surface1.argb else Mocha.Mantle.argb
                        radius = CascadeGeometricRadius(4f)

                        effect(OutlineEffect {
                            color = if (b3) Mocha.Lavender.argb else Mocha.Surface0.argb
                        })

                        on<MouseEvent.Press> {
                            cancel()
                            if (button != 0) return@on
                            RadialEditor.commit()
                            RadialEditor.reload(i1, i2)
                        }

                        on<MouseEvent.Move.Enter> {
                            if (RadialEditor.main == i1 && RadialEditor.sub == i2) return@on
                            color = Mocha.Surface0.argb
                        }

                        on<MouseEvent.Move.Exit> {
                            color = if (RadialEditor.main == i1 && RadialEditor.sub == i2) Mocha.Surface1.argb else Mocha.Mantle.argb
                        }

                        attach(side0)

                        adopt(item {
                            item = list0[i1].sub[i2].item
                            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4, 0)
                        })

                        adopt(text {
                            wrapper = CascadeTextWrapper
                            text = CascadeFonts.arial.truncate(list0[i1].sub[i2].name.ifBlank { "..." }, 9f, 42f).literal()
                            textSize = 9f
                            color = if (b3) Mocha.Text.argb else Mocha.Subtext0.argb
                            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 22, 0)
                        })

                        if (!b3) return@roundedRectangle
                        adopt(container {
                            size = FixedSizeConstraint(40, 14)
                            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -4, 0)

                            adopt(roundedRectangle {
                                size = FixedSizeConstraint(12, 12)
                                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 0, 0)
                                color = Mocha.Surface1.argb
                                radius = CascadeGeometricRadius(2.5f)

                                effect(OutlineEffect {
                                    color = Mocha.Overlay0.argb
                                })

                                on<MouseEvent.Press> {
                                    cancel()
                                    if (button != 0) return@on
                                    if (i2 <= 0) return@on

                                    RadialEditor.commit()
                                    val parent0 = list0[i1]
                                    val list2 = parent0.sub.toMutableList()
                                    val tmp1 = list2[i2]
                                    list2[i2] = list2[i2 - 1]
                                    list2[i2 - 1] = tmp1
                                    parent0.sub = list2
                                    RadialEditor.reload(i1, i2 - 1)
                                }

                                adopt(image {
                                    location = ResourceAPI.identify("textures/gui/chevron.png")
                                    rotation = 0f
                                    size = FixedSizeConstraint(6f, 6f)
                                    color = Mocha.Text.argb
                                    position = CenterPositionConstraint()
                                    interact = false
                                })
                            })

                            adopt(roundedRectangle {
                                size = FixedSizeConstraint(12, 12)
                                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.CENTER, 0, 0)
                                color = Mocha.Surface1.argb
                                radius = CascadeGeometricRadius(2.5f)

                                effect(OutlineEffect {
                                    color = Mocha.Overlay0.argb
                                })

                                on<MouseEvent.Press> {
                                    cancel()
                                    if (button != 0) return@on
                                    if (i2 >= list0[i1].sub.lastIndex) return@on

                                    RadialEditor.commit()
                                    val parent0 = list0[i1]
                                    val list2 = parent0.sub.toMutableList()
                                    val tmp1 = list2[i2]
                                    list2[i2] = list2[i2 + 1]
                                    list2[i2 + 1] = tmp1
                                    parent0.sub = list2
                                    RadialEditor.reload(i1, i2 + 1)
                                }

                                adopt(image {
                                    location = ResourceAPI.identify("textures/gui/chevron.png")
                                    rotation = 180f
                                    size = FixedSizeConstraint(6f, 6f)
                                    color = Mocha.Text.argb
                                    position = CenterPositionConstraint()
                                    interact = false
                                })
                            })

                            adopt(roundedRectangle {
                                size = FixedSizeConstraint(12, 12)
                                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, 0, 0)
                                color = Mocha.Surface1.argb
                                radius = CascadeGeometricRadius(2.5f)

                                effect(OutlineEffect {
                                    color = Mocha.Overlay0.argb
                                })

                                on<MouseEvent.Press> {
                                    cancel()
                                    if (button != 0) return@on
                                    RadialEditor.commit()
                                    val parent0 = list0[i1]
                                    val list2 = parent0.sub.toMutableList()
                                    list2.removeAt(i2)
                                    parent0.sub = list2
                                    RadialEditor.reload(i1, if (parent0.sub.isEmpty()) -1 else maxOf(0, i2 - 1))
                                }

                                adopt(text {
                                    wrapper = CascadeTextWrapper
                                    text = "×".literal()
                                    textSize = 8.5f
                                    color = Mocha.Red.argb
                                    position = CenterPositionConstraint()
                                })
                            })
                        })
                    }

                    rectangle {
                        size = FixedSizeConstraint(1, 14)
                        position = FixedPositionConstraint(10, i0 + 3)
                        color = Mocha.Surface0.argb
                        interact = false

                        attach(side0)
                    }

                    i0 += 22
                }
            }

            if (b2 || (b0 && !b1)) {
                roundedRectangle {
                    size = MixedSizeConstraint(PercentSizeConstraint(84f, 0f), FixedSizeConstraint(0, 18))
                    position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.START, -4, i0)
                    color = Mocha.Mantle.argb
                    radius = CascadeGeometricRadius(4f)

                    effect(OutlineEffect {
                        color = Mocha.Surface0.argb
                    })

                    attach(side0)

                    on<MouseEvent.Press> {
                        cancel()
                        if (button != 0) return@on
                        RadialEditor.commit()
                        val parent0 = list0.getOrNull(i1) ?: return@on
                        if (parent0.sub.size >= m0) return@on

                        parent0.sub += RadialSlot("New Sub")
                        list1.remove(i1)
                        RadialEditor.reload(i1, list0[i1].sub.lastIndex)
                    }

                    on<MouseEvent.Move.Enter> {
                        color = Mocha.Surface0.argb
                    }

                    on<MouseEvent.Move.Exit> {
                        color = Mocha.Mantle.argb
                    }

                    adopt(text {
                        wrapper = CascadeTextWrapper
                        text = "+ Sub".literal()
                        textSize = 8.5f
                        color = Mocha.Green.argb
                        position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 6, 0)
                    })
                }

                rectangle {
                    size = FixedSizeConstraint(1, 14)
                    position = FixedPositionConstraint(10, i0 + 2)
                    color = Mocha.Surface0.argb
                    interact = false

                    attach(side0)
                }

                i0 += 22
            }
        }

        roundedRectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 22))
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, i0)
            color = Mocha.Mantle.argb
            radius = CascadeGeometricRadius(4f)

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            attach(side0)

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                RadialEditor.commit()
                list0.add(RadialSlot("New Slot"))
                RadialEditor.reload(list0.lastIndex, -1)
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Surface0.argb
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Mantle.argb
            }

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "+ Slot".literal()
                textSize = 9.5f
                color = Mocha.Green.argb
                position = CenterPositionConstraint()
            })
        }

        container {
            size = FixedSizeConstraint(10, 4)
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, i0 + 24)
            interact = false
            attach(side0)
        }
    }
}
