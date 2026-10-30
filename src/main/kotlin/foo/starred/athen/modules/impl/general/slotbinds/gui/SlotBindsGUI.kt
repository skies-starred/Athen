@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.general.slotbinds.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.general.slotbinds.impl.SlotBinds
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.stroke.stroke
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.kontext.api.format.extension.color.brighten
import foo.starred.kontext.api.format.extension.component.literal
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f

object SlotBindsGUI : CascadeScreen("Slot Binds Editor [Athen]") {
    private var deleting: String? = null
    private var renaming: String? = null
    private var selected: Int? = null

    private var left: ScrollablePrimitive
    private var preview: RectanglePrimitive
    private var empty: TextPrimitive

    private var `profile$new`: RectanglePrimitive
    private var `profile$rename`: RectanglePrimitive
    private var `profile$delete`: RectanglePrimitive
    private var `profile$field`: TextFieldComponent
    private lateinit var `profile$text$rename`: TextPrimitive
    private lateinit var `profile$text$delete`: TextPrimitive

    private data class ProfileRow(val row: RectanglePrimitive, val label: TextPrimitive)
    private data class SlotCell(val cell: RectanglePrimitive, val label: TextPrimitive)

    private val rows = LinkedHashMap<String, ProfileRow>()
    private val cells = LinkedHashMap<Int, SlotCell>()

