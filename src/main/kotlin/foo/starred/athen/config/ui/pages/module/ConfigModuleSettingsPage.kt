package foo.starred.athen.config.ui.pages.module

import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.base.IConfigElementData
import foo.starred.athen.config.data.feature.ConfigFeatureData
import foo.starred.athen.config.data.impl.*
import foo.starred.athen.config.ui.ConfigUI
import foo.starred.athen.config.ui.pages.module.elements.button.ConfigButtonElement
import foo.starred.athen.config.ui.pages.module.elements.color.ConfigColorPickerElement
import foo.starred.athen.config.ui.pages.module.elements.group.ConfigGroupElement
import foo.starred.athen.config.ui.pages.module.elements.hud.ConfigHUDElement
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputElement
import foo.starred.athen.config.ui.pages.module.elements.keybind.ConfigKeybindElement
import foo.starred.athen.config.ui.pages.module.elements.selector.ConfigMultiSelectorElement
import foo.starred.athen.config.ui.pages.module.elements.selector.ConfigSelectorElement
import foo.starred.athen.config.ui.pages.module.elements.slider.ConfigSliderElement
import foo.starred.athen.config.ui.pages.module.elements.texts.ConfigInformationElement
import foo.starred.athen.config.ui.pages.module.elements.texts.ConfigVariablesElement
import foo.starred.athen.config.ui.pages.module.elements.toggle.ConfigSwitchElement
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.cascade.animation.data.AnimatableColor.Companion.animateColor
import foo.starred.cascade.animation.enums.CascadeAnimations
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.AnchorPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.AnimatedSizeConstraint.Companion.animateSize
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.FlexibleSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.literal
import net.minecraft.client.gui.GuiGraphicsExtractor

object ConfigModuleSettingsPage {
    fun fn(feature: ConfigFeatureData) {
        for (child in ConfigUI.right0.children.filter { it != ConfigUI.right }) child.forEach { it.detach() }
        for (child in ConfigUI.right.children) child.forEach { it.detach() }

        ConfigUI.hide()
        ConfigUI.headerText.text = "<bold><#FDCCDA>A<#FCDDD3>t<#FAEDCB>h<#F0E2D7>e<#E5D8E4>n<#DBCDF0></bold> <gray>I ${feature.name}".parse()

        object : RoundedRectanglePrimitive() {
            override fun draw(graphics: GuiGraphicsExtractor) {
                graphics.nextStratum()
                super.draw(graphics)
            }
        }.apply {
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.END, -12f, -12f)
            size = FixedSizeConstraint(24f, 24f)
            color = Catppuccin.Mocha.Mantle.argb
            radius = CascadeGeometricRadius(4f)

            effect(OutlineEffect {
                color = Catppuccin.Mocha.Surface0.argb
                inset = false
            })

            on<MouseEvent.Move.Enter> {
                animateColor(Catppuccin.Mocha.Surface0.argb, 0.15f)
            }

            on<MouseEvent.Move.Exit> {
                animateColor(Catppuccin.Mocha.Mantle.argb, 0.15f)
            }

            on<MouseEvent.Press> {
                if (button != 0) return@on
                cancel()

                ConfigModules.active = null
                ConfigModules.fn()
            }

            attach(ConfigUI.right0)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "×".literal()
                textSize = 12f
                color = Catppuccin.Mocha.Subtext0.argb
                position = CenterPositionConstraint()
            })
        }

        val blocks = buildList {
            val list = mutableListOf<IConfigElementData>()

            for (a in feature.options.filter { it.parent == null }) {
                if (a !is ConfigGroupElementData) {
                    list.add(a)
                    continue
                }

                if (list.isNotEmpty()) add(list.toList())
                list.clear()
                add(listOf(a) + feature.options.filter { it.parent == a.key })
            }

            if (list.isNotEmpty()) add(list)
        }

        var last: IPrimitiveElement<*>? = null
        for (block in blocks) {
            if (block.isEmpty()) continue
            last = card(block, last)
        }

        if (last == null) return
        container {
            position = AnchorPositionConstraint({ last }, PositionAnchor.BELOW, 0f, 14f)
            size = FixedSizeConstraint(1f, 1f)
            attach(ConfigUI.right)
        }
    }

    private fun card(block: List<IConfigElementData>, above: IPrimitiveElement<*>?): IPrimitiveElement<*> {
        val first = block.first()
        return roundedRectangle {
            position = if (above == null) FixedPositionConstraint(14f, 14f) else AnchorPositionConstraint({ above }, PositionAnchor.BELOW, 0f, 14f)
            size = MixedSizeConstraint(FixedSizeConstraint(482f, 0f), FlexibleSizeConstraint(0f))
            color = Catppuccin.Mocha.Mantle.argb
            radius = CascadeGeometricRadius(6f)

            effect(OutlineEffect {
                color = Catppuccin.Mocha.Surface0.argb
                inset = false
            })

            attach(ConfigUI.right)

            if (first !is ConfigGroupElementData) {
                rows(this, split(block))
                return@roundedRectangle
            }

            val inner = split(block.subList(1, block.size))
            val full = inner.sumOf { if (it.size == 1 && it[0] is ConfigInformationElementData && it == inner.last()) 30 else 26 }.toFloat()
            val start = if (ConfigManager.get(first.key) as? Boolean ?: !first.collapsed) full else 0f

            val header = container {
                position = FixedPositionConstraint(0f, 0f)
                size = FixedSizeConstraint(482f, 26f)
                attach(this@roundedRectangle)

                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = first.name.parse()
                    textSize = 10f
                    color = Catppuccin.Mocha.Text.argb
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 10f, 0f)
                })

                val desc = first.description
                if (desc.isNullOrEmpty()) return@container

                on<MouseEvent.Move.Any> {
                    if (!hovered) return@on
                    ConfigUI.show(desc, x, y)
                }

                on<MouseEvent.Move.Exit> {
                    ConfigUI.hide()
                }
            }

            val content = object : ContainerPrimitive() {
                var bool = true

                override fun render(graphics: GuiGraphicsExtractor) {
                    val v = height > 0f
                    if (bool != v) {
                        bool = v
                        for (child in children) child.visible = v
                    }

                    if (!v) return
                    graphics.scissor(x, y, width, height) {
                        super.render(graphics)
                    }
                }
            }.apply {
                position = AnchorPositionConstraint({ header }, PositionAnchor.BELOW, 0f, 0f)
                size = FixedSizeConstraint(482f, start)
                attach(this@roundedRectangle)
            }

            ConfigGroupElement.of(header, first) {
                content.animateSize(482f, if (it) full else 0f, 0.25f, CascadeAnimations.EASE_OUT)
                if (!it) content.forEach { a -> (a as? ConfigColorPickerElement)?.close() }
            }

            rows(content, inner)
        }
    }

    private fun split(list: List<IConfigElementData>): List<List<IConfigElementData>> {
        return buildList {
            val pair = mutableListOf<IConfigElementData>()

            for (item in list) {
                if (item !is ConfigInformationElementData) {
                    pair += item
                    if (pair.size < 2) continue

                    add(pair.toList())
                    pair.clear()
                    continue
                }

                if (pair.isNotEmpty()) {
                    add(pair.toList())
                    pair.clear()
                }

                add(listOf(item))
            }

            if (pair.isNotEmpty()) add(pair)
        }
    }

    private fun rows(parent: IPrimitiveElement<*>, list: List<List<IConfigElementData>>): IPrimitiveElement<*>? {
        var prev: IPrimitiveElement<*>? = null

        for (i in list) {
            val above = prev
            val bool = i.size == 1 && i[0] is ConfigInformationElementData

            prev = container {
                position = if (above == null) FixedPositionConstraint(0f, if (bool) 4f else 0f) else AnchorPositionConstraint({ above }, PositionAnchor.BELOW, 0f, 0f)
                size = FixedSizeConstraint(482f, if (bool && i == list.last()) 30f else 26f)
                attach(parent)

                for ((i, v) in i.withIndex()) cell(this, v, i * 241f)
            }
        }

        return prev
    }

    private fun cell(parent: IPrimitiveElement<*>, config: IConfigElementData, x0: Float) {
        if (config is ConfigInformationElementData) {
            ConfigInformationElement.of(parent, config)
            return
        }

        container {
            position = FixedPositionConstraint(x0, 0f)
            size = FixedSizeConstraint(241f, 26f)
            attach(parent)

            adopt(text {
                wrapper = CascadeTextWrapper
                text = config.name.parse()
                textSize = 10f
                color = Catppuccin.Mocha.Text.argb
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER, 10f, 0f)
            })

            val content = container {
                position = FixedPositionConstraint(0f, 0f)
                size = FixedSizeConstraint(241f, 26f)
            }.also { adopt(it) }

            when (config) {
                is ConfigSliderElementData -> ConfigSliderElement.of(content, config)
                is ConfigSwitchElementData -> ConfigSwitchElement.of(content, config)
                is ConfigButtonElementData -> ConfigButtonElement.of(content, config)
                is ConfigTextInputElementData -> ConfigInputElement.of(content, config)
                is ConfigKeybindElementData -> ConfigKeybindElement.of(content, config)
                is ConfigSelectorElementData -> ConfigSelectorElement.of(content, config)
                is ConfigMultiSelectorElementData -> ConfigMultiSelectorElement.of(content, config)
                is ConfigVariablesElementData -> ConfigVariablesElement.of(content, config)
                is ConfigColorPickerElementData -> ConfigColorPickerElement.of(content, config)
                is ConfigHudElementData -> ConfigHUDElement.of(content, config)
                else -> {}
            }

            val text0 = config.description.takeIf { !it.isNullOrEmpty() } ?: return@container
            on<MouseEvent.Move.Any> {
                if (!hovered) return@on
                ConfigUI.show(text0, x, y)
            }

            on<MouseEvent.Move.Exit> {
                ConfigUI.hide()
            }
        }
    }
}
