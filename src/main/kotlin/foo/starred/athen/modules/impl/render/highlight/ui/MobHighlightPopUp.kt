@file:Suppress("PrivatePropertyName", "LocalVariableName")

package foo.starred.athen.modules.impl.render.highlight.ui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent
import foo.starred.athen.api.rendering.ui.components.impl.TextFieldComponent.Companion.textField
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
import foo.starred.cascade.events.impl.KeyEvent
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.utils.literal
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType

class MobHighlightPopUp(
    private val gui: CascadeScreen,
    private val onClose: () -> Unit
) : ContainerPrimitive() {
    private var index: Int? = null
    private var typed = false

    private var title: TextPrimitive = TextPrimitive.NONE
    private var `target$label`: TextPrimitive = TextPrimitive.NONE
    private var `target$field`: TextFieldComponent
    private var `color$preview`: RectanglePrimitive
    private var `color$field`: TextFieldComponent
    private var `maxHp$field`: TextFieldComponent
    private var `tab$named`: RectanglePrimitive
    private var `tab$typed`: RectanglePrimitive
    private lateinit var `tab$named$outline`: OutlineEffect
    private lateinit var `tab$typed$outline`: OutlineEffect
    private lateinit var `tab$named$text`: TextPrimitive
    private lateinit var `tab$typed$text`: TextPrimitive

    init {
        size = FillSizeConstraint()
        position = FixedPositionConstraint(0, 0)

        on<KeyEvent.Press> {
            if (key == InputConstants.KEY_ESCAPE) onClose() else if (key == InputConstants.KEY_RETURN) save() else return@on
            cancel()
        }

        rectangle {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            color = Mocha.Crust.withAlpha(0.6f)

            on<MouseEvent.Press> {
                cancel()
            }

            attach(this@MobHighlightPopUp)
        }

        val box = rectangle {
            size = FixedSizeConstraint(300, 198)
            position = CenterPositionConstraint()
            color = Mocha.Base.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            on<MouseEvent.Press> {
                cancel()
            }

            attach(this@MobHighlightPopUp)
        }

        val header = container {
            position = FixedPositionConstraint(0, 0)
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 24))
            attach(box)

            adopt(text {
                text = "Add Highlight".literal()
                color = Mocha.Lavender.argb
                position = MixedPositionConstraint(FixedPositionConstraint(8, 0), CenterPositionConstraint())
            }.also { title = it })
        }

        val divider0 = rectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 1))
            position = AnchorPositionConstraint({ header }, PositionAnchor.BELOW)
            color = Mocha.Surface0.argb
            attach(box)
        }

        val tabs = container {
            size = FixedSizeConstraint(284, 18)
            position = AnchorPositionConstraint({ divider0 }, PositionAnchor.BELOW, 8, 8)
            attach(box)
        }

        `tab$named` = rectangle {
            size = FixedSizeConstraint(140, 18)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 0)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Lavender.argb
            }.also { `tab$named$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                if (!typed) return@on
                typed = false
                tabs()
            }

            attach(tabs)
            adopt(text {
                text = "Named".literal()
                color = Mocha.Lavender.argb
                position = CenterPositionConstraint()
            }.also { `tab$named$text` = it })
        }

        `tab$typed` = rectangle {
            size = FixedSizeConstraint(140, 18)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, 0)
            color = Mocha.Surface0.argb

            effect(OutlineEffect {
                color = Mocha.Overlay0.argb
            }.also { `tab$typed$outline` = it })

            on<MouseEvent.Press> {
                cancel()
                if (button != 0) return@on
                if (typed) return@on
                typed = true
                tabs()
            }

            attach(tabs)
            adopt(text {
                text = "Typed".literal()
                color = Mocha.Subtext0.argb
                position = CenterPositionConstraint()
            }.also { `tab$typed$text` = it })
        }

        val `target$label` = text {
            text = "Name".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ tabs }, PositionAnchor.BELOW, 0, 8)
            attach(box)
        }.also { this@MobHighlightPopUp.`target$label` = it }

        `target$field` = textField {
            size = FixedSizeConstraint(284, 16)
            position = AnchorPositionConstraint({ `target$label` }, PositionAnchor.BELOW, 0, 3)
            placeholder = "Entity Name (e.g. Lost Adventurer)"
            attach(box)
        }

        val `color$label` = text {
            text = "Color (Hex)".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ `target$field` }, PositionAnchor.BELOW, 0, 8)
            attach(box)
        }

        val `color$row` = container {
            size = FixedSizeConstraint(284, 16)
            position = AnchorPositionConstraint({ `color$label` }, PositionAnchor.BELOW, 0, 3)
            attach(box)
        }

        `color$preview` = rectangle {
            size = FixedSizeConstraint(16, 16)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 0)
            color = 0xFFFF0000.toInt()
            interact = false

            effect(OutlineEffect {
                color = Mocha.Surface2.argb
            })

            attach(`color$row`)
        }

        `color$field` = textField {
            size = FixedSizeConstraint(264, 16)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, 0)
            placeholder = "ff0000"
            attach(`color$row`)

            on<KeyEvent.Type> {
                color()
            }

            on<KeyEvent.Press> {
                color()
            }
        }

        val maxHpLabel = text {
            text = "Filter Max HP (-1 for any)".literal()
            color = Mocha.Subtext0.argb
            position = AnchorPositionConstraint({ `color$row` }, PositionAnchor.BELOW, 0, 8)
            attach(box)
        }

        `maxHp$field` = textField {
            size = FixedSizeConstraint(284, 16)
            position = AnchorPositionConstraint({ maxHpLabel }, PositionAnchor.BELOW, 0, 3)
            placeholder = "-1"
            attach(box)
        }

        val footer = container {
            size = FixedSizeConstraint(284, 20)
            position = AnchorPositionConstraint({ `maxHp$field` }, PositionAnchor.BELOW, 0, 10)
            attach(box)
        }

        rectangle {
            size = FixedSizeConstraint(138, 20)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 0)
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

            attach(footer)
            adopt(text {
                text = "Cancel".literal()
                color = Mocha.Red.argb
                position = CenterPositionConstraint()
            })
        }

        rectangle {
            size = FixedSizeConstraint(138, 20)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, 0)
            color = Mocha.Surface1.argb

            effect(OutlineEffect {
                color = Mocha.Green.argb
            })

            on<MouseEvent.Press> {
                if (button != 0) return@on

                cancel()
                save()
            }

            on<MouseEvent.Move.Enter> {
                color = Mocha.Surface2.argb
            }

            on<MouseEvent.Move.Exit> {
                color = Mocha.Surface1.argb
            }

            attach(footer)
            adopt(text {
                text = "Save".literal()
                color = Mocha.Green.argb
                position = CenterPositionConstraint()
            })
        }
    }

    fun open(typed: Boolean = false, index: Int? = null) {
        this.index = index
        this.typed = typed
        title.text = (if (index == null) "Add Highlight" else "Edit Highlight").literal()
        tabs()

        val entry = index?.let { if (typed) MobHighlight.e1.value.getOrNull(it) else MobHighlight.e0.value.getOrNull(it) }
        `target$field`.reset(true)
        `color$field`.reset(true)
        `maxHp$field`.reset(true)

        if (entry == null) {
            `color$field`.value = "ff0000"
            `color$field`.cursor = 6
            `maxHp$field`.value = "-1"
            `maxHp$field`.cursor = 2
        } else {
            `target$field`.value = if (typed) BuiltInRegistries.ENTITY_TYPE.getKey((entry as MobHighlight.EntityTyped).type).toString() else (entry as MobHighlight.EntityNamed).name
            `color$field`.value = "%06x".format(entry.color and 0xFFFFFF)
            `maxHp$field`.value = entry.max.toString()
            `target$field`.cursor = `target$field`.value.length
            `color$field`.cursor = `color$field`.value.length
            `maxHp$field`.cursor = `maxHp$field`.value.length
        }

        color()
        visible = true
        gui.scene.focused = `target$field`
    }

    fun open(name: String?, type: EntityType<*>?, max: Int = -1) {
        index = null
        typed = name == null && type != null

        title.text = "Add Highlight".literal()
        tabs()

        `target$field`.reset(true)
        `target$field`.value = if (typed && type != null) BuiltInRegistries.ENTITY_TYPE.getKey(type).toString() else name ?: ""
        `target$field`.cursor = `target$field`.value.length

        `color$field`.reset(true)
        `color$field`.value = "ff0000"
        `color$field`.cursor = 6

        `maxHp$field`.reset(true)
        `maxHp$field`.value = max.toString()
        `maxHp$field`.cursor = `maxHp$field`.value.length

        color()
        visible = true
        gui.scene.focused = `color$field`
    }

    private fun tabs() {
        `tab$named`.color = if (!typed) Mocha.Surface1.argb else Mocha.Surface0.argb
        `tab$named$outline`.color = if (!typed) Mocha.Lavender.argb else Mocha.Overlay0.argb
        `tab$named$text`.color = if (!typed) Mocha.Lavender.argb else Mocha.Subtext0.argb

        `tab$typed`.color = if (typed) Mocha.Surface1.argb else Mocha.Surface0.argb
        `tab$typed$outline`.color = if (typed) Mocha.Lavender.argb else Mocha.Overlay0.argb
        `tab$typed$text`.color = if (typed) Mocha.Lavender.argb else Mocha.Subtext0.argb

        `target$label`.text = (if (typed) "Entity ID" else "Name").literal()
        `target$field`.placeholder = if (typed) "Entity Type (e.g. minecraft:zombie)" else "Entity Name (e.g. Lost Adventurer)"
    }

    private fun color() {
        val hex = `color$field`.value.trim().removePrefix("#")
        val color = hex.toIntOrNull(16) ?: 0xFF0000
        `color$preview`.color = color or 0xFF000000.toInt()
    }

    private fun save() {
        val target = `target$field`.value.trim()
        if (target.isEmpty()) return

        val hex = `color$field`.value.trim().removePrefix("#")
        val color = hex.toIntOrNull(16) ?: return
        val max = `maxHp$field`.value.trim().toIntOrNull() ?: -1

        if (typed) {
            //~ if >= 26.2 'EntityType.byString(target)' -> 'BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(target))'
            val type = EntityType.byString(target).orElse(null) ?: return
            MobHighlight.e1.update { if (index == null) add(MobHighlight.EntityTyped(type, color, max)) else set(index!!, MobHighlight.EntityTyped(type, color, max)) }
            MobHighlight.json.save()
            onClose()
            return
        }

        MobHighlight.e0.update { if (index == null) add(MobHighlight.EntityNamed(target, color, max)) else set(index!!, MobHighlight.EntityNamed(target, color, max)) }
        MobHighlight.json.save()
        onClose()
    }
}
