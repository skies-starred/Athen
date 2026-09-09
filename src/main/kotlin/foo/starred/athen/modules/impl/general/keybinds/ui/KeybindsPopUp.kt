@file:Suppress("PrivatePropertyName")

package foo.starred.athen.modules.impl.general.keybinds.ui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.dungeon.enums.DungeonClass
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.api.rendering.ui.components.impl.MultiCheckboxComponent
import foo.starred.athen.api.rendering.ui.components.impl.MultiCheckboxComponent.Companion.multiCheckbox
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
import foo.starred.athen.modules.impl.general.keybinds.Keybinds
import foo.starred.athen.modules.impl.general.keybinds.Keybinds.add
import foo.starred.athen.modules.impl.general.keybinds.Keybinds.update
import foo.starred.athen.modules.impl.general.keybinds.data.BindingEntry
import foo.starred.athen.modules.impl.general.keybinds.data.KeybindCondition
import foo.starred.athen.modules.impl.general.keybinds.data.KeybindWorkIn
import foo.starred.athen.modules.impl.general.keybinds.ui.KeybindsGUI.str
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
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.api.client
import foo.starred.snowbird.utils.literal
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor

class KeybindsPopUp(
    private val gui: CascadeScreen,
    private val onClose: () -> Unit
) : ContainerPrimitive() {
    private var entry: BindingEntry? = null
    private var binding = mutableListOf<Int>()
    private var capturing = false
    private val captured = mutableSetOf<Int>()
    private var condition = KeybindCondition()
    private var category = ""
    private var categories: List<String> = emptyList()

    private val all = listOf(1, 2, 3, 4, 5)

    private var title: TextPrimitive = TextPrimitive.NONE
    private var field: TextFieldComponent
    private var `keys$box`: RectanglePrimitive
    private lateinit var `keys$box$outline`: OutlineEffect
    private var `keys$boxText`: TextPrimitive = TextPrimitive.NONE
    private var `keys$hint`: RectanglePrimitive
    private var `checkbox$category`: MultiCheckboxComponent
    private var `checkbox$workIn`: MultiCheckboxComponent
    private var `checkbox$islands`: MultiCheckboxComponent
    private var `checkbox$floors`: MultiCheckboxComponent
    private var `checkbox$classes`: MultiCheckboxComponent
    private var `checkbox$phases`: MultiCheckboxComponent

    init {
        size = FillSizeConstraint()
        position = FixedPositionConstraint(0, 0)

        on<KeyEvent.Press> {
            if (!capturing) {
                if (key == InputConstants.KEY_ESCAPE) {
                    onClose()
                    cancel()
                }

                return@on
            }

            when {
                key == InputConstants.KEY_RETURN && captured.isNotEmpty() -> {
                    capturing = false
                    unfocus = true
                    binding = captured.toMutableList()
                }

                key == InputConstants.KEY_ESCAPE -> {
                    capturing = false
                    unfocus = true
                    captured.clear()
                }

                key > 0 -> captured.add(key)
            }

            keys()
            cancel()
        }

        on<MouseEvent.Press> {
            if (!capturing) return@on

            captured.add(button)
            keys()
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

            attach(this@KeybindsPopUp)
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

            attach(this@KeybindsPopUp)
        }

        val header = container {
            position = FixedPositionConstraint(0, 0)
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 24))
            attach(box)

            adopt(text {
                text = "Create Keybind".literal()
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

        val command = text {
            text = "Command".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ divider }, PositionAnchor.BELOW, 16, 8)
            attach(box)
        }

        val keys = text {
            text = "Keys".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ divider }, PositionAnchor.BELOW, 194, 8)
            attach(box)
        }

        field = textField {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ command }, PositionAnchor.BELOW, 0, 2)
            placeholder = "Command or message"
            attach(box)
        }

        `keys$box` = rectangle {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ keys }, PositionAnchor.BELOW, 0, 2)
            color = Mocha.Surface0.argb

            effect(OutlineEffect {
                color = Mocha.Overlay0.argb
            }.also { `keys$box$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on

                capturing = true
                this@KeybindsPopUp.unfocus = false
                captured.clear()
                gui.scene.focused = this@KeybindsPopUp
                keys()
            }

            attach(box)
            adopt(text {
                text = "Click to bind".literal()
                color = Mocha.Text.argb
                position = CenterPositionConstraint()
            }.also { `keys$boxText` = it })
        }

        `checkbox$category` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ field }, PositionAnchor.BELOW, 0, 16)
            label = "Category"
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

        `checkbox$workIn` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `keys$box` }, PositionAnchor.BELOW, 0, 16)
            label = "Work In"
            items = KeybindWorkIn.entries.map { it.displayName }

            selected {
                condition.workIn == KeybindWorkIn.entries[it]
            }

            select {
                condition.workIn = KeybindWorkIn.entries[it]
                `checkbox$workIn`.text = condition.workIn.displayName
            }

            attach(box)
        }

        `checkbox$islands` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `checkbox$category` }, PositionAnchor.BELOW, 0, 16)
            label = "Islands"
            items = listOf("Any") + SkyBlockIsland.entries.map { it.displayName }

            selected {
                if (it == 0) condition.islands.isEmpty()
                else condition.islands.contains(SkyBlockIsland.entries[it - 1])
            }

            select {
                if (it == 0) {
                    condition.islands.clear()
                } else {
                    val i = SkyBlockIsland.entries[it - 1]
                    if (condition.islands.contains(i)) condition.islands.remove(i) else condition.islands.add(i)
                }

                `checkbox$islands`.text = if (condition.islands.isEmpty()) "Any" else "${condition.islands.size} Islands"
            }

            attach(box)
        }

        `checkbox$floors` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `checkbox$workIn` }, PositionAnchor.BELOW, 0, 16)
            label = "Dungeon Floors"
            items = listOf("Any") + DungeonFloor.entries.map { it.name }

            selected {
                if (it == 0) condition.floors.isEmpty()
                else condition.floors.contains(DungeonFloor.entries[it - 1])
            }

            select {
                if (it == 0) {
                    condition.floors.clear()
                } else {
                    val f = DungeonFloor.entries[it - 1]
                    if (condition.floors.contains(f)) condition.floors.remove(f) else condition.floors.add(f)
                }

                `checkbox$floors`.text = if (condition.floors.isEmpty()) "Any" else "${condition.floors.size} Floors"
            }

            attach(box)
        }

        `checkbox$classes` = multiCheckbox {
            val all = DungeonClass.entries

            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `checkbox$islands` }, PositionAnchor.BELOW, 0, 16)
            label = "Dungeon Classes"
            items = listOf("Any") + all.map { it.displayName }

            selected {
                if (it == 0) condition.classes.isEmpty()
                else condition.classes.contains(all[it - 1])
            }

            select {
                if (it == 0) {
                    condition.classes.clear()
                } else {
                    val c = all[it - 1]
                    if (condition.classes.contains(c)) condition.classes.remove(c) else condition.classes.add(c)
                }

                `checkbox$classes`.text = if (condition.classes.isEmpty()) "Any" else "${condition.classes.size} Classes"
            }

            attach(box)
        }

        `checkbox$phases` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `checkbox$floors` }, PositionAnchor.BELOW, 0, 16)
            label = "F7 Phases"
            items = listOf("Any") + all.map { "Phase $it" }

            selected {
                if (it == 0) condition.phases.isEmpty()
                else condition.phases.contains(all[it - 1])
            }

            select {
                if (it == 0) {
                    condition.phases.clear()
                } else {
                    val p = all[it - 1]
                    if (condition.phases.contains(p)) condition.phases.remove(p) else condition.phases.add(p)
                }

                `checkbox$phases`.text = if (condition.phases.isEmpty()) "Any" else "${condition.phases.size} Phases"
            }

            attach(box)
        }

        val bottom = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 1))
            position = FixedPositionConstraint(0, 220)
            color = Mocha.Surface0.argb
            attach(box)
        }

        val cancel = rectangle {
            size = FixedSizeConstraint(170, 22)
            position = AnchorPositionConstraint({ bottom }, PositionAnchor.BELOW, 16, 8)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Red.argb
            })

            on<MouseEvent.Press> {
                if (button == 0) onClose()
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
                text = "Cancel".literal()
                color = Mocha.Red.argb
                position = CenterPositionConstraint()
            })
        }

        rectangle {
            size = FixedSizeConstraint(170, 22)
            position = AnchorPositionConstraint({ cancel }, PositionAnchor.RIGHT, 8)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Green.argb
            })

            on<MouseEvent.Press> {
                if (button != 0) return@on cancel()

                val str = field.value.trim()
                if (str.isEmpty()) return@on cancel()
                if (binding.isEmpty()) return@on cancel()

                entry?.index?.update(this@KeybindsPopUp.binding, str, entry!!.binding.enabled, category, condition) ?: binding.add(str, category, condition)

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

        `keys$hint` = rectangle {
            val str = "Press Enter to confirm | Escape to cancel"
            val w = (client.font?.width(str) ?: 200) + 12

            size = FixedSizeConstraint(w, (client.font?.lineHeight ?: 9) + 8)
            position = MixedPositionConstraint(CenterPositionConstraint(), AnchorPositionConstraint({ box }, PositionAnchor.BELOW, 0, 6))
            color = Mocha.Base.argb
            visible = false

            effect(OutlineEffect {
                color = Mocha.Overlay0.argb
            })

            attach(box)
            adopt(text {
                text = str.literal()
                color = Mocha.Text.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 6)
            })
        }
    }

    fun open(entry: BindingEntry?, selectedCategory: String?) {
        this@KeybindsPopUp.entry = entry
        if (entry != null) {
            binding = entry.binding.keys.toMutableList()
            condition = entry.binding.condition.copy()
            category = entry.binding.category
        } else {
            binding = mutableListOf()
            condition = KeybindCondition()
            category = selectedCategory ?: ""
        }

        capturing = false
        captured.clear()

        title.text = (if (entry == null) "Create Keybind" else "Edit Keybind").literal()
        field.reset(true)
        field.value = entry?.binding?.command ?: ""
        field.cursor = field.value.length

        categories = Keybinds.categories.value.map { it.name }
        `checkbox$category`.items = listOf("Uncategorized") + categories

        `checkbox$category`.text = category.ifEmpty { "Uncategorized" }
        `checkbox$workIn`.text = condition.workIn.displayName
        `checkbox$islands`.text = if (condition.islands.isEmpty()) "Any" else "${condition.islands.size} Islands"
        `checkbox$floors`.text = if (condition.floors.isEmpty()) "Any" else "${condition.floors.size} Floors"
        `checkbox$classes`.text = if (condition.classes.isEmpty()) "Any" else "${condition.classes.size} Classes"
        `checkbox$phases`.text = if (condition.phases.isEmpty()) "Any" else "${condition.phases.size} Phases"
        keys()

        gui.scene.focused = this
    }

    private fun keys() {
        `keys$box`.color = if (capturing) Mocha.Peach.withAlpha(0.3f) else Mocha.Surface0.argb
        `keys$box$outline`.color = if (capturing) Mocha.Peach.argb else Mocha.Overlay0.argb
        `keys$hint`.visible = capturing

        `keys$boxText`.text = when {
            capturing -> if (captured.isEmpty()) "Press keys..." else captured.toList().str()
            binding.isEmpty() -> "Click to bind"
            else -> binding.str()
        }.literal()
    }
}
