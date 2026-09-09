@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render.highlight.ui

import foo.starred.athen.modules.impl.render.highlight.MobHighlight
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
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.nextTick
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.literal
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType

object MobHighlightGUI : CascadeScreen("Mob Highlights [Athen]", CascadeGeometricResolution.FHD.of(2f)) {
    private var category = false
    private var deleting: Int? = null
    private var entry: Int? = null

    private var left: ScrollablePrimitive
    private var right: ScrollablePrimitive
    private var footer: RectanglePrimitive
    private var popup: MobHighlightPopUp

    private var `highlight$add`: RectanglePrimitive
    private var `highlight$edit`: RectanglePrimitive
    private var `highlight$delete`: RectanglePrimitive
    private lateinit var `highlight$edit$outline`: OutlineEffect
    private lateinit var `highlight$delete$outline`: OutlineEffect
    private lateinit var `highlight$text$edit`: TextPrimitive
    private lateinit var `highlight$text$delete`: TextPrimitive

    private data class CategoryRow(val row: RectanglePrimitive, val label: TextPrimitive)
    private data class EntryRow(val row: RectanglePrimitive, val swatch: RectanglePrimitive, val outline: OutlineEffect)

    private val rows0 = LinkedHashMap<Boolean, CategoryRow>()
    private val rows1 = LinkedHashMap<Int, EntryRow>()

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
            size = FillSizeConstraint(4)
            position = CenterPositionConstraint()
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

        popup = MobHighlightPopUp(this) {
            popup.visible = false
            scene.focused = null
            list()
            categories()
            footer()
        }.apply {
            attach(scene)
            visible = false
        }

        `highlight$add` = rectangle {
            size = FixedSizeConstraint(120, 20)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 8)
            color = Mocha.Green.argb.brighten(0.8f)

