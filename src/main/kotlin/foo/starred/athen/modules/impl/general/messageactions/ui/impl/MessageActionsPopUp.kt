@file:Suppress("PrivatePropertyName", "LocalVariableName")

package foo.starred.athen.modules.impl.general.messageactions.ui.impl

import foo.starred.athen.api.rendering.ui.components.impl.MultiCheckboxComponent
import foo.starred.athen.api.rendering.ui.components.impl.MultiCheckboxComponent.Companion.multiCheckbox
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
import foo.starred.athen.modules.impl.general.messageactions.MessageActions
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionEntry
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageMatchType
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.AnchorPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.position.MixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.utils.literal

class MessageActionsPopUp(
    private val gui: CascadeScreen,
    private val onClose: () -> Unit
) : ContainerPrimitive() {
    private var entry: ActionEntryData? = null
    private var action = 0
    private var cancel = false
    private var category = ""
    private var match = MessageMatchType.CONTAINS
    private var categories: List<String> = emptyList()

    private var title: TextPrimitive = TextPrimitive.NONE
    private var pattern: TextFieldComponent
    private var delay: TextFieldComponent
    private var panel: ContainerPrimitive
    private var `checkbox$match`: MultiCheckboxComponent
    private var `checkbox$category`: MultiCheckboxComponent
    private var `cancel$box`: RectanglePrimitive
    private lateinit var `cancel$box$outline`: OutlineEffect
    private lateinit var `cancel$text`: TextPrimitive

    data class ActionEntryData(val index: Int, val entry: MessageActionEntry)
    data class ActionButton(val rect: RectanglePrimitive, val text: TextPrimitive, val outline: OutlineEffect, val id: Int)

    private val actions = mutableListOf<ActionButton>()
    private val inputs = LinkedHashMap<String, TextFieldComponent>()

    init {
        size = FillSizeConstraint()
        position = FixedPositionConstraint(0, 0)

        on<MouseEvent.Press> {
            if (root.focused is MultiCheckboxComponent) root.focused = null
            cancel()
        }

        rectangle {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            color = Mocha.Crust.withAlpha(0.6f)

            on<MouseEvent.Press> {
                if (root.focused is MultiCheckboxComponent) root.focused = null
                cancel()
            }

            attach(this@MessageActionsPopUp)
        }

        val box = rectangle {
            size = FixedSizeConstraint(380, 260)
            position = CenterPositionConstraint()
            color = Mocha.Base.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            on<MouseEvent.Press> {
                if (root.focused is MultiCheckboxComponent) root.focused = null
                cancel()
            }

            attach(this@MessageActionsPopUp)
        }

        val header = container {
            position = FixedPositionConstraint(0, 0)
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 24))
            attach(box)

            adopt(text {
                text = "Create Action".literal()
                color = Mocha.Lavender.argb
                position = MixedPositionConstraint(FixedPositionConstraint(8, 0), CenterPositionConstraint())
            }.also { title = it })
        }

        val divider = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 1))
            position = AnchorPositionConstraint({ header }, PositionAnchor.BELOW)
            color = Mocha.Surface0.argb
            attach(box)
        }

        val pattern0 = text {
            text = "Pattern".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ divider }, PositionAnchor.BELOW, 16, 8)
            attach(box)
        }

        text {
            text = "Match Type".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ divider }, PositionAnchor.BELOW, 200, 8)
            attach(box)
        }

        pattern = textField {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ pattern0 }, PositionAnchor.BELOW, 0, 2)
            placeholder = "Pattern to match"
            attach(box)
        }

        `checkbox$match` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ pattern0 }, PositionAnchor.BELOW, 184, 2)
            items = MessageMatchType.entries.map { it.displayName }

            selected {
                match == MessageMatchType.entries[it]
            }

            select {
                match = MessageMatchType.entries[it]
                `checkbox$match`.text = match.displayName
            }

            attach(box)
        }

        val action0 = text {
            text = "Action".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ pattern }, PositionAnchor.BELOW, 0, 8)
            attach(box)
        }

        val row0 = container {
            size = FixedSizeConstraint(354, 14)
            position = AnchorPositionConstraint({ action0 }, PositionAnchor.BELOW, 0, 2)
            attach(box)
        }

        val all = IMessageAction.all()
        val width = (354 - (all.size - 1) * 4) / all.size
        for ((index, type) in all.withIndex()) {
            lateinit var outline: OutlineEffect
            var label = TextPrimitive.NONE

            val rect = rectangle {
                size = FixedSizeConstraint(width, 16)
                position = FixedPositionConstraint(index * (width + 4), 0)
                color = Mocha.Surface1.argb

                effect(OutlineEffect {
                    color = Mocha.Overlay0.argb
                }.also { outline = it })

                on<MouseEvent.Press> {
                    if (button != 0) return@on
                    cancel()

                    action = type.id
                    actions()
                    values()
                }

                on<MouseEvent.Move.Enter> {
                    if (action == type.id) return@on
                    color = Mocha.Surface2.argb
                }

                on<MouseEvent.Move.Exit> {
                    if (action == type.id) return@on
                    color = Mocha.Surface1.argb
                }

                attach(row0)
                adopt(text {
                    text = type.name.literal()
                    color = Mocha.Text.argb
                    shadow = false
                    position = CenterPositionConstraint()
                }.also { label = it })
            }

            actions.add(ActionButton(rect, label, outline, type.id))
        }

        val `category$label` = text {
            text = "Category".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ row0 }, PositionAnchor.BELOW, 0, 8)
            attach(box)
        }

        `checkbox$category` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `category$label` }, PositionAnchor.BELOW, 0, 2)
            items = listOf("Uncategorized")

            selected {
                if (it == 0) category.isEmpty() else categories.getOrNull(it - 1) == category
            }

            select {
                category = if (it == 0) "" else categories.getOrElse(it - 1) { "" }
                `checkbox$category`.text = category.ifEmpty { "Uncategorized" }
            }

            attach(box)
        }

        val `delay$label` = text {
            text = "Delay".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ row0 }, PositionAnchor.BELOW, 184, 8)
            attach(box)
        }

        delay = textField {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `delay$label` }, PositionAnchor.BELOW, 0, 2)
            placeholder = "Delay (seconds)"

            on<KeyEvent.Type> {
                if (char.code < 32) return@on
                if (char.code == 127) return@on
                if (char == '.') return@on
                if (char.isDigit()) return@on

                cancel()
            }

            attach(box)
        }

        val `cancel$label` = text {
            text = "Cancel message".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ `checkbox$category` }, PositionAnchor.BELOW, 0, 8)
            attach(box)
        }

        `cancel$box` = rectangle {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `cancel$label` }, PositionAnchor.BELOW, 0, 2)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = if (cancel) Mocha.Green.argb else Mocha.Red.argb
                inset = false
            }.also { `cancel$box$outline` = it })

            on<MouseEvent.Press> {
                if (button != 0) return@on
                cancel()
                cancel = !cancel
                `cancel$box$outline`.color = if (cancel) Mocha.Green.argb else Mocha.Red.argb
                `cancel$text`.text = (if (cancel) "True" else "False").literal()
                `cancel$text`.color = if (cancel) Mocha.Green.argb else Mocha.Red.argb
            }

            attach(box)
            adopt(text {
                text = (if (cancel) "True" else "False").literal()
                color = if (cancel) Mocha.Green.argb else Mocha.Red.argb
                position = CenterPositionConstraint()
            }.also { `cancel$text` = it })
        }

        panel = container {
            size = FixedSizeConstraint(354, 70)
            position = AnchorPositionConstraint({ `checkbox$category` }, PositionAnchor.BELOW, 0, 8)
            interact = false

            attach(box)
        }

        text {
            text = $$"Regex: use $0 for full message, $1, $2... for groups".literal()
            color = Mocha.Overlay0.argb
            position = FixedPositionConstraint(16, 208)
            attach(box)
        }

        val bottom = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 1))
            position = FixedPositionConstraint(0, 220)
            color = Mocha.Surface0.argb
            attach(box)
        }

        val cancel0 = rectangle {
            size = FixedSizeConstraint(170, 22)
            position = AnchorPositionConstraint({ bottom }, PositionAnchor.BELOW, 16, 8)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Red.argb
            })

            on<MouseEvent.Press> {
                if (button != 0) return@on

                cancel()
                onClose()
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Surface2.argb
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Surface1.argb
            }

            attach(box)
            adopt(text {
                text = "Cancel".literal()
                color = Mocha.Red.argb
                position = CenterPositionConstraint()
            })
        }

        rectangle {
            size = FixedSizeConstraint(170, 22)
            position = AnchorPositionConstraint({ cancel0 }, PositionAnchor.RIGHT, 8)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Green.argb
            })

            on<MouseEvent.Press> {
                if (button != 0) return@on cancel()

                val pattern = pattern.value.trim()
                if (pattern.isEmpty()) return@on cancel()

                val delay = delay.value.toDoubleOrNull() ?: 0.0
                val fields = IMessageAction.get(action)?.fields ?: emptyList()
                val data = mutableMapOf<String, String>()

                for ((key, _, _, default) in fields) {
                    data[key] = inputs[key]?.value ?: default
                }

                val action = IMessageAction.create(action, data)
                val new = MessageActionEntry(pattern, match, action, entry?.entry?.enabled ?: true, category, cancel, delay)

                if (entry != null) MessageActions.update(entry!!.index, new)
                else MessageActions.add(new.pattern, new.match, new.action, new.category, new.cancel, new.delay)

                onClose()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Surface2.argb
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Surface1.argb
            }

            attach(box)
            adopt(text {
                text = "Save".literal()
                color = Mocha.Green.argb
                position = CenterPositionConstraint()
            })
        }
    }

    fun open(entry: ActionEntryData?, selectedCategory: String?) {
        this.entry = entry

        pattern.value = entry?.entry?.pattern ?: ""
        pattern.cursor = pattern.value.length
        match = entry?.entry?.match ?: CONTAINS
        action = entry?.entry?.action?.id ?: 0
        cancel = entry?.entry?.cancel ?: false
        category = entry?.entry?.category ?: selectedCategory ?: ""
        delay.value = entry?.entry?.delay?.takeIf { it > 0.0 }?.toString() ?: ""
        delay.cursor = delay.value.length

        title.text = (if (entry == null) "Create Action" else "Edit Action").literal()

        categories = MessageActions.categories.map { it.name }
        `checkbox$category`.items = listOf("Uncategorized") + categories
        `checkbox$category`.text = category.ifEmpty { "Uncategorized" }
        `checkbox$match`.text = match.displayName

        actions()
        values(entry?.entry?.action?.serializable)

        `cancel$box$outline`.color = if (cancel) Mocha.Green.argb else Mocha.Red.argb
        `cancel$text`.text = (if (cancel) "True" else "False").literal()
        `cancel$text`.color = if (cancel) Mocha.Green.argb else Mocha.Red.argb
        gui.scene.focused = this
    }

    private fun actions() {
        for ((rect, text, outline, id) in actions) {
            val selected = id == action

            rect.color = if (selected) Mocha.Lavender.argb else Mocha.Surface1.argb
            outline.color = if (selected) Mocha.Lavender.argb else Mocha.Overlay0.argb
            text.color = if (selected) Mocha.Base.argb else Mocha.Text.argb
        }
    }

    private fun values(map: Map<String, String>? = null) {
        panel.children.clear()
        inputs.clear()

        val fields = IMessageAction.get(action)?.fields ?: emptyList()
        if (fields.isEmpty()) {
            text {
                text = "Value".literal()
                color = Mocha.Overlay0.argb
                position = FixedPositionConstraint(184, 0)
                attach(panel)
            }

            rectangle {
                size = FixedSizeConstraint(170, 16)
                position = FixedPositionConstraint(184, 10)
                color = Mocha.Crust.argb
                interact = false

                effect(OutlineEffect {
                    color = Mocha.Surface0.argb
                    inset = false
                })

                attach(panel)
            }

            return
        }

        for ((i, field) in fields.withIndex()) {
            val (key, label, holder, default, numeric) = field

            val i1 = i + 1
            val x0 = if (i1 % 2 == 0) 0 else 184
            val y0 = (i1 / 2) * 36

            text {
                text = label.literal()
                color = Mocha.Subtext0.argb
                position = FixedPositionConstraint(x0, y0)
                attach(panel)
            }

            inputs[key] = textField {
                size = FixedSizeConstraint(170, 16)
                position = FixedPositionConstraint(x0, y0 + 10)
                placeholder = holder.ifEmpty { label }
                value = map?.get(key) ?: default
                cursor = value.length

                attach(panel)

                if (!numeric) return@textField
                on<KeyEvent.Type> {
                    if (char.code < 32) return@on
                    if (char.code == 127) return@on
                    if (char.isDigit()) return@on

                    cancel()
                }
            }
        }
    }
}
