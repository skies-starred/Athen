@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.general.messageactions.ui.impl

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
import foo.starred.athen.modules.impl.general.messageactions.MessageActions
import foo.starred.athen.ui.themes.Catppuccin.Mocha
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
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.literal

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
    private lateinit var `category$toggle$outline`: OutlineEffect
    private lateinit var `category$delete$outline`: OutlineEffect
    private var `category$field`: TextFieldComponent
    private lateinit var `category$text$toggle`: TextPrimitive
    private lateinit var `category$text$delete`: TextPrimitive

    private var `action$edit`: RectanglePrimitive
    private var `action$delete`: RectanglePrimitive
    private lateinit var `action$edit$outline`: OutlineEffect
    private lateinit var `action$delete$outline`: OutlineEffect
    private lateinit var `action$edit$text`: TextPrimitive
    private lateinit var `action$delete$text`: TextPrimitive

    private data class CategoryRow(val row: RectanglePrimitive, val label: TextPrimitive)
    private data class EntryRow(val row: RectanglePrimitive, val toggle: RectanglePrimitive, val outline: OutlineEffect)

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
            color = Mocha.Base.argb
            interact = false

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
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
            color = Mocha.Base.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            attach(side0)
        }

        `category$new` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 2)
            color = Mocha.Green.argb.brighten(0.8f)

            effect(OutlineEffect {
                color = Mocha.Green.argb.brighten(0.5f)
            })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                deleting = null
                visible = false

                `category$toggle`.visible = false
                `category$delete`.visible = false
                `category$field`.reset(true)
                `category$field`.visible = true
                scene.focused = `category$field`
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

        `category$toggle` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = CenterPositionConstraint()
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `category$toggle$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val name = category ?: return@on

                MessageActions.toggle(name)
                rows0[name]?.label?.color = if (MessageActions.categories.find { it.name == name }?.enabled != true) Mocha.Overlay0.argb else Mocha.Lavender.argb
                buttons()

                for ((index, action) in MessageActions.actions.withIndex()) {
                    if (action.category == name) entry(index)
                }
            }

            attach(bar)
            adopt(text {
                text = "\uD83D\uDD01".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `category$text$toggle` = it })
        }

        `category$delete` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -2)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `category$delete$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on

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
            }

            attach(bar)
            adopt(text {
                text = "\uD83D\uDDD1".literal()
                color = Mocha.Overlay0.argb
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
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4)
            color = Mocha.Green.argb.brighten(0.8f)

            effect(OutlineEffect {
                color = Mocha.Green.argb.brighten(0.5f)
            })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on

                popup.visible = true
                popup.open(null, category)
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Green.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Green.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Create action".literal()
                color = Mocha.Base.argb
                shadow = false
                position = CenterPositionConstraint()
            })
        }

        `action$edit` = rectangle {
            size = PercentSizeConstraint(32.8f, 78f)
            position = AnchorPositionConstraint({ create }, PositionAnchor.RIGHT, 3)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `action$edit$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val current = entry ?: return@on
                val actionEntry = MessageActions.actions.getOrNull(current) ?: return@on
                popup.visible = true
                popup.open(MessageActionsPopUp.ActionEntryData(current, actionEntry), category)
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = Mocha.Lavender.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = Mocha.Lavender.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Edit action".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `action$edit$text` = it })
        }

        `action$delete` = rectangle {
            size = PercentSizeConstraint(32.2f, 78f)
            position = AnchorPositionConstraint({ `action$edit` }, PositionAnchor.RIGHT, 3)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            }.also { `action$delete$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                if (entry == null) return@on

                entry?.let { MessageActions.remove(it) }
                entry = null
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
                text = "Delete action".literal()
                color = Mocha.Overlay0.argb
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

        val kv = sequenceOf(null to "Global") + MessageActions.categories.map { it.name to it.name }
        var cy = 4

        for ((k, v) in kv) {
            val b0 = category == k
            val b1 = k == null || MessageActions.categories.find { it.name == k }?.enabled == true

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
                    buttons()
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
                color = if (!b1) Mocha.Overlay0.argb else if (b0) Mocha.Lavender.argb else Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4)
                attach(row)
            }

            rows0[k] = CategoryRow(row, label)
            cy += 20
        }
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val all = MessageActions.actions.mapIndexed { i, a -> i to a }
        val filtered = if (category != null) all.filter { it.second.category == category } else all

        if (filtered.isEmpty()) {
            text {
                text = "No actions".literal()
                color = Mocha.Subtext0.argb
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for ((index, action) in filtered) {
            lateinit var outline: OutlineEffect
            val b0 = action.enabled && (action.category.isEmpty() || MessageActions.categories.find { it.name == action.category }?.enabled != false)
            val b1 = entry == index

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = if (b1) Mocha.Surface1.argb else if (!b0) Mocha.Red.withAlpha(0.15f) else Mocha.Surface0.argb

                effect(OutlineEffect {
                    color = if (b1) Mocha.Lavender.argb else if (!b0) Mocha.Red.withAlpha(0.6f) else Mocha.Overlay0.argb
                }.also { outline = it })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    val n = if (entry == index) null else index
                    if (entry == n) return@on

                    val previous = entry
                    entry = n
                    previous?.let(::entry)
                    n?.let(::entry)
                    footer()
                }

                attach(right)
            }

            var a = RectanglePrimitive.NONE
            rectangle {
                size = FixedSizeConstraint(14, 14)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 8)
                color = Mocha.Surface1.argb

                effect(OutlineEffect {
                    color = Mocha.Surface2.argb
                })

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    val current = MessageActions.actions.getOrNull(index) ?: return@on

                    MessageActions.update(index, current.copy(enabled = !current.enabled))
                    rows1[index]?.toggle?.visible = !current.enabled
                    entry(index)
                }

                attach(row)
                adopt(rectangle {
                    size = FixedSizeConstraint(8, 8)
                    position = CenterPositionConstraint()
                    color = Mocha.Green.argb
                    interact = false
                    visible = action.enabled
                }.also { a = it })
            }

            val b = rectangle {
                val str = action.match.displayName

                size = FixedSizeConstraint(client.font?.width(str)?.plus(8) ?: 20, 16)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 30)
                color = Mocha.Surface2.argb
                interact = false

                effect(OutlineEffect {
                    color = Mocha.Crust.argb
                })

                attach(row)
                adopt(text {
                    text = str.literal()
                    color = Mocha.Lavender.argb
                    position = CenterPositionConstraint()
                })
            }

            var next: RectanglePrimitive = b
            if (action.cancel) {
                next = rectangle {
                    size = FixedSizeConstraint(client.font?.width("✕")?.plus(6) ?: 12, 16)
                    position = AnchorPositionConstraint({ b }, PositionAnchor.RIGHT, 4)
                    color = Mocha.Red.withAlpha(0.2f)
                    interact = false

                    effect(OutlineEffect {
                        color = Mocha.Red.withAlpha(0.6f)
                    })

                    attach(row)
                    adopt(text {
                        text = "✕".literal()
                        color = Mocha.Red.argb
                        position = CenterPositionConstraint()
                    })
                }
            }

            text {
                text = action.pattern.literal()
                color = if (b0) Mocha.Text.argb else Mocha.Red.argb
                position = MixedPositionConstraint(AnchorPositionConstraint({ next }, PositionAnchor.RIGHT, 8), CenterPositionConstraint())
                attach(row)
            }

            rows1[index] = EntryRow(row, a, outline)
            cy += 32
        }
    }

    private fun category(name: String?) {
        val entry = rows0[name] ?: return
        val b0 = category == name
        val b1 = name == null || MessageActions.categories.find { it.name == name }?.enabled == true

        entry.row.color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb
        entry.label.color = if (!b1) Mocha.Overlay0.argb else if (b0) Mocha.Lavender.argb else Mocha.Subtext0.argb
    }

    private fun buttons() {
        val b0 = category != null
        val b1 = b0 && MessageActions.categories.find { it.name == category }?.enabled == true
        val b2 = b0 && deleting == category

        `category$toggle`.color = if (!b0) Mocha.Surface1.argb else if (b1) Mocha.Lavender.argb.brighten(0.8f) else Mocha.Surface1.argb
        `category$toggle$outline`.color = if (!b0) Mocha.Surface0.argb else if (b1) Mocha.Lavender.argb.brighten(0.5f) else Mocha.Overlay0.argb
        `category$text$toggle`.color = if (!b0) Mocha.Overlay0.argb else Mocha.Base.argb

        `category$delete`.color = if (!b0) Mocha.Surface1.argb else if (b2) Mocha.Red.argb.brighten(0.9f) else Mocha.Red.argb.brighten(0.8f)
        `category$delete$outline`.color = if (!b0) Mocha.Surface0.argb else Mocha.Red.argb.brighten(0.5f)
        `category$text$delete`.color = if (!b0) Mocha.Overlay0.argb else Mocha.Base.argb
        `category$text$delete`.text = (if (b2) "✔" else "\uD83D\uDDD1").literal()
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val actionEntry = MessageActions.actions.getOrNull(index) ?: return
        val b0 = actionEntry.enabled && (actionEntry.category.isEmpty() || MessageActions.categories.find { it.name == actionEntry.category }?.enabled != false)
        val b1 = entry == index

        row.row.color = if (b1) Mocha.Surface1.argb else if (!b0) Mocha.Red.withAlpha(0.15f) else Mocha.Surface0.argb
        row.outline.color = if (b1) Mocha.Lavender.argb else if (!b0) Mocha.Red.withAlpha(0.6f) else Mocha.Overlay0.argb
    }

    private fun footer() {
        val b = entry != null
        `action$edit`.color = if (b) Mocha.Lavender.argb.brighten(0.8f) else Mocha.Surface1.argb
        `action$edit$outline`.color = if (b) Mocha.Lavender.argb.brighten(0.5f) else Mocha.Surface0.argb
        `action$edit$text`.color = if (b) Mocha.Base.argb else Mocha.Overlay0.argb

        `action$delete`.color = if (b) Mocha.Red.argb.brighten(0.8f) else Mocha.Surface1.argb
        `action$delete$outline`.color = if (b) Mocha.Red.argb.brighten(0.5f) else Mocha.Surface0.argb
        `action$delete$text`.color = if (b) Mocha.Base.argb else Mocha.Overlay0.argb
    }
}
