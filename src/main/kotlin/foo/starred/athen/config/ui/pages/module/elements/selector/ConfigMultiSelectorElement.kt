package foo.starred.athen.config.ui.pages.module.elements.selector

import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.impl.ConfigMultiSelectorElementData
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
import foo.starred.cascade.primitives.impl.*
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.ImagePrimitive.Companion.image
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.utils.literal
import net.minecraft.client.gui.GuiGraphicsExtractor

class ConfigMultiSelectorElement(
    private val config: ConfigMultiSelectorElementData
) : RoundedRectanglePrimitive() {
    private val labels = mutableListOf<TextPrimitive>()
    private val slot0 = mutableListOf<ContainerPrimitive>()
    private val slot1 = mutableListOf<RectanglePrimitive>()
    private val total: Int = maxOf(1, (config.options.size + 2) / 3)

    private var page: Int = 0
    private var selected: MutableList<Int> = get(ConfigManager.get(config.key))

    private var chevron0: ImagePrimitive? = null
    private var chevron1: ImagePrimitive? = null

    private val text0 = text {
        wrapper = CascadeTextWrapper
        textSize = 8f
        color = Catppuccin.Mocha.Text.argb
        position = CenterPositionConstraint()
    }

    private val box = object : RoundedRectanglePrimitive() {
        override fun draw(graphics: GuiGraphicsExtractor) {
            graphics.nextStratum()
            super.draw(graphics)
        }
    }.apply {
        radius = CascadeGeometricRadius(4f)
        color = Catppuccin.Mocha.Base.argb
        visible = false

        effect(OutlineEffect {
            color = Catppuccin.Mocha.Surface2.argb
            inset = false
        })

        adopt(text0)
        attach(ConfigUI.scene)
    }

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
                    color = Catppuccin.Mocha.Subtext0.argb
                    position = CenterPositionConstraint()
                }.also { labels.add(it) })

                on<MouseEvent.Press> {
                    if (button != 0) return@on
                    cancel()

                    val index = page * 3 + i
                    if (index >= config.options.size) return@on

                    if (selected.contains(index)) selected.remove(index) else selected.add(index)
                    ConfigManager.update(config.key, selected.toList())
                    update()
                }

                on<MouseEvent.Move.Enter> {
                    val index = page * 3 + i
                    if (index >= config.options.size) return@on

                    val width0 = 114f / (config.options.size - page * 3).coerceIn(1, 3)

                    val text = config.options[index]
                    val width1 = CascadeFonts.arial.width(text, 8f)
                    if (width1 <= width0 - 6f) return@on

                    text0.text = text.literal()
                    val width2 = width1 + 12f

                    box.size = FixedSizeConstraint(width2, 16f)
                    val x1 = this@ConfigMultiSelectorElement.x + i * width0
                    val y1 = this@ConfigMultiSelectorElement.y

                    box.position = FixedPositionConstraint(x1 + (width0 - width2) / 2f, if (y1 - 19f < 0f) y1 + 17f else y1 - 19f)
                    box.visible = true
                }

                on<MouseEvent.Move.Exit> {
                    box.visible = false
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
                    box.visible = false
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
                    box.visible = false
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
        val count = (config.options.size - page * 3).coerceIn(1, 3)
        val width1 = 114f / count

        for (i in 0..2) {
            val index = page * 3 + i
            val visible = i < count

            slot0[i].visible = visible
            slot0[i].position = FixedPositionConstraint(i * width1, 0f)
            slot0[i].size = FixedSizeConstraint(width1, 14f)
            labels[i].text = if (visible) CascadeFonts.arial.truncate(config.options[index], 8f, width1 - 6f, "…").literal() else "".literal()
            labels[i].color = if (selected.contains(index)) Catppuccin.Mocha.Green.argb else Catppuccin.Mocha.Subtext0.argb

            if (i >= 2) continue
            slot1[i].visible = i + 1 < count
            slot1[i].position = FixedPositionConstraint((i + 1) * width1, 3f)
        }

        if (total <= 1) return
        chevron0?.color = if (page > 0) Catppuccin.Mocha.Text.argb else Catppuccin.Mocha.Surface2.argb
        chevron1?.color = if (page < total - 1) Catppuccin.Mocha.Text.argb else Catppuccin.Mocha.Surface2.argb
    }

    private fun get(value: Any?): MutableList<Int> {
        val list = value as? List<*> ?: config.default
        return list.mapNotNull { (it as? Number)?.toInt() }.toMutableList()
    }

    companion object {
        fun of(parent: IPrimitiveElement<*>, config: ConfigMultiSelectorElementData): ConfigMultiSelectorElement {
            return ConfigMultiSelectorElement(config).apply {
                attach(parent)
            }
        }
    }
}
