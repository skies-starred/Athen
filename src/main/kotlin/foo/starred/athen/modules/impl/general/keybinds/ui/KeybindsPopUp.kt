@file:Suppress("PrivatePropertyName")

package foo.starred.athen.modules.impl.general.keybinds.ui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.dungeon.data.DungeonClass
import foo.starred.athen.api.dungeon.data.DungeonFloor
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.rendering.ui.components.impl.MultiCheckboxComponent
import foo.starred.athen.api.rendering.ui.components.impl.MultiCheckboxComponent.Companion.multiCheckbox
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.general.keybinds.Keybinds
import foo.starred.athen.modules.impl.general.keybinds.Keybinds.add
import foo.starred.athen.modules.impl.general.keybinds.Keybinds.update
import foo.starred.athen.modules.impl.general.keybinds.data.BindingEntry
import foo.starred.athen.modules.impl.general.keybinds.data.KeybindCondition
import foo.starred.athen.modules.impl.general.keybinds.data.KeybindWorkIn
import foo.starred.athen.modules.impl.general.keybinds.ui.KeybindsGUI.string
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
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.abstraction.input.impl.MouseInputState
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.api.format.extension.component.literal

class KeybindsPopUp(
    private val gui: CascadeScreen,
    private val onClose: () -> Unit
) : ContainerPrimitive() {
    private var entry: BindingEntry? = null
    private var binding = mutableListOf<InputConstants.Key>()
    private var capturing = false
    private val captured = mutableSetOf<InputConstants.Key>()
    private var condition = KeybindCondition()
    private var category = ""
    private var categories: List<String> = emptyList()

    private val all = listOf(1, 2, 3, 4, 5)

    private var title: TextPrimitive = TextPrimitive.NONE
    private var field: TextFieldComponent
    private var `keys$box`: RectanglePrimitive
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

                key > 0 -> captured.add(KeyboardInputState.vanilla(key))
            }

            keys()
            cancel()
        }

        on<MouseEvent.Press> {
            if (!capturing) return@on

            captured.add(MouseInputState.vanilla(button))
            keys()
            cancel()
        }

        rectangle {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            color = CascadeGeometricColor(MochaColorScheme.Crust.alpha(0.6f))

            on<MouseEvent.Press> {
                if (root.focused is MultiCheckboxComponent) root.focused = null
                cancel()
            }

            attach(this@KeybindsPopUp)
        }

        val box = rectangle {
            size = FixedSizeConstraint(380, 260)
            position = CenterPositionConstraint()
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
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
                color = CascadeGeometricColor(MochaColorScheme.Lavender.argb)
                position = MixedPositionConstraint(FixedPositionConstraint(8, 0), CenterPositionConstraint())
            }.also { title = it })
        }

        val divider = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 1))
            position = AnchorPositionConstraint({ header }, PositionAnchor.BELOW)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            attach(box)
        }

        val command = text {
            text = "Command".literal()
            color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
            position = AnchorPositionConstraint({ divider }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(16f, 8f)
            attach(box)
        }

        val keys = text {
            text = "Keys".literal()
            color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
            position = AnchorPositionConstraint({ divider }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(194f, 8f)
            attach(box)
        }

        field = textField {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ command }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 2f)
            placeholder = "Command or message"
            attach(box)
        }

        `keys$box` = rectangle {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ keys }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 2f)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                capturing = true
                this@KeybindsPopUp.unfocus = false
                captured.clear()
                gui.scene.focused = this@KeybindsPopUp
                keys()
                cancel()
            }

            attach(box)
            adopt(text {
                text = "Click to bind".literal()
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = CenterPositionConstraint()
            }.also { `keys$boxText` = it })
        }

        `checkbox$category` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ field }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 16f)
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
            position = AnchorPositionConstraint({ `keys$box` }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 16f)
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
            position = AnchorPositionConstraint({ `checkbox$category` }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 16f)
            label = "Islands"
            items = listOf("Any") + PresetSkyBlockIsland.entries.map { it.string }

            selected {
                if (it == 0) condition.islands.isEmpty()
                else condition.islands.contains(PresetSkyBlockIsland.entries[it - 1])
            }

            select {
                if (it == 0) {
                    condition.islands.clear()
                } else {
                    val i = PresetSkyBlockIsland.entries[it - 1]
                    if (condition.islands.contains(i)) condition.islands.remove(i) else condition.islands.add(i)
                }

                `checkbox$islands`.text = if (condition.islands.isEmpty()) "Any" else "${condition.islands.size} Islands"
            }

            attach(box)
        }

        `checkbox$floors` = multiCheckbox {
            size = FixedSizeConstraint(170, 16)
            position = AnchorPositionConstraint({ `checkbox$workIn` }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 16f)
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
            position = AnchorPositionConstraint({ `checkbox$islands` }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 16f)
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
            position = AnchorPositionConstraint({ `checkbox$floors` }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 16f)
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
            color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            attach(box)
        }

        val cancel = rectangle {
            size = FixedSizeConstraint(170, 22)
            position = AnchorPositionConstraint({ bottom }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(16f, 8f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Red.argb)
            })

            on<MouseEvent.Press> {
                if (button == InputConstants.MOUSE_BUTTON_LEFT) onClose()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            }

            attach(box)
            adopt(text {
                text = "Cancel".literal()
                color = CascadeGeometricColor(MochaColorScheme.Red.argb)
                position = CenterPositionConstraint()
            })
        }

        rectangle {
            size = FixedSizeConstraint(170, 22)
            position = AnchorPositionConstraint({ cancel }, PositionAnchor.RIGHT)
            offset = CascadeGeometricOffset(8f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on cancel()

                val str = field.value.trim()
                if (str.isEmpty()) return@on cancel()
                if (binding.isEmpty()) return@on cancel()

                entry?.index?.update(this@KeybindsPopUp.binding, str, entry!!.binding.enabled, category, condition) ?: binding.add(str, category, condition)

                onClose()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
            }

            attach(box)
            adopt(text {
                text = "Save".literal()
                color = CascadeGeometricColor(MochaColorScheme.Green.argb)
                position = CenterPositionConstraint()
            })
        }

        `keys$hint` = rectangle {
            val str = "Press Enter to confirm | Escape to cancel"
            val w = MinecraftFontMeasurer.width(str) + 12

            size = FixedSizeConstraint(w, MinecraftFontMeasurer.height + 8)
            position = MixedPositionConstraint(CenterPositionConstraint(), AnchorPositionConstraint({ box }, PositionAnchor.BELOW))
            offset = CascadeGeometricOffset(0f, 6f)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            visible = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
            })

            attach(box)
            adopt(text {
                text = str.literal()
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(6f, 0f)
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
        field.block = field.value.length

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
        `keys$box`.color = CascadeGeometricColor(if (capturing) MochaColorScheme.Peach.alpha(0.3f) else MochaColorScheme.Surface0.argb)
        `keys$box`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (capturing) MochaColorScheme.Peach.argb else MochaColorScheme.Overlay0.argb)
        `keys$hint`.visible = capturing

        `keys$boxText`.text = when {
            capturing -> if (captured.isEmpty()) "Press keys..." else captured.toList().string()
            binding.isEmpty() -> "Click to bind"
            else -> binding.string()
        }.literal()
    }
}
