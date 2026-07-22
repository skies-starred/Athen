@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general.slotbinds

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.data.PositionAlignment
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.AlignPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.CenterPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.FixedPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.FillSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.FixedSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.MixedSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.PercentSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.components.impl.TextFieldComponent
import xyz.aerii.athen.api.rendering.ui.dsl.elements.components.impl.TextFieldComponent.Companion.textField
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ContainerPrimitive.Companion.container
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.RectanglePrimitive
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.RectanglePrimitive.Companion.rectangle
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ScrollablePrimitive
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ScrollablePrimitive.Companion.scrollable
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.TextPrimitive
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.TextPrimitive.Companion.text
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.KeyEvent
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.MouseEvent
import xyz.aerii.athen.api.rendering.ui.dsl.screen.PrimitiveScreen
import xyz.aerii.athen.api.rendering.ui.shapes.line.line
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.library.utils.brighten
import xyz.aerii.library.utils.literal

object SlotBindsGUI : PrimitiveScreen("Slot Binds Editor [Athen]") {
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
            color = Mocha.Base.argb
            border = true
            borderColor = Mocha.Surface0.argb
            interact = false
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
            color = Mocha.Base.argb
            border = true
            borderColor = Mocha.Surface0.argb
            attach(side)
        }

        `profile$new` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 2)
            color = Mocha.Green.argb.brighten(0.8f)
            border = true
            borderColor = Mocha.Green.argb.brighten(0.5f)

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                deleting = null
                renaming = null
                visible = false

                `profile$rename`.visible = false
                `profile$delete`.visible = false
                `profile$field`.reset(true)
                `profile$field`.visible = true
                scene.focused = `profile$field`
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Green.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Green.argb.brighten(0.8f)
            }

            attach(bar)
            adopt(text {
                text = "+".literal()
                color = Mocha.Base.argb
                position = CenterPositionConstraint()
                shadow = false
            })
        }

        `profile$rename` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = CenterPositionConstraint()
            color = Mocha.Surface1.argb
            border = true
            borderColor = Mocha.Surface0.argb

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                deleting = null
                renaming = SlotBinds.active
                visible = false

                `profile$new`.visible = false
                `profile$delete`.visible = false
                `profile$field`.value = SlotBinds.active
                `profile$field`.cursor = SlotBinds.active.length
                `profile$field`.visible = true
                scene.focused = `profile$field`
            }

            attach(bar)
            adopt(text {
                text = "\u270F".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `profile$text$rename` = it })
        }

        `profile$delete` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -2)
            color = Mocha.Red.argb.brighten(0.8f)
            border = true
            borderColor = Mocha.Red.argb.brighten(0.5f)

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val active = SlotBinds.active

                if (deleting != active) {
                    deleting = active
                    buttons()
                    return@on
                }

                if (SlotBinds.map0.size > 1) {
                    SlotBinds.save()
                    SlotBinds.delete(active)
                }

                deleting = null
                selected = null
                profiles()
                slots()
                buttons()
            }

            attach(bar)
            adopt(text {
                text = "\uD83D\uDDD1".literal()
                color = Mocha.Base.argb
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
                if (key == GLFW.GLFW_KEY_ENTER) {
                    val v = value.trim()
                    val r = renaming

                    if (r != null) {
                        if (v.isNotEmpty() && v != r && !SlotBinds.map0.containsKey(v)) {
                            SlotBinds.rename(r, v)
                        }
                        renaming = null
                    } else {
                        if (v.isNotEmpty() && !SlotBinds.map0.containsKey(v)) {
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

                if (key != GLFW.GLFW_KEY_ESCAPE) return@on
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
            override fun render(graphics: GuiGraphics) {
                super.render(graphics)

                val x0 = x + 16
                val y0 = y + 48
                val y1 = y + 112

                for (e in SlotBinds.m0.int2IntEntrySet()) {
                    val a = pos(e.intKey, x0, y0, y1) ?: continue
                    val b = pos(e.intValue, x0, y0, y1) ?: continue
                    graphics.line(a.first, a.second, b.first, b.second, SlotBinds.m2.get(e.intKey), 1)
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
            color = Mocha.Base.argb
            border = true
            borderColor = Mocha.Surface0.argb
            interact = false
            attach(main)
        }

        text {
            text = "Preview".literal()
            color = Mocha.Text.argb
            position = FixedPositionConstraint(6, 6)
            attach(preview)
        }

        rectangle {
            size = FixedSizeConstraint(198, 1)
            position = FixedPositionConstraint(6, 18)
            color = Mocha.Surface0.argb
            interact = false
            attach(preview)
        }

        for (row in 0 until 3) for (col in 0 until 9) {
            slot(9 + row * 9 + col, 16 + col * 20, 48 + row * 20)
        }

        rectangle {
            size = FixedSizeConstraint(178, 1)
            position = FixedPositionConstraint(16, 108)
            color = Mocha.Surface0.argb
            interact = false
            attach(preview)
        }

        for (col in 0 until 9) {
            slot(36 + col, 16 + col * 20, 112)
        }

        empty = text {
            text = "No binds yet.".literal()
            color = Mocha.Overlay0.argb
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, 30)
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
        for (name in SlotBinds.map0.keys) {
            val b0 = name == SlotBinds.active

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, cy)
                color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    if (name == SlotBinds.active) return@on

                    SlotBinds.save()
                    SlotBinds.disk()
                    SlotBinds.load(name)
                    deleting = null
                    selected = null
                    profiles()
                    slots()
                    buttons()
                }

                on<MouseEvent.Move.Enter> {
                    if (name != SlotBinds.active) color = Mocha.Surface0.withAlpha(0.5f)
                }

                on<MouseEvent.Move.Exit> {
                    if (name != SlotBinds.active) color = Mocha.Base.argb
                }

                attach(left)
            }

            val label = text {
                text = name.literal()
                color = if (b0) Mocha.Mauve.argb else Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4)
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

            cell.cell.color = if (b1) Mocha.Surface1.argb else if (b0) Mocha.Surface2.argb else Mocha.Surface0.argb
            cell.cell.borderColor = if (b1) Mocha.Lavender.argb else if (b0) i0 else Mocha.Overlay0.argb
            cell.label.color = if (b1) Mocha.Lavender.argb else if (b0) i0 else Mocha.Subtext0.argb
        }

        empty.text = "${SlotBinds.m0.size} binds".literal()
    }

    private fun buttons() {
        val b0 = deleting == SlotBinds.active
        val b1 = SlotBinds.map0.size > 1

        `profile$rename`.color = Mocha.Lavender.argb.brighten(0.8f)
        `profile$rename`.borderColor = Mocha.Lavender.argb.brighten(0.5f)
        `profile$text$rename`.color = Mocha.Base.argb

        `profile$delete`.color = if (!b1) Mocha.Surface1.argb else if (b0) Mocha.Red.argb.brighten(0.9f) else Mocha.Red.argb.brighten(0.8f)
        `profile$delete`.borderColor = if (!b1) Mocha.Surface0.argb else Mocha.Red.argb.brighten(0.5f)
        `profile$text$delete`.color = if (!b1) Mocha.Overlay0.argb else Mocha.Base.argb
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
            color = Mocha.Surface0.argb
            border = true
            borderColor = Mocha.Overlay0.argb

            on<MouseEvent.Press> {
                if (button == 1) {
                    if (fn(slot)) SlotBinds.unbind(slot)
                    selected = null
                    slots()
                    cancel()
                    return@on
                }

                if (button != 0) {
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
                color = Mocha.Subtext0.argb
                position = CenterPositionConstraint()
                shadow = false
            }.also { label0 = it })
        }

        cells[slot] = SlotCell(cell, label0)
    }
}