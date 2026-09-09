@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer.carry.ui

import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.modules.impl.slayer.carry.impl.SlayerCarryTracker
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.*
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.api.client
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.literal

object SlayerCarryGUI : CascadeScreen("Slayer Carries [Athen]") {
    private var filter: SlayerBoss? = null
    private var selected: Int? = null
    private var deleting: Int? = null

    private var left: ScrollablePrimitive
    private var right: ScrollablePrimitive
    private var footer: RectanglePrimitive
    private var `carry$complete`: RectanglePrimitive
    private var `carry$delete`: RectanglePrimitive
    private lateinit var `carry$complete$outline`: OutlineEffect
    private lateinit var `carry$delete$outline`: OutlineEffect
    private lateinit var `carry$complete$text`: TextPrimitive
    private lateinit var `carry$delete$text`: TextPrimitive

    private data class FilterRow(val row: RectanglePrimitive, val label: TextPrimitive)
    private data class CarryRow(val row: RectanglePrimitive, val outline: OutlineEffect)

    private val rows0 = LinkedHashMap<SlayerBoss?, FilterRow>()
    private val rows1 = LinkedHashMap<Int, CarryRow>()

    init {
        container {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(scene)
        }

        val main = container {
            size = FixedSizeConstraint(576, 300)
            position = CenterPositionConstraint()
            attach(scene)
        }

        val side0 = rectangle {
            size = FixedSizeConstraint(110, 300)
            position = FixedPositionConstraint(0, 0)
            color = Mocha.Base.argb
            interact = false

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            attach(main)
        }

        left = scrollable {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 300))
            position = FixedPositionConstraint(0, 0)
            attach(side0)
        }

        val right0 = rectangle {
            size = FixedSizeConstraint(460, 260)
            position = FixedPositionConstraint(116, 0)
            color = Mocha.Base.argb
            interact = false

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            attach(main)
        }

        right = scrollable {
            size = FillSizeConstraint(6)
            position = CenterPositionConstraint()
            attach(right0)
        }

        footer = rectangle {
            size = FixedSizeConstraint(460, 34)
            position = FixedPositionConstraint(116, 266)
            color = Mocha.Base.argb
            interact = false

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            attach(main)
        }

        `carry$complete` = rectangle {
            size = PercentSizeConstraint(49f, 78f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `carry$complete$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val idx = selected ?: return@on

                SlayerCarryTracker.complete(idx)
                selected = null
                deleting = null
                list()
                footer()
            }

            on<MouseEvent.Move.Enter> {
                if (selected != null) color = Mocha.Green.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                if (selected != null) color = Mocha.Green.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Complete".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `carry$complete$text` = it })
        }

        `carry$delete` = rectangle {
            size = PercentSizeConstraint(49f, 78f)
            position = AnchorPositionConstraint({ `carry$complete` }, PositionAnchor.RIGHT, 3)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `carry$delete$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val idx = selected ?: return@on

                if (deleting != idx) {
                    deleting = idx
                    footer()
                    return@on
                }

                SlayerCarryTracker.remove(idx)
                selected = null
                deleting = null
                list()
                footer()
            }

            on<MouseEvent.Move.Enter> {
                if (selected != null) color = Mocha.Red.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                if (selected != null) color = Mocha.Red.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Delete".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `carry$delete$text` = it })
        }

        filters()
        list()
    }

    override fun onClose() {
        selected = null
        deleting = null
        super.onClose()
    }

    private fun filters() {
        left.children.clear()
        rows0.clear()

        val kv = sequenceOf(null to "Global") + SlayerBoss.entries.map { it to it.display.substringBefore(" ") }
        var cy = 4

        for ((k, v) in kv) {
            val b0 = filter == k

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, cy)
                color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    if (filter == k) return@on

                    val p = filter
                    filter = k
                    selected = null
                    deleting = null
                    filter(p)
                    filter(k)
                    list()
                    footer()
                }

                on<MouseEvent.Move.Enter> {
                    if (filter != k) color = Mocha.Surface0.withAlpha(0.5f)
                }

                on<MouseEvent.Move.Exit> {
                    if (filter != k) color = Mocha.Base.argb
                }

                attach(left)
            }

            val label = text {
                text = v.literal()
                color = if (b0) Mocha.Lavender.argb else Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4)
                attach(row)
            }

            rows0[k] = FilterRow(row, label)
            cy += 20
        }
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val all = SlayerCarryTracker.tracked.value
        val kv = if (filter != null) all.mapIndexed { i, c -> i to c }.filter { it.second.type == filter } else all.mapIndexed { i, c -> i to c }

        if (kv.isEmpty()) {
            text {
                text = "No active carries".literal()
                color = Mocha.Subtext0.argb
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for ((index, carry) in kv) {
            val b1 = selected == index
            lateinit var outline: OutlineEffect

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = if (b1) Mocha.Surface1.argb else Mocha.Surface0.argb

                effect(OutlineEffect {
                    color = if (b1) Mocha.Lavender.argb else Mocha.Overlay0.argb
                }.also { outline = it })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    val n = if (selected == index) null else index
                    if (selected == n) return@on

                    val previous = selected
                    selected = n
                    deleting = null
                    previous?.let(::entry)
                    n?.let(::entry)
                    footer()
                }

                attach(right)
            }

            rectangle {
                val s = "${carry.type.short}${carry.tier?.let { " T${it.int}" } ?: " Any"}"
                val w = client.font.width(s) + 8

                size = FixedSizeConstraint(w, 16)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 8)
                color = Mocha.Surface2.argb
                interact = false

                effect(OutlineEffect {
                    color = Mocha.Crust.argb
                })

                attach(row)
                adopt(text {
                    text = s.literal()
                    color = Mocha.Lavender.argb
                    position = CenterPositionConstraint()
                })

                adopt(text {
                    text = carry.name.literal()
                    color = Mocha.Text.argb
                    position = MixedPositionConstraint(AnchorPositionConstraint({ this@rectangle }, PositionAnchor.RIGHT, 8), CenterPositionConstraint())
                    attach(row)
                })
            }

            var offset = -8
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                color = Mocha.Surface1.argb

                effect(OutlineEffect {
                    color = Mocha.Surface0.argb
                })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max + 1, carry.done)
                    list()
                }

                on<MouseEvent.Move.Enter> {
                    color = Mocha.Surface2.argb
                }

                on<MouseEvent.Move.Exit> {
                    color = Mocha.Surface1.argb
                }

                attach(row)
                adopt(text {
                    text = "+".literal()
                    color = Mocha.Text.argb
                    position = CenterPositionConstraint()
                })
            }

            offset -= 20
            val s0 = carry.max.toString()
            text {
                text = s0.literal()
                color = Mocha.Text.argb
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                attach(row)
            }

            offset -= client.font.width(s0) + 4
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                color = Mocha.Surface1.argb

                effect(OutlineEffect {
                    color = Mocha.Surface0.argb
                })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    if (carry.max <= 1) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max - 1, carry.done)
                    list()
                }

                on<MouseEvent.Move.Enter> {
                    color = Mocha.Surface2.argb
                }

                on<MouseEvent.Move.Exit> {
                    color = Mocha.Surface1.argb
                }

                attach(row)
                adopt(text {
                    text = "-".literal()
                    color = Mocha.Text.argb
                    position = CenterPositionConstraint()
                })
            }

            offset -= 20
            text {
                text = "Total:".literal()
                color = Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                attach(row)
            }

            offset -= client.font.width("Total:") + 12
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                color = Mocha.Surface1.argb

                effect(OutlineEffect {
                    color = Mocha.Surface0.argb
                })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max, carry.done + 1)
                    list()
                }

                on<MouseEvent.Move.Enter> {
                    color = Mocha.Surface2.argb
                }

                on<MouseEvent.Move.Exit> {
                    color = Mocha.Surface1.argb
                }

                attach(row)
                adopt(text {
                    text = "+".literal()
                    color = Mocha.Text.argb
                    position = CenterPositionConstraint()
                })
            }

            offset -= 20
            val s1 = carry.done.toString()
            text {
                text = s1.literal()
                color = Mocha.Text.argb
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                attach(row)
            }

            offset -= client.font.width(s1) + 4
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                color = Mocha.Surface1.argb

                effect(OutlineEffect {
                    color = Mocha.Surface0.argb
                })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    if (carry.done <= 0) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max, carry.done - 1)
                    list()
                }

                on<MouseEvent.Move.Enter> {
                    color = Mocha.Surface2.argb
                }

                on<MouseEvent.Move.Exit> {
                    color = Mocha.Surface1.argb
                }

                attach(row)
                adopt(text {
                    text = "-".literal()
                    color = Mocha.Text.argb
                    position = CenterPositionConstraint()
                })
            }

            offset -= 20
            text {
                text = "Done:".literal()
                color = Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, offset)
                attach(row)
            }

            rows1[index] = CarryRow(row, outline)
            cy += 32
        }
    }

    private fun filter(boss: SlayerBoss?) {
        val entry = rows0[boss] ?: return
        val b0 = filter == boss

        entry.row.color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb
        entry.label.color = if (b0) Mocha.Lavender.argb else Mocha.Subtext0.argb
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val b1 = selected == index

        row.row.color = if (b1) Mocha.Surface1.argb else Mocha.Surface0.argb
        row.outline.color = if (b1) Mocha.Lavender.argb else Mocha.Overlay0.argb
    }

    private fun footer() {
        val b = selected != null
        val b2 = b && deleting == selected

        `carry$complete`.color = if (b) Mocha.Green.argb.brighten(0.8f) else Mocha.Surface1.argb
        `carry$complete$outline`.color = if (b) Mocha.Green.argb.brighten(0.5f) else Mocha.Surface0.argb
        `carry$complete$text`.color = if (b) Mocha.Base.argb else Mocha.Overlay0.argb

        `carry$delete`.color = if (!b) Mocha.Surface1.argb else if (b2) Mocha.Red.argb.brighten(0.9f) else Mocha.Red.argb.brighten(0.8f)
        `carry$delete$outline`.color = if (!b) Mocha.Surface0.argb else Mocha.Red.argb.brighten(0.5f)
        `carry$delete$text`.color = if (b) Mocha.Base.argb else Mocha.Overlay0.argb
        `carry$delete$text`.text = (if (b2) "Confirm?" else "Delete").literal()
    }
}
