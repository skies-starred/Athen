package foo.starred.athen.config.ui.pages.module.elements.texts

import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.data.impl.ConfigVariablesElementData
import foo.starred.athen.config.ui.ConfigUI
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.cascade.animation.data.AnimatableColor.Companion.animateColor
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.ImagePrimitive
import foo.starred.cascade.primitives.impl.ImagePrimitive.Companion.image
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.api.client
import foo.starred.snowbird.utils.literal

class ConfigVariablesElement(
    private val config: ConfigVariablesElementData
) : RoundedRectanglePrimitive() {
    private val labels = mutableListOf<TextPrimitive>()
    private val slot0 = mutableListOf<ContainerPrimitive>()
    private val slot1 = mutableListOf<RectanglePrimitive>()
    private val total: Int = maxOf(1, (config.tokens.size + 2) / 3)

    private var page: Int = 0
    private var copied: String? = null

    private var chevron0: ImagePrimitive? = null
    private var chevron1: ImagePrimitive? = null

    init {
        position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER, -8f, 0f)
        size = FixedSizeConstraint(if (total > 1) 140f else 114f, 14f)
        radius = CascadeGeometricRadius(4f)
        color = Catppuccin.Mocha.Surface0.argb

        effect(OutlineEffect {
            color = Catppuccin.Mocha.Surface1.argb
            inset = false
        })

        for (i in 0..2) {
            adopt(container {
                position = FixedPositionConstraint(i * 38f, 0f)
                size = FixedSizeConstraint(38f, 14f)

                adopt(text {
                    wrapper = CascadeTextWrapper
                    textSize = 8f
                    color = Catppuccin.Mocha.Lavender.argb
                    position = CenterPositionConstraint()
                }.also { labels.add(it) })

                on<MouseEvent.Press> {
                    if (button != 0) return@on
                    cancel()

                    val token = config.tokens.getOrNull(page * 3 + i) ?: return@on
                    copied = token
                    client.keyboardHandler.clipboard = token
                    ConfigUI.show("<#A6E3A1>Copied $token to clipboard!", x, y)
                }

                on<MouseEvent.Move.Any> {
                    if (!hovered) return@on

                    val token = config.tokens.getOrNull(page * 3 + i) ?: return@on
                    val text = if (copied == token) "<#A6E3A1>Copied $token to clipboard!" else "Click to copy <#CBA6F7>$token"
                    ConfigUI.show(text, x, y)
                }

                on<MouseEvent.Move.Exit> {
                    copied = null
                    ConfigUI.hide()
                }
            }.also { slot0.add(it) })

            if (i >= 2) continue
            adopt(rectangle {
                position = FixedPositionConstraint((i + 1) * 38f, 3f)
                size = FixedSizeConstraint(1f, 8f)
                color = Catppuccin.Mocha.Surface2.argb
                interact = false
            }.also { slot1.add(it) })
        }

        if (total > 1) {
            adopt(rectangle {
                position = FixedPositionConstraint(114f, 3f)
                size = FixedSizeConstraint(1f, 8f)
                color = Catppuccin.Mocha.Surface2.argb
                interact = false
            })

            adopt(container {
                position = FixedPositionConstraint(114f, 0f)
                size = FixedSizeConstraint(13f, 14f)

                adopt(image {
                    location = ResourceAPI.identify("textures/gui/chevron.png")
                    color = Catppuccin.Mocha.Subtext0.argb
                    position = CenterPositionConstraint()
                    size = FixedSizeConstraint(5f, 5f)
                    interact = false
                }.also { chevron0 = it })

                on<MouseEvent.Press> {
                    if (button != 0) return@on
                    if (page <= 0) return@on

                    page--
                    copied = null
                    ConfigUI.hide()
                    update()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    if (page <= 0) return@on
                    chevron0?.animateColor(Catppuccin.Mocha.Text.argb, 0.1f)
                }

                on<MouseEvent.Move.Exit> {
                    update()
                }
            })

            adopt(rectangle {
                position = FixedPositionConstraint(127f, 3f)
                size = FixedSizeConstraint(1f, 8f)
                color = Catppuccin.Mocha.Surface2.argb
                interact = false
            })

            adopt(container {
                position = FixedPositionConstraint(127f, 0f)
                size = FixedSizeConstraint(13f, 14f)

                adopt(image {
                    location = ResourceAPI.identify("textures/gui/chevron.png")
                    rotation = 180f
                    color = Catppuccin.Mocha.Subtext0.argb
                    position = CenterPositionConstraint()
                    size = FixedSizeConstraint(5f, 5f)
                    interact = false
                }.also { chevron1 = it })

                on<MouseEvent.Press> {
                    if (button != 0) return@on
                    if (page >= total - 1) return@on

                    page++
                    copied = null
                    ConfigUI.hide()
                    update()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    if (page >= total - 1) return@on
                    chevron1?.animateColor(Catppuccin.Mocha.Text.argb, 0.1f)
                }

                on<MouseEvent.Move.Exit> {
                    update()
                }
            })
        }

        update()
    }

    private fun update() {
        val count = (config.tokens.size - page * 3).coerceIn(1, 3)
        val width1 = 114f / count

        for (i in 0..2) {
            val index = page * 3 + i
            val visible = i < count

            slot0[i].visible = visible
            slot0[i].position = FixedPositionConstraint(i * width1, 0f)
            slot0[i].size = FixedSizeConstraint(width1, 14f)
            labels[i].text = if (visible) CascadeFonts.arial.truncate(config.tokens[index], 8f, width1 - 6f, "…").literal() else "".literal()

            if (i >= 2) continue
            slot1[i].visible = i + 1 < count
            slot1[i].position = FixedPositionConstraint((i + 1) * width1, 3f)
        }

        if (total <= 1) return
        chevron0?.color = if (page > 0) Catppuccin.Mocha.Text.argb else Catppuccin.Mocha.Surface2.argb
        chevron1?.color = if (page < total - 1) Catppuccin.Mocha.Text.argb else Catppuccin.Mocha.Surface2.argb
    }

    companion object {
        fun of(parent: IPrimitiveElement<*>, config: ConfigVariablesElementData): ConfigVariablesElement {
            return ConfigVariablesElement(config).apply {
                attach(parent)
            }
        }
    }
}
