@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general.keybinds.ui

import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.AlignPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.CenterPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.FixedPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.data.PositionAlignment
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.data.PositionAnchor
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.AnchorPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position.MixedPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.FillSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.FixedSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.PercentSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size.MixedSizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ContainerPrimitive.Companion.container
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.RectanglePrimitive
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.RectanglePrimitive.Companion.rectangle
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ScrollablePrimitive
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ScrollablePrimitive.Companion.scrollable
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.TextPrimitive
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.TextPrimitive.Companion.text
import xyz.aerii.athen.api.rendering.ui.dsl.elements.components.impl.TextFieldComponent
import xyz.aerii.athen.api.rendering.ui.dsl.elements.components.impl.TextFieldComponent.Companion.textField
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.MouseEvent
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.KeyEvent
import xyz.aerii.athen.api.rendering.ui.dsl.screen.PrimitiveScreen
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.remove
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.update
import xyz.aerii.athen.modules.impl.general.keybinds.data.BindingEntry
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.keyName
import xyz.aerii.library.api.client
import xyz.aerii.library.utils.brighten
import xyz.aerii.library.utils.literal

object KeybindsGUI : PrimitiveScreen("Keybinds Manager [Athen]") {
    private var category: String? = null
    private var deleting: String? = null
    private var entry: Int? = null

    private var left: ScrollablePrimitive
    private var right: ScrollablePrimitive
    private var footer: RectanglePrimitive
    private var popup: KeybindPopUpModal

    private var `category$new`: RectanglePrimitive
    private var `category$toggle`: RectanglePrimitive
    private var `category$delete`: RectanglePrimitive
    private var `category$field`: TextFieldComponent
    private lateinit var `category$text$toggle`: TextPrimitive
    private lateinit var `category$text$delete`: TextPrimitive