    init {
        container {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(scene)
        }

        val main = container {
            size = FixedSizeConstraint(326, 160)
            position = CenterPositionConstraint()
            attach(scene)
        }

        val side = rectangle {
            size = FixedSizeConstraint(110, 160)
            position = FixedPositionConstraint(0, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        left = scrollable {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 136))
            position = FixedPositionConstraint(0, 0)
            attach(side)
        }

        val bar = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 24))
            position = FixedPositionConstraint(0, 136)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(side)
        }

        `profile$new` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(2f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.5f))
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                deleting = null
                renaming = null
                visible = false

                `profile$rename`.visible = false
                `profile$delete`.visible = false
                `profile$field`.reset(true)
                `profile$field`.visible = true
                scene.focused = `profile$field`

                cancel()
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))
            }

            attach(bar)
            adopt(text {
                text = "+".literal()
                color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                position = CenterPositionConstraint()
                shadow = false
            })
        }

        `profile$rename` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = CenterPositionConstraint()
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                deleting = null
                renaming = SlotBinds.store.active
                visible = false

                `profile$new`.visible = false
                `profile$delete`.visible = false
                `profile$field`.value = SlotBinds.store.active
                `profile$field`.block = SlotBinds.store.active.length
                `profile$field`.visible = true
                scene.focused = `profile$field`

                cancel()
            }

            attach(bar)
            adopt(text {
                text = "\u270F".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `profile$text$rename` = it })
        }

        `profile$delete` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(-2f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.8f))

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.5f))
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                val active = SlotBinds.store.active
                if (deleting != active) {
                    deleting = active
                    buttons()
                    return@on
                }

                if (SlotBinds.store.profiles.size > 1) {
                    SlotBinds.save()
                    SlotBinds.delete(active)
                }

                deleting = null
                selected = null
                profiles()
                slots()
                buttons()
                cancel()
            }

            attach(bar)
            adopt(text {
                text = "\uD83D\uDDD1".literal()
                color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `profile$text$delete` = it })
        }

        `profile$field` = textField {
            size = MixedSizeConstraint(PercentSizeConstraint(96f, 0f), FixedSizeConstraint(0, 18))
            position = CenterPositionConstraint()
            placeholder = "Name..."
            visible = false

            attach(bar)

            on<KeyEvent.Press> {
                if (key == InputConstants.KEY_RETURN) {
                    val v = value.trim()
                    val r = renaming

                    if (r != null) {
                        if (v.isNotEmpty() && v != r && !SlotBinds.store.profiles.containsKey(v)) {
                            SlotBinds.rename(r, v)
                        }
                        renaming = null
                    } else {
                        if (v.isNotEmpty() && !SlotBinds.store.profiles.containsKey(v)) {
                            SlotBinds.save()
                            SlotBinds.disk()
                            SlotBinds.add(v)
                        }
                    }

                    `profile$field`.visible = false
                    `profile$new`.visible = true
                    `profile$rename`.visible = true
                    `profile$delete`.visible = true
                    scene.focused = null

                    profiles()
                    slots()
                    buttons()
                    cancel()
                    return@on
                }

                if (key != InputConstants.KEY_ESCAPE) return@on
                renaming = null
                `profile$field`.visible = false
                `profile$new`.visible = true
                `profile$rename`.visible = true
                `profile$delete`.visible = true
                scene.focused = null
                cancel()
            }
        }

        preview = object : RectanglePrimitive() {
            override fun draw(graphics: GuiGraphicsExtractor) {
                super.draw(graphics)

                val x0 = x.toInt() + 16
                val y0 = y.toInt() + 48
                val y1 = y.toInt() + 112

                val pose = Matrix3x2f(graphics.pose())
                val scissor = graphics.scissorStack.peek()

                for (e in SlotBinds.m0.int2IntEntrySet()) {
                    val a = pos(e.intKey, x0, y0, y1) ?: continue
                    val b = pos(e.intValue, x0, y0, y1) ?: continue
                    graphics.stroke(a.first.toFloat(), a.second.toFloat(), b.first.toFloat(), b.second.toFloat(), SlotBinds.m2.get(e.intKey), 1f, false, pose, scissor)
                }
            }

            private fun pos(slot: Int, x: Int, y0: Int, y1: Int): Pair<Int, Int>? {
                return when (slot) {
                    in 9..35 -> (x + (slot - 9) % 9 * 20 + 9) to (y0 + (slot - 9) / 9 * 20 + 9)
                    in 36..44 -> (x + (slot - 36) * 20 + 9) to (y1 + 9)
                    else -> null
                }
            }
        }.apply {
            size = FixedSizeConstraint(210, 160)
            position = FixedPositionConstraint(116, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            interact = false
            attach(main)
        }

        text {
            text = "Preview".literal()
            color = CascadeGeometricColor(MochaColorScheme.Text.argb)
            position = FixedPositionConstraint(6, 6)
            attach(preview)
        }

        rectangle {
            size = FixedSizeConstraint(198, 1)
            position = FixedPositionConstraint(6, 18)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            interact = false
            attach(preview)
        }

        for (row in 0 until 3) for (col in 0 until 9) {
            slot(9 + row * 9 + col, 16 + col * 20, 48 + row * 20)
        }

        rectangle {
            size = FixedSizeConstraint(178, 1)
            position = FixedPositionConstraint(16, 108)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            interact = false
            attach(preview)
        }

        for (col in 0 until 9) {
            slot(36 + col, 16 + col * 20, 112)
        }

        empty = text {
            text = "No binds yet.".literal()
            color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
            offset = CascadeGeometricOffset(0f, 30f)
            attach(preview)
        }

        buttons()
        profiles()
        slots()
    }

    override fun init() {
        super.init()
        deleting = null
        renaming = null
        selected = null
        profiles()
        slots()
        buttons()
    }

    override fun onClose() {
        SlotBinds.save()
        SlotBinds.disk()
        super.onClose()
    }

    private fun profiles() {
        left.children.clear()
        rows.clear()

        var cy = 4
        for (name in SlotBinds.store.profiles.keys) {
            val b0 = name == SlotBinds.store.active

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                offset = CascadeGeometricOffset(0f, cy)
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    if (name == SlotBinds.store.active) return@on

                    SlotBinds.save()
                    SlotBinds.disk()
                    SlotBinds.load(name)
                    deleting = null
                    selected = null
                    profiles()
                    slots()
                    buttons()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    if (name != SlotBinds.store.active) color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.5f))
                }

                on<MouseEvent.Move.Exit> {
                    if (name != SlotBinds.store.active) color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                }

                attach(left)
            }

            val label = text {
                text = name.literal()
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(4f, 0f)
                attach(row)
            }

            rows[name] = ProfileRow(row, label)
            cy += 20
        }
    }

    private fun slots() {
        for (id in cells.keys) {
            val cell = cells[id] ?: continue
            val b0 = fn(id)
            val b1 = selected == id
            val i0 = if (b0) SlotBinds.sc(id) else 0

            cell.cell.color = CascadeGeometricColor(if (b1) MochaColorScheme.Surface1.argb else if (b0) MochaColorScheme.Surface2.argb else MochaColorScheme.Surface0.argb)
            cell.cell.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (b1) MochaColorScheme.Lavender.argb else if (b0) i0 else MochaColorScheme.Overlay0.argb)
            cell.label.color = CascadeGeometricColor(if (b1) MochaColorScheme.Lavender.argb else if (b0) i0 else MochaColorScheme.Subtext0.argb)
        }

        empty.text = "${SlotBinds.m0.size} binds".literal()
    }

    private fun buttons() {
        val b0 = deleting == SlotBinds.store.active
        val b1 = SlotBinds.store.profiles.size > 1

        `profile$rename`.color = CascadeGeometricColor(MochaColorScheme.Lavender.argb.brighten(0.8f))
        `profile$rename`.effect<OutlineEffect>()?.color = CascadeGeometricColor(MochaColorScheme.Lavender.argb.brighten(0.5f))
        `profile$text$rename`.color = CascadeGeometricColor(MochaColorScheme.Base.argb)

        `profile$delete`.color = CascadeGeometricColor(if (!b1) MochaColorScheme.Surface1.argb else if (b0) MochaColorScheme.Red.argb.brighten(0.9f) else MochaColorScheme.Red.argb.brighten(0.8f))
        `profile$delete`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (!b1) MochaColorScheme.Surface0.argb else MochaColorScheme.Red.argb.brighten(0.5f))
        `profile$text$delete`.color = CascadeGeometricColor(if (!b1) MochaColorScheme.Overlay0.argb else MochaColorScheme.Base.argb)
        `profile$text$delete`.text = (if (b0) "✔" else "\uD83D\uDDD1").literal()
    }

    private fun fn(int: Int): Boolean {
        return SlotBinds.m0.containsKey(int) || SlotBinds.m1.containsKey(int)
    }

    private fun slot(slot: Int, x: Int, y: Int) {
        var label0 = TextPrimitive.NONE

        val cell = rectangle {
            size = FixedSizeConstraint(18, 18)
            position = FixedPositionConstraint(x, y)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            on<MouseEvent.Press> {
                if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                    if (fn(slot)) SlotBinds.unbind(slot)
                    selected = null
                    slots()
                    cancel()
                    return@on
                }

                if (button != InputConstants.MOUSE_BUTTON_LEFT) {
                    cancel()
                    return@on
                }

                when {
                    selected == slot -> {
                        selected = null
                    }

                    selected != null && ((selected in 36..44) != (slot in 36..44)) -> {
                        SlotBinds.bind(selected ?: 0, slot)
                        selected = null
                    }

                    fn(slot) && selected == null -> {
                        SlotBinds.cycle(slot)
                    }

                    else -> {
                        selected = slot
                    }
                }

                cancel()
                slots()
            }

            attach(preview)
            adopt(text {
                text = slot.toString().literal()
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = CenterPositionConstraint()
                shadow = false
            }.also { label0 = it })
        }

        cells[slot] = SlotCell(cell, label0)
    }
}
