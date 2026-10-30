@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.general.messageactions.ui.impl

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.general.messageactions.MessageActions
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.*
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.api.format.extension.color.brighten
import foo.starred.kontext.api.format.extension.component.literal

object MessageActionsGUI : CascadeScreen("Message Actions [Athen]", CascadeGeometricResolution.FHD.of(2f)) {
    private var category: String? = null
    private var deleting: String? = null
    private var entry: Int? = null

    private var left: ScrollablePrimitive
    private var right: ScrollablePrimitive
    private var footer: RectanglePrimitive
    private var popup: MessageActionsPopUp

    private var `category$new`: RectanglePrimitive
    private var `category$toggle`: RectanglePrimitive
    private var `category$delete`: RectanglePrimitive
    private var `category$field`: TextFieldComponent
    private lateinit var `category$text$toggle`: TextPrimitive
    private lateinit var `category$text$delete`: TextPrimitive

    private var `action$edit`: RectanglePrimitive
    private var `action$delete`: RectanglePrimitive
    private lateinit var `action$edit$text`: TextPrimitive
    private lateinit var `action$delete$text`: TextPrimitive

    private data class CategoryRow(val row: RectanglePrimitive, val label: TextPrimitive)
    private data class EntryRow(val row: RectanglePrimitive, val toggle: RectanglePrimitive)

