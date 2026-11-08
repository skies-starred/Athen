package foo.starred.athen.config.ui.pages.module

import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.base.IConfigElementData
import foo.starred.athen.config.data.feature.ConfigFeatureData
import foo.starred.athen.config.data.impl.*
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
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
import foo.starred.cascade.animation.extension.impl.animate
import foo.starred.cascade.animation.interpolator.easing.impl.EaseOutEasingInterpolator
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.AnchorPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.FlexibleSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.text.parser.impl.parse
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.time.Duration.Companion.milliseconds

object ConfigModuleSettingsPage {
    fun fn(feature: ConfigFeatureData) {
        for (child in ConfigUI.right.children) child.detach()

        ConfigUI.hide()
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

        if (blocks.isEmpty()) {
            last = roundedRectangle {
                position = FixedPositionConstraint(8f, 6f)
                size = FixedSizeConstraint(280f, 36f)
                color = CascadeGeometricColor(MochaColorScheme.Crust.alpha(0.50f))
                radius = CascadeGeometricRadius(5f)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface1.alpha(0.35f))
                    inset = false
                })

                attach(ConfigUI.right)
                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = "No additional options for this module".literal()
                    textSize = 9.5f
                    color = CascadeGeometricColor(MochaColorScheme.Subtext0.alpha(0.6f))
                    position = CenterPositionConstraint()
                })
            }
        }

        if (last == null) return
        container {
            position = AnchorPositionConstraint({ last }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 10f)
            size = FixedSizeConstraint(1f, 1f)
            attach(ConfigUI.right)
        }
    }

    private fun card(block: List<IConfigElementData>, above: IPrimitiveElement<*>?): IPrimitiveElement<*> {
        val first = block.first()
        return roundedRectangle {
            position = if (above == null) FixedPositionConstraint(8f, 6f) else AnchorPositionConstraint({ above }, PositionAnchor.BELOW)
            offset = if (above == null) CascadeGeometricOffset.ZERO else CascadeGeometricOffset(0f, 6f)
            size = MixedSizeConstraint(FixedSizeConstraint(280f, 0f), FlexibleSizeConstraint(0f))
            color = CascadeGeometricColor(MochaColorScheme.Crust.alpha(0.60f))
            radius = CascadeGeometricRadius(5f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface1.alpha(0.40f))
                inset = false
            })

            attach(ConfigUI.right)

            if (first !is ConfigGroupElementData) {
                rows(this, block)
                return@roundedRectangle
            }

            val inner = block.subList(1, block.size)
            val full = inner.sumOf { if (it is ConfigInformationElementData && it == inner.last()) 30 else 26 }.toFloat()
            val start = if (ConfigManager.get(first.key) as? Boolean ?: !first.collapsed) full else 0f

            val header = container {
                position = FixedPositionConstraint(0f, 0f)
                size = FixedSizeConstraint(280f, 26f)
                attach(this@roundedRectangle)

                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = first.name.parse()
                    textSize = 10f
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(10f, 0f)
                })
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
                position = AnchorPositionConstraint({ header }, PositionAnchor.BELOW)
                size = FixedSizeConstraint(280f, start)
                attach(this@roundedRectangle)
            }

            ConfigGroupElement.of(header, first) {
                content.animate(250.milliseconds, EaseOutEasingInterpolator) {
                    size<FixedSizeConstraint> {
                        ::height to (if (it) full else 0f)
                    }
                }

                if (it) return@of
                content.iterateChildren { child ->
                    (child as? ConfigColorPickerElement)?.close()
                }
            }

            rows(content, inner)
        }
    }

    private fun rows(parent: IPrimitiveElement<*>, list: List<IConfigElementData>): IPrimitiveElement<*>? {
        var prev: IPrimitiveElement<*>? = null

        for (i in list) {
            val above = prev
            val bool = i is ConfigInformationElementData

            prev = container {
                position = if (above == null) FixedPositionConstraint(0f, if (bool) 4f else 0f) else AnchorPositionConstraint({ above }, PositionAnchor.BELOW)
                size = FixedSizeConstraint(280f, if (bool && i == list.last()) 30f else 26f)
                attach(parent)

                cell(this, i)
            }
        }

        return prev
    }

    private fun cell(parent: IPrimitiveElement<*>, config: IConfigElementData) {
        val bool = config is ConfigInformationElementData

        container {
            position = FixedPositionConstraint(0f, 0f)
            size = FixedSizeConstraint(280f, 26f)
            attach(parent)

            if (!bool) {
                val description = config.description?.takeIf { it.isNotEmpty() }
                val truncated = CascadeFonts.sans.truncate(config.name, 10f, if (description != null) 116f else 130f)
                val width0 = CascadeFonts.sans.width(truncated, 10f)

                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = truncated.parse()
                    textSize = 10f
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(10f, 0f)
                })

                if (description != null) {
                    adopt(badge(description, 10f + width0 + 4f))
                }
            }

            when (config) {
                is ConfigSliderElementData -> ConfigSliderElement.of(this, config)
                is ConfigSwitchElementData -> ConfigSwitchElement.of(this, config)
                is ConfigButtonElementData -> ConfigButtonElement.of(this, config)
                is ConfigTextInputElementData -> ConfigInputElement.of(this, config)
                is ConfigKeybindElementData -> ConfigKeybindElement.of(this, config)
                is ConfigSelectorElementData -> ConfigSelectorElement.of(this, config)
                is ConfigMultiSelectorElementData -> ConfigMultiSelectorElement.of(this, config)
                is ConfigVariablesElementData -> ConfigVariablesElement.of(this, config)
                is ConfigColorPickerElementData -> ConfigColorPickerElement.of(this, config)
                is ConfigHudElementData -> ConfigHUDElement.of(this, config)
                is ConfigInformationElementData -> ConfigInformationElement.of(this, config)
                else -> {}
            }
        }
    }

    private fun badge(string: String, x0: Float): RoundedRectanglePrimitive {
        return roundedRectangle {
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(x0, 0f)
            size = FixedSizeConstraint(12f, 12f)
            radius = CascadeGeometricRadius(6f)
            color = CascadeGeometricColor.TRANSPARENT

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface1.alpha(0.50f))
                inset = false
            })

            var label: TextPrimitive
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "?".parse()
                textSize = 9f
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = CenterPositionConstraint()
            }.also { label = it })

            on<MouseEvent.Move.Enter> {
                ConfigUI.show(string)

                animate(120.milliseconds) {
                    effect<OutlineEffect> {
                        ::color to CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.80f))
                    }
                }

                animate(120.milliseconds) {
                    label::color to CascadeGeometricColor(MochaColorScheme.Lavender.argb)
                }
            }

            on<MouseEvent.Move.Exit> {
                ConfigUI.hide()

                animate(120.milliseconds) {
                    effect<OutlineEffect> {
                        ::color to CascadeGeometricColor(MochaColorScheme.Surface1.alpha(0.50f))
                    }
                }

                animate(120.milliseconds) {
                    label::color to CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                }

                ConfigUI.hide()
            }
        }
    }
}