            effect(OutlineEffect {
                color = Mocha.Green.argb.brighten(0.5f)
            })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                deleting = null
                popup.open(category)
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Green.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Green.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "+ Add Highlight".literal()
                color = Mocha.Base.argb
                position = CenterPositionConstraint()
                shadow = false
            })
        }

        `highlight$edit` = rectangle {
            size = FixedSizeConstraint(70, 20)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -82)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `highlight$edit$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val idx = entry ?: return@on
                deleting = null
                popup.open(category, idx)
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = Mocha.Lavender.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = Mocha.Lavender.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Edit".literal()
                color = Mocha.Overlay0.argb
                position = CenterPositionConstraint()
                shadow = false
            }.also { `highlight$text$edit` = it })
        }

        `highlight$delete` = rectangle {
            size = FixedSizeConstraint(70, 20)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -8)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `highlight$delete$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val idx = entry ?: return@on

                if (deleting != idx) {
                    deleting = idx
                    footer()
                    return@on
                }

                if (category) MobHighlight.e1.update { removeAt(idx) }
                else MobHighlight.e0.update { removeAt(idx) }

                deleting = null
                entry = null
                categories()
                list()
                footer()
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = Mocha.Red.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = Mocha.Red.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Delete".literal()
                color = Mocha.Overlay0.argb
                position = CenterPositionConstraint()
                shadow = false
            }.also { `highlight$text$delete` = it })
        }
    }

    fun pop(name: String?, type: EntityType<*>?, max: Int = -1) {
        open()
        nextTick {
            popup.open(name, type, max)
        }
    }

    override fun init() {
        super.init()
        entry = null
        deleting = null
        popup.visible = false
        categories()
        list()
        footer()
    }

    override fun onClose() {
        MobHighlight.json.save()
        super.onClose()
    }

    private fun categories() {
        left.children.clear()
        rows0.clear()

        val kv = listOf(false to "Named (${MobHighlight.e0.value.size})", true to "Typed (${MobHighlight.e1.value.size})")
        var cy = 4

        for ((k, v) in kv) {
            val b0 = category == k

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START, 0, cy)
                color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    if (category == k) return@on

                    val p = category
                    category = k
                    entry = null
                    deleting = null
                    category(p)
                    category(k)
                    list()
                    footer()
                }

                on<MouseEvent.Move.Enter> {
                    if (category != k) color = Mocha.Surface0.withAlpha(0.5f)
                }

                on<MouseEvent.Move.Exit> {
                    if (category != k) color = Mocha.Base.argb
                }

                attach(left)
            }

            val label = text {
                text = v.literal()
                color = if (b0) Mocha.Lavender.argb else Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 6)
                attach(row)
            }

            rows0[k] = CategoryRow(row, label)
            cy += 22
        }
    }

    private fun category(key: Boolean) {
        val entry = rows0[key] ?: return
        val b0 = category == key

        entry.row.color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb
        entry.label.color = if (b0) Mocha.Lavender.argb else Mocha.Subtext0.argb
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val total = if (category) MobHighlight.e1.value.size else MobHighlight.e0.value.size
        if (total == 0) {
            text {
                text = (if (category) "No typed highlights" else "No named highlights").literal()
                color = Mocha.Subtext0.argb
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for (i in 0 until total) {
            val bool = entry == i
            val color0: Int
            val label: String
            val max: Int
            lateinit var outline: OutlineEffect

            if (category) {
                val e = MobHighlight.e1.value[i]
                color0 = e.color
                label = BuiltInRegistries.ENTITY_TYPE.getKey(e.type).toString()
                max = e.max
            } else {
                val e = MobHighlight.e0.value[i]
                color0 = e.color
                label = e.name
                max = e.max
            }

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = if (bool) Mocha.Surface1.argb else Mocha.Surface0.argb

                effect(OutlineEffect {
                    color = if (bool) Mocha.Lavender.argb else Mocha.Overlay0.argb
                }.also { outline = it })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    val n = if (entry == i) null else i
                    if (entry == n) return@on

                    val previous = entry
                    entry = n
                    deleting = null
                    previous?.let(::entry)
                    n?.let(::entry)
                    footer()
                }

                attach(right)
            }

            val swatch = rectangle {
                size = FixedSizeConstraint(14, 14)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 8)
                color = color0 or 0xFF000000.toInt()
                interact = false

                effect(OutlineEffect {
                    color = Mocha.Surface2.argb
                })

                attach(row)
            }

            text {
                text = label.literal()
                color = Mocha.Text.argb
                position = MixedPositionConstraint(AnchorPositionConstraint({ swatch }, PositionAnchor.RIGHT, 8), CenterPositionConstraint())
                attach(row)
            }

            rectangle {
                val hp = if (max == -1) "HP: any" else "HP: $max"
                val width = client.font.width(hp) + 8

                size = FixedSizeConstraint(width, 14)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -8)
                color = Mocha.Surface2.argb
                interact = false

                effect(OutlineEffect {
                    color = Mocha.Crust.argb
                })

                attach(row)
                adopt(text {
                    text = hp.literal()
                    color = if (max == -1) Mocha.Subtext0.argb else Mocha.Peach.argb
                    position = CenterPositionConstraint()
                })
            }

            rows1[i] = EntryRow(row, swatch, outline)
            cy += 32
        }
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val bool = entry == index

        row.row.color = if (bool) Mocha.Surface1.argb else Mocha.Surface0.argb
        row.outline.color = if (bool) Mocha.Lavender.argb else Mocha.Overlay0.argb
    }

    private fun footer() {
        val bool0 = entry != null
        val bool1 = deleting != null

        `highlight$edit`.color = if (bool0) Mocha.Lavender.argb.brighten(0.8f) else Mocha.Surface1.argb
        `highlight$edit$outline`.color = if (bool0) Mocha.Lavender.argb.brighten(0.5f) else Mocha.Surface0.argb
        `highlight$text$edit`.color = if (bool0) Mocha.Base.argb else Mocha.Overlay0.argb

        `highlight$delete`.color = if (!bool0) Mocha.Surface1.argb else if (bool1) Mocha.Red.argb.brighten(0.9f) else Mocha.Red.argb.brighten(0.8f)
        `highlight$delete$outline`.color = if (!bool0) Mocha.Surface0.argb else Mocha.Red.argb.brighten(0.5f)
        `highlight$text$delete`.color = if (bool0) Mocha.Base.argb else Mocha.Overlay0.argb
        `highlight$text$delete`.text = (if (bool1) "✔" else "Delete").literal()
    }
}