    private var `keybind$edit`: RectanglePrimitive
    private var `keybind$delete`: RectanglePrimitive
    private lateinit var `keybind$edit$text`: TextPrimitive
    private lateinit var `keybind$delete$text`: TextPrimitive

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
            color = Mocha.Base.argb
            border = true
            borderColor = Mocha.Surface0.argb
            interact = false
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
            border = true
            borderColor = Mocha.Surface0.argb
            interact = false
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
            border = true
            borderColor = Mocha.Surface0.argb
            interact = false
            attach(main)
        }

        popup = KeybindPopUpModal(this) {
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
            border = true
            borderColor = Mocha.Surface0.argb
            attach(side0)
        }

        `category$new` = rectangle {
            size = PercentSizeConstraint(31f, 84f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 2)
            color = Mocha.Green.argb.brighten(0.8f)
            border = true
            borderColor = Mocha.Green.argb.brighten(0.5f)

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                deleting = null
                visible = false

                `category$toggle`.visible = false
                `category$delete`.visible = false
                `category$field`.value = ""
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
            border = true
            borderColor = Mocha.Surface0.argb

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val name = category ?: return@on

                Keybinds.toggleCategory(name)
                rows0[name]?.label?.color = if (Keybinds.categories.value.find { it.name == name }?.enabled != true) Mocha.Overlay0.argb else Mocha.Mauve.argb
                buttons()

                for ((index, binding) in Keybinds.bindings.value.withIndex()) {
                    if (binding.category == name) entry(index)
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
            border = true
            borderColor = Mocha.Surface0.argb

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on

                val name = category ?: return@on
                if (deleting != name) {
                    deleting = name
                    buttons()
                    return@on
                }

                Keybinds.removeCategory(name)
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
                if (key == GLFW.GLFW_KEY_ENTER) {
                    val v = value.trim()
                    if (v.isNotEmpty()) Keybinds.addCategory(v)

                    `category$field`.visible = false
                    `category$new`.visible = true
                    `category$toggle`.visible = true
                    `category$delete`.visible = true
                    scene.focused = null

                    categories()
                    cancel()
                    return@on
                }

                if (key != GLFW.GLFW_KEY_ESCAPE) return@on
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
            border = true
            borderColor = Mocha.Green.argb.brighten(0.5f)

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
                text = "Create keybind".literal()
                color = Mocha.Base.argb
                shadow = false
                position = CenterPositionConstraint()
            })
        }

        `keybind$edit` = rectangle {
            size = PercentSizeConstraint(32.8f, 78f)
            position = AnchorPositionConstraint({ create }, PositionAnchor.RIGHT, 3)
            color = Mocha.Surface1.argb
            border = true
            borderColor = Mocha.Surface0.argb

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                val current = entry ?: return@on
                val entry = Keybinds.bindings.value.mapIndexed { i, b -> BindingEntry(i, b) }.find { it.index == current } ?: return@on
                popup.visible = true
                popup.open(entry, category)
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = Mocha.Lavender.argb.brighten(0.9f)
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = Mocha.Lavender.argb.brighten(0.8f)
            }

            attach(footer)
            adopt(text {
                text = "Edit keybind".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `keybind$edit$text` = it })
        }

        `keybind$delete` = rectangle {
            size = PercentSizeConstraint(32.2f, 78f)
            position = AnchorPositionConstraint({ `keybind$edit` }, PositionAnchor.RIGHT, 3)
            color = Mocha.Surface1.argb
            border = true
            borderColor = Mocha.Surface0.argb

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                if (entry == null) return@on

                entry?.remove()
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
                text = "Delete keybind".literal()
                color = Mocha.Overlay0.argb
                shadow = false
                position = CenterPositionConstraint()
            }.also { `keybind$delete$text` = it })
        }

        categories()
        list()
    }

    override fun onClose() {
        Keybinds.storage.save()
        super.onClose()
    }

    private fun categories() {
        left.children.clear()
        rows0.clear()

        val kv = sequenceOf(null to "Global") + Keybinds.categories.value.map { it.name to it.name }
        var cy = 4

        for ((k, v) in kv) {
            val b0 = category == k
            val b1 = k == null || Keybinds.categories.value.find { it.name == k }?.enabled == true

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

            val labelText = text {
                text = v.literal()
                color = if (!b1) Mocha.Overlay0.argb else if (b0) Mocha.Mauve.argb else Mocha.Subtext0.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 4)
                attach(row)
            }

            rows0[k] = CategoryRow(row, labelText)
            cy += 20
        }
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val all = Keybinds.bindings.value.mapIndexed { i, b -> BindingEntry(i, b) }
        val list = if (category != null) all.filter { it.binding.category == category } else all

        if (list.isEmpty()) {
            text {
                text = "No keybinds".literal()
                color = Mocha.Subtext0.argb
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for (entry in list) {
            val b0 = entry.binding.enabled && (entry.binding.category.isEmpty() || Keybinds.categories.value.find { it.name == entry.binding.category }?.enabled != false)
            val b1 = KeybindsGUI.entry == entry.index

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = if (b1) Mocha.Surface1.argb else if (!b0) Mocha.Red.withAlpha(0.15f) else Mocha.Surface0.argb
                border = true
                borderColor = if (b1) Mocha.Mauve.argb else if (!b0) Mocha.Red.withAlpha(0.6f) else Mocha.Overlay0.argb

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on

                    val n = if (KeybindsGUI.entry == entry.index) null else entry.index
                    if (KeybindsGUI.entry == n) return@on

                    val previous = KeybindsGUI.entry
                    KeybindsGUI.entry = n
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
                border = true
                borderColor = Mocha.Surface2.argb

                on<MouseEvent.Press> {
                    cancel()
                    if (button != 0) return@on
                    val current = Keybinds.bindings.value.getOrNull(entry.index) ?: return@on

                    entry.index.update(current.keys, current.command, !current.enabled, current.category, current.condition)
                    rows1[entry.index]?.toggle?.visible = !current.enabled
                    entry(entry.index)
                }

                attach(row)
                adopt(rectangle {
                    size = FixedSizeConstraint(8, 8)
                    position = CenterPositionConstraint()
                    color = Mocha.Green.argb
                    interact = false
                    visible = entry.binding.enabled
                }.also { a = it })
            }

            rectangle {
                val str = entry.binding.keys.str()

                size = FixedSizeConstraint(client.font?.width(str)?.plus(8) ?: 20, 16)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 30)
                color = Mocha.Surface2.argb
                border = true
                borderColor = Mocha.Crust.argb
                interact = false

                attach(row)
                adopt(text {
                    text = str.literal()
                    color = Mocha.Text.argb
                    position = CenterPositionConstraint()
                })

                adopt(text {
                    text = entry.binding.command.literal()
                    color = Mocha.Text.argb
                    position = MixedPositionConstraint(AnchorPositionConstraint({ this@rectangle }, PositionAnchor.RIGHT, 8), CenterPositionConstraint())
                    attach(row)
                })
            }

            rows1[entry.index] = EntryRow(row, a)
            cy += 32
        }
    }

    private fun category(name: String?) {
        val entry = rows0[name] ?: return
        val b0 = category == name
        val b1 = name == null || Keybinds.categories.value.find { it.name == name }?.enabled == true

        entry.row.color = if (b0) Mocha.Surface0.argb else Mocha.Base.argb
        entry.label.color = if (!b1) Mocha.Overlay0.argb else if (b0) Mocha.Mauve.argb else Mocha.Subtext0.argb
    }

    private fun buttons() {
        val b0 = category != null
        val b1 = b0 && Keybinds.categories.value.find { it.name == category }?.enabled == true
        val b2 = b0 && deleting == category

        `category$toggle`.color = if (!b0) Mocha.Surface1.argb else if (b1) Mocha.Lavender.argb.brighten(0.8f) else Mocha.Surface1.argb
        `category$toggle`.borderColor = if (!b0) Mocha.Surface0.argb else if (b1) Mocha.Lavender.argb.brighten(0.5f) else Mocha.Overlay0.argb
        `category$text$toggle`.color = if (!b0) Mocha.Overlay0.argb else Mocha.Base.argb

        `category$delete`.color = if (!b0) Mocha.Surface1.argb else if (b2) Mocha.Red.argb.brighten(0.9f) else Mocha.Red.argb.brighten(0.8f)
        `category$delete`.borderColor = if (!b0) Mocha.Surface0.argb else Mocha.Red.argb.brighten(0.5f)
        `category$text$delete`.color = if (!b0) Mocha.Overlay0.argb else Mocha.Base.argb
        `category$text$delete`.text = (if (b2) "✔" else "\uD83D\uDDD1").literal()
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val binding = Keybinds.bindings.value.getOrNull(index) ?: return
        val b0 = binding.enabled && (binding.category.isEmpty() || Keybinds.categories.value.find { it.name == binding.category }?.enabled != false)
        val b1 = entry == index

        row.row.color = if (b1) Mocha.Surface1.argb else if (!b0) Mocha.Red.withAlpha(0.15f) else Mocha.Surface0.argb
        row.row.borderColor = if (b1) Mocha.Mauve.argb else if (!b0) Mocha.Red.withAlpha(0.6f) else Mocha.Overlay0.argb
    }

    private fun footer() {
        val b = entry != null
        `keybind$edit`.color = if (b) Mocha.Lavender.argb.brighten(0.8f) else Mocha.Surface1.argb
        `keybind$edit`.borderColor = if (b) Mocha.Lavender.argb.brighten(0.5f) else Mocha.Surface0.argb
        `keybind$edit$text`.color = if (b) Mocha.Base.argb else Mocha.Overlay0.argb

        `keybind$delete`.color = if (b) Mocha.Red.argb.brighten(0.8f) else Mocha.Surface1.argb
        `keybind$delete`.borderColor = if (b) Mocha.Red.argb.brighten(0.5f) else Mocha.Surface0.argb
        `keybind$delete$text`.color = if (b) Mocha.Base.argb else Mocha.Overlay0.argb
    }

    fun Iterable<Int>.str(): String {
        if (!iterator().hasNext()) return "None"
        return joinToString(" + ") { it.keyName }
    }
}