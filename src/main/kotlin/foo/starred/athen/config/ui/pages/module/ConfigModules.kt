package foo.starred.athen.config.ui.pages.module

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.feature.ConfigFeatureData
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.config.ui.ConfigUI
import foo.starred.athen.config.ui.pages.main.ConfigCategories
import foo.starred.athen.config.ui.pages.main.ConfigInfoPage
import foo.starred.cascade.animation.extension.impl.animate
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.AnchorPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.ImagePrimitive.Companion.image
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.text.parser.impl.parse
import kotlin.time.Duration.Companion.milliseconds

object ConfigModules {
    var active: ConfigFeatureData? = null

    fun fn() {
        for (child in ConfigUI.right.children) child.iterateChildren { it.detach() }

        ConfigUI.hide()

        if (ConfigCategories.active == ConfigCategory.INFO) {
            ConfigUI.modules.visible = false
            ConfigUI.divider.visible = false
            ConfigUI.modules.children.clear()

            ConfigUI.right.visible = true
            ConfigUI.right.position = FixedPositionConstraint(0f, 0f)
            ConfigUI.right.size = FixedSizeConstraint(502f, 310f)
            for (child in ConfigUI.right.children) child.detach()

            return ConfigInfoPage.fn()
        }

        ConfigUI.modules.visible = true
        ConfigUI.divider.visible = true
        ConfigUI.right.visible = true
        ConfigUI.right.position = FixedPositionConstraint(206f, 0f)
        ConfigUI.right.size = FixedSizeConstraint(296f, 310f)

        list()
        settings()
    }

    private fun list() {
        ConfigUI.modules.children.clear()

        val query = ConfigUI.search.value.trim()
        val features = (ConfigManager.features[ConfigCategories.active] ?: return).filter { it.matches(query) }.sortedWith(compareByDescending<ConfigFeatureData> { it.name.startsWith(query, true) }.thenBy { it.name })
        var last: IPrimitiveElement<*>? = null

        for (v in features) {
            val above = last
            val selected = active == v
            var enabled = ConfigManager.get(v.key) as? Boolean ?: (v.default as? Boolean ?: false)

            val row = roundedRectangle {
                position = if (above == null) FixedPositionConstraint(6f, 6f) else AnchorPositionConstraint({ above }, PositionAnchor.BELOW)
                offset = if (above == null) CascadeGeometricOffset.ZERO else CascadeGeometricOffset(0f, 6f)
                size = FixedSizeConstraint(193f, 26f)
                radius = CascadeGeometricRadius(4f)
                color = CascadeGeometricColor(if (selected) MochaColorScheme.Base.alpha(0.70f) else MochaColorScheme.Crust.alpha(0.55f))

                effect(OutlineEffect {
                    color = CascadeGeometricColor(if (selected) MochaColorScheme.Lavender.alpha(0.60f) else MochaColorScheme.Surface1.alpha(0.35f))
                    inset = false
                })

                attach(ConfigUI.modules)

                var name: TextPrimitive
                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = CascadeFonts.sans.truncate(v.name, 11f, 138f).parse()
                    textSize = 11f
                    color = CascadeGeometricColor(if (selected) MochaColorScheme.Lavender.argb else if (enabled) MochaColorScheme.Text.argb else MochaColorScheme.Subtext0.alpha(0.60f))
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(9f, 0f)
                }.also { name = it })

                adopt(roundedRectangle {
                    position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(-8f, 0f)
                    size = FixedSizeConstraint(28f, 15f)
                    radius = CascadeGeometricRadius(3.5f)
                    color = CascadeGeometricColor(if (enabled) MochaColorScheme.Lavender.alpha(0.20f) else MochaColorScheme.Crust.alpha(0.70f))

                    effect(OutlineEffect {
                        color = CascadeGeometricColor(if (enabled) MochaColorScheme.Lavender.alpha(0.75f) else MochaColorScheme.Surface1.alpha(0.35f))
                        inset = false
                    })

                    var label: TextPrimitive
                    adopt(text {
                        wrapper = CascadeTextWrapper
                        text = (if (enabled) "<bold><#B4BEFE>ON" else "<bold><#6C7086>OFF").parse()
                        textSize = 8.5f
                        position = CenterPositionConstraint()
                    }.also { label = it })

                    on<MouseEvent.Press> {
                        if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                        cancel()

                        enabled = !enabled
                        ConfigManager.update(v.key, enabled)

                        if (!selected) {
                            animate(150.milliseconds) {
                                name::color to CascadeGeometricColor(if (enabled) MochaColorScheme.Text.argb else MochaColorScheme.Subtext0.alpha(0.60f))
                            }
                        }

                        animate(150.milliseconds) {
                            ::color to CascadeGeometricColor(if (enabled) MochaColorScheme.Lavender.alpha(0.20f) else MochaColorScheme.Crust.alpha(0.70f))

                            effect<OutlineEffect> {
                                ::color to CascadeGeometricColor(if (enabled) MochaColorScheme.Lavender.alpha(0.75f) else MochaColorScheme.Surface1.alpha(0.35f))
                            }
                        }

                        label.text = (if (enabled) "<bold><#B4BEFE>ON" else "<bold><#6C7086>OFF").parse()
                        if (selected) settings()
                    }

                    on<MouseEvent.Move.Enter> {
                        if (enabled) return@on

                        animate(150.milliseconds) {
                            ::color to CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.80f))

                            effect<OutlineEffect> {
                                ::color to CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.40f))
                            }
                        }
                    }

                    on<MouseEvent.Move.Exit> {
                        if (enabled) return@on

                        animate(150.milliseconds) {
                            ::color to CascadeGeometricColor(MochaColorScheme.Crust.alpha(0.70f))

                            effect<OutlineEffect> {
                                ::color to CascadeGeometricColor(MochaColorScheme.Surface1.alpha(0.35f))
                            }
                        }
                    }
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                    cancel()
                    ConfigModulesNavigationState.navigate(ConfigCategories.active, v.takeUnless { active == it })
                }

                if (selected) {
                    on<MouseEvent.Move.Exit> {
                        ConfigUI.hide()
                    }

                    return@roundedRectangle
                }

                on<MouseEvent.Move.Enter> {
                    if (v.description.isNotEmpty()) {
                        ConfigUI.show(v.description)
                    }

                    animate(150.milliseconds) {
                        ::color to CascadeGeometricColor(MochaColorScheme.Base.alpha(0.60f))
                    }
                }

                on<MouseEvent.Move.Exit> {
                    if (v.description.isNotEmpty()) {
                        ConfigUI.hide()
                    }

                    animate(150.milliseconds) {
                        ::color to CascadeGeometricColor(MochaColorScheme.Crust.alpha(0.55f))
                    }

                    ConfigUI.hide()
                }
            }

            last = row
        }

        if (last == null) return
        container {
            position = AnchorPositionConstraint({ last }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 8f)
            size = FixedSizeConstraint(1f, 1f)
            attach(ConfigUI.modules)
        }
    }

    private fun settings() {
        for (child in ConfigUI.right.children) child.detach()

        val feature = active
        if (feature != null) {
            ConfigModuleSettingsPage.fn(feature)
            return
        }

        container {
            position = CenterPositionConstraint()
            size = FixedSizeConstraint(200f, 80f)
            attach(ConfigUI.right)

            adopt(image {
                location = ResourceAPI.identify("textures/gui/gear.png")
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.25f))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                size = FixedSizeConstraint(24f, 24f)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Select a Module".literal()
                textSize = 11.5f
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.alpha(0.8f))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                offset = CascadeGeometricOffset(0f, 30f)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "Choose a module to configure its settings".literal()
                textSize = 9f
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.alpha(0.6f))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                offset = CascadeGeometricOffset(0f, 46f)
            })
        }
    }

    private fun ConfigFeatureData.matches(query: String): Boolean {
        return query.isEmpty() || name.contains(query, true) || description.contains(query, true) || options.any { it.name.contains(query, true) || it.description?.contains(query, true) == true }
    }
}