    private val rows0 = LinkedHashMap<String?, CategoryRow>()
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
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        left = scrollable {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 276))
            position = FixedPositionConstraint(0, 0)
            attach(side0)
        }

        val right0 = rectangle {
            size = FixedSizeConstraint(460, 260)
            position = FixedPositionConstraint(116, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
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
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        popup = MessageActionsPopUp(this) {
            popup.visible = false
            scene.focused = null
            list()
            footer()
        }.apply {
            attach(scene)
            visible = false
        }

        val bar = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 24))
            position = FixedPositionConstraint(0, 276)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(side0)
        }

        `category$new` = rectangle {
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
                visible = false

                `category$toggle`.visible = false
                `category$delete`.visible = false
                `category$field`.reset(true)
                `category$field`.visible = true
                scene.focused = `category$field`

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

        `category$toggle` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = CenterPositionConstraint()
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                val name = category ?: return@on

                MessageActions.toggle(name)
                rows0[name]?.label?.color = CascadeGeometricColor(if (MessageActions.store.data.categories.find { it.name == name }?.enabled != true) MochaColorScheme.Overlay0.argb else MochaColorScheme.Lavender.argb)
                buttons()
                cancel()

                for ((index, action) in MessageActions.store.data.actions.withIndex()) {
                    if (action.category == name) entry(index)
                }
            }

            attach(bar)
            adopt(text {
                text = "\uD83D\uDD01".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `category$text$toggle` = it })
        }

        `category$delete` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(-2f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                val name = category ?: return@on
                if (deleting != name) {
                    deleting = name
                    buttons()
                    return@on
                }

                MessageActions.remove(name)
                deleting = null
                category = null
                categories()
                list()
                footer()
                buttons()
                cancel()
            }

            attach(bar)
            adopt(text {
                text = "\uD83D\uDDD1".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `category$text$delete` = it })
        }

        `category$field` = textField {
            size = MixedSizeConstraint(PercentSizeConstraint(96f, 0f), FixedSizeConstraint(0, 18))
            position = CenterPositionConstraint()
            placeholder = "Name..."
            visible = false

            attach(bar)

            on<KeyEvent.Press> {
                if (key == InputConstants.KEY_RETURN) {
                    val v = value.trim()
                    if (v.isNotEmpty()) MessageActions.add(v)

                    `category$field`.visible = false
                    `category$new`.visible = true
                    `category$toggle`.visible = true
                    `category$delete`.visible = true
                    scene.focused = null

                    categories()
                    cancel()
                    return@on
                }

                if (key != InputConstants.KEY_ESCAPE) return@on
                `category$field`.visible = false
                `category$new`.visible = true
                `category$toggle`.visible = true
                `category$delete`.visible = true
                scene.focused = null
                categories()
                cancel()
            }
        }

        buttons()
        val create = rectangle {
            size = PercentSizeConstraint(32.2f, 78f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(4f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.5f))
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                popup.visible = true
                popup.open(null, category)
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Create action".literal()
                color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                shadow = false
                position = CenterPositionConstraint()
            })
        }

        `action$edit` = rectangle {
            size = PercentSizeConstraint(32.8f, 78f)
            position = AnchorPositionConstraint({ create }, PositionAnchor.RIGHT)
            offset = CascadeGeometricOffset(3f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                val current = entry ?: return@on
                val actionEntry = MessageActions.store.data.actions.getOrNull(current) ?: return@on

                popup.visible = true
                popup.open(MessageActionsPopUp.ActionEntryData(current, actionEntry), category)
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Lavender.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Lavender.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Edit action".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `action$edit$text` = it })
        }

        `action$delete` = rectangle {
            size = PercentSizeConstraint(32.2f, 78f)
            position = AnchorPositionConstraint({ `action$edit` }, PositionAnchor.RIGHT)
            offset = CascadeGeometricOffset(3f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                if (entry == null) return@on

                entry?.let { MessageActions.remove(it) }
                entry = null
                list()
                footer()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Delete action".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `action$delete$text` = it })
        }

        categories()
        list()
    }

    override fun onClose() {
        MessageActions.disk()
        super.onClose()
    }

    private fun categories() {
        left.children.clear()
        rows0.clear()

        val kv = sequenceOf(null to "Global") + MessageActions.store.data.categories.map { it.name to it.name }
        var cy = 4

        for ((k, v) in kv) {
            val b0 = category == k
            val b1 = k == null || MessageActions.store.data.categories.find { it.name == k }?.enabled == true

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                offset = CascadeGeometricOffset(0f, cy.toFloat())
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    if (category == k) return@on

                    val p = category
                    category = k
                    entry = null
                    deleting = null
                    category(p)
                    category(k)
                    list()
                    footer()
                    buttons()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    if (category != k) color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.5f))
                }

                on<MouseEvent.Move.Exit> {
                    if (category != k) color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                }

                attach(left)
            }

            val label = text {
                text = v.literal()
                color = CascadeGeometricColor(if (!b1) MochaColorScheme.Overlay0.argb else if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(4f, 0f)
                attach(row)
            }

            rows0[k] = CategoryRow(row, label)
            cy += 20
        }
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val all = MessageActions.store.data.actions.mapIndexed { i, a -> i to a }
        val filtered = if (category != null) all.filter { it.second.category == category } else all

        if (filtered.isEmpty()) {
            text {
                text = "No actions".literal()
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for ((index, action) in filtered) {
            val b0 = action.enabled && (action.category.isEmpty() || MessageActions.store.data.categories.find { it.name == action.category }?.enabled != false)
            val b1 = entry == index

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = CascadeGeometricColor(if (b1) MochaColorScheme.Surface1.argb else if (!b0) MochaColorScheme.Red.alpha(0.15f) else MochaColorScheme.Surface0.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(if (b1) MochaColorScheme.Lavender.argb else if (!b0) MochaColorScheme.Red.alpha(0.6f) else MochaColorScheme.Overlay0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                    val n = if (entry == index) null else index
                    if (entry == n) return@on

                    val previous = entry
                    entry = n
                    previous?.let(::entry)
                    n?.let(::entry)
                    footer()
                    cancel()
                }

                attach(right)
            }

            var a = RectanglePrimitive.NONE
            rectangle {
                size = FixedSizeConstraint(14, 14)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(8f, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    val current = MessageActions.store.data.actions.getOrNull(index) ?: return@on

                    MessageActions.update(index, current.copy(enabled = !current.enabled))
                    rows1[index]?.toggle?.visible = !current.enabled
                    entry(index)
                    cancel()
                }

                attach(row)
                adopt(rectangle {
                    size = FixedSizeConstraint(8, 8)
                    position = CenterPositionConstraint()
                    color = CascadeGeometricColor(MochaColorScheme.Green.argb)
                    interact = false
                    visible = action.enabled
                }.also { a = it })
            }

            val b = rectangle {
                val str = action.match.displayName

                size = FixedSizeConstraint(MinecraftFontMeasurer.width(str) + 8, 16)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(30f, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                interact = false

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Crust.argb)
                })

                attach(row)
                adopt(text {
                    text = str.literal()
                    color = CascadeGeometricColor(MochaColorScheme.Lavender.argb)
                    position = CenterPositionConstraint()
                })
            }

            var next: RectanglePrimitive = b
            if (action.cancel) {
                next = rectangle {
                    size = FixedSizeConstraint(MinecraftFontMeasurer.width("✕") + 6, 16)
                    position = AnchorPositionConstraint({ b }, PositionAnchor.RIGHT)
                    offset = CascadeGeometricOffset(4f, 0f)
                    color = CascadeGeometricColor(MochaColorScheme.Red.alpha(0.2f))
                    interact = false

                    effect(OutlineEffect {
                        color = CascadeGeometricColor(MochaColorScheme.Red.alpha(0.6f))
                    })

                    attach(row)
                    adopt(text {
                        text = "✕".literal()
                        color = CascadeGeometricColor(MochaColorScheme.Red.argb)
                        position = CenterPositionConstraint()
                    })
                }
            }

            text {
                text = action.pattern.literal()
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Text.argb else MochaColorScheme.Red.argb)
                position = MixedPositionConstraint(AnchorPositionConstraint({ next }, PositionAnchor.RIGHT), CenterPositionConstraint())
                offset = CascadeGeometricOffset(8f, 0f)
                attach(row)
            }

            rows1[index] = EntryRow(row, a)
            cy += 32
        }
    }

    private fun category(name: String?) {
        val entry = rows0[name] ?: return
        val b0 = category == name
        val b1 = name == null || MessageActions.store.data.categories.find { it.name == name }?.enabled == true

        entry.row.color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)
        entry.label.color = CascadeGeometricColor(if (!b1) MochaColorScheme.Overlay0.argb else if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
    }

    private fun buttons() {
        val b0 = category != null
        val b1 = b0 && MessageActions.store.data.categories.find { it.name == category }?.enabled == true
        val b2 = b0 && deleting == category

        `category$toggle`.color = CascadeGeometricColor(if (!b0) MochaColorScheme.Surface1.argb else if (b1) MochaColorScheme.Lavender.argb.brighten(0.8f) else MochaColorScheme.Surface1.argb)
        `category$toggle`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (!b0) MochaColorScheme.Surface0.argb else if (b1) MochaColorScheme.Lavender.argb.brighten(0.5f) else MochaColorScheme.Overlay0.argb)
        `category$text$toggle`.color = CascadeGeometricColor(if (!b0) MochaColorScheme.Overlay0.argb else MochaColorScheme.Base.argb)

        `category$delete`.color = CascadeGeometricColor(if (!b0) MochaColorScheme.Surface1.argb else if (b2) MochaColorScheme.Red.argb.brighten(0.9f) else MochaColorScheme.Red.argb.brighten(0.8f))
        `category$delete`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (!b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Red.argb.brighten(0.5f))
        `category$text$delete`.color = CascadeGeometricColor(if (!b0) MochaColorScheme.Overlay0.argb else MochaColorScheme.Base.argb)
        `category$text$delete`.text = (if (b2) "✔" else "\uD83D\uDDD1").literal()
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val action = MessageActions.store.data.actions.getOrNull(index) ?: return
        val b0 = action.enabled && (action.category.isEmpty() || MessageActions.store.data.categories.find { it.name == action.category }?.enabled != false)
        val b1 = entry == index

        row.row.color = CascadeGeometricColor(if (b1) MochaColorScheme.Surface1.argb else if (!b0) MochaColorScheme.Red.alpha(0.15f) else MochaColorScheme.Surface0.argb)
        row.row.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (b1) MochaColorScheme.Lavender.argb else if (!b0) MochaColorScheme.Red.alpha(0.6f) else MochaColorScheme.Overlay0.argb)
    }

    private fun footer() {
        val b = entry != null
        `action$edit`.color = CascadeGeometricColor(if (b) MochaColorScheme.Lavender.argb.brighten(0.8f) else MochaColorScheme.Surface1.argb)
        `action$edit`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (b) MochaColorScheme.Lavender.argb.brighten(0.5f) else MochaColorScheme.Surface0.argb)
        `action$edit$text`.color = CascadeGeometricColor(if (b) MochaColorScheme.Base.argb else MochaColorScheme.Overlay0.argb)

        `action$delete`.color = CascadeGeometricColor(if (b) MochaColorScheme.Red.argb.brighten(0.8f) else MochaColorScheme.Surface1.argb)
        `action$delete`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (b) MochaColorScheme.Red.argb.brighten(0.5f) else MochaColorScheme.Surface0.argb)
        `action$delete$text`.color = CascadeGeometricColor(if (b) MochaColorScheme.Base.argb else MochaColorScheme.Overlay0.argb)
    }
}
