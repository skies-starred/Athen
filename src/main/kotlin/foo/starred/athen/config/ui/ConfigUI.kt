package foo.starred.athen.config.ui

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.config.hud.ui.HudElementEditorUI
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.config.ui.pages.main.ConfigCategories
import foo.starred.athen.config.ui.pages.module.ConfigModules
import foo.starred.athen.config.ui.pages.module.ConfigModulesNavigationState
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputElement
import foo.starred.athen.config.ui.pages.module.elements.input.ConfigInputElement.Companion.configInputElement
import foo.starred.athen.modules.impl.ModSettings
import foo.starred.athen.utils.command
import foo.starred.cascade.constraints.base.ISizeConstraint
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.BackdropBlurEffect
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.OverlayPrimitive.Companion.overlay
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.text.parser.impl.parse
import net.minecraft.client.gui.GuiGraphicsExtractor

@LoadableAthenModule
object ConfigUI : CascadeScreen("Config UI [Athen]", CascadeGeometricResolution.FHD.of(2f)) {
    private val text = text {
        wrapper = CascadeTextWrapper
        textSize = 10f
        color = CascadeGeometricColor(MochaColorScheme.Text.argb)
        position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.START)
        offset = CascadeGeometricOffset(4f, 4f)
    }

    private val tooltip = overlay {
        visible = false
        follow = true
        clamp = true

        adopt(roundedRectangle {
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            radius = CascadeGeometricRadius(4f)
            position = FixedPositionConstraint(5f, 5f)

            size = object : ISizeConstraint {
                override fun _width(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Float {
                    return CascadeFonts.sans.width(text.text, 10f) + 8f
                }

                override fun _height(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Float {
                    return (CascadeFonts.sans.regular.height * 10f) + 8f
                }
            }

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                inset = false
            })

            adopt(text)
        })
    }

    lateinit var search: ConfigInputElement
        private set

    var left: RoundedRectanglePrimitive
        private set

    var right0: RoundedRectanglePrimitive
        private set

    var modules: ScrollablePrimitive
        private set

    var divider: RectanglePrimitive
        private set

    var right: ScrollablePrimitive
        private set

    init {
        command {
            executes {
                if (!ModSettings.commandConfig) return@executes help()

                open()
                "Opened the config! <gray>(use /athen help to view commands)".mod()
            }

            "help" {
                help()
            }

            "config" {
                open()
            }

            "hud" {
                MinecraftGui.screen(HudElementEditorUI)
            }
        }

        val panel = container {
            position = CenterPositionConstraint()
            size = FixedSizeConstraint(650f, 350f)

            attach(scene)
        }

        roundedRectangle {
            position = FixedPositionConstraint(0f, 0f)
            size = FixedSizeConstraint(140f, 32f)
            color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.88f))
            radius = CascadeGeometricRadius(5f)

            effect(BackdropBlurEffect {
                blur = 12f
                color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.40f))
                radius = CascadeGeometricRadius(5f)
            })

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
                inset = false
            })

            attach(panel)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "<bold><#FDCCDA>A<#FCDDD3>t<#FAEDCB>h<#F0E2D7>e<#E5D8E4>n<#DBCDF0>".parse()
                textSize = 16f
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = CenterPositionConstraint()
            })
        }

        roundedRectangle {
            position = FixedPositionConstraint(148f, 0f)
            size = FixedSizeConstraint(502f, 32f)
            color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.88f))
            radius = CascadeGeometricRadius(5f)

            effect(BackdropBlurEffect {
                blur = 12f
                color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.40f))
                radius = CascadeGeometricRadius(5f)
            })

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
                inset = false
            })

            attach(panel)
            adopt(ConfigModulesNavigationState.fn())
            adopt(configInputElement {
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(8f, 0f)
                size = FixedSizeConstraint(180f, 18f)
                placeholder = "Search..."
                color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.45f))

                update {
                    ConfigModules.active = null
                    ConfigModules.fn()
                }
            }.also { search = it })
        }

        left = roundedRectangle {
            position = FixedPositionConstraint(0f, 40f)
            size = FixedSizeConstraint(140f, 310f)
            color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.88f))
            radius = CascadeGeometricRadius(5f)

            effect(BackdropBlurEffect {
                blur = 12f
                color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.40f))
                radius = CascadeGeometricRadius(5f)
            })

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
                inset = false
            })

            attach(panel)
        }

        right0 = roundedRectangle {
            position = FixedPositionConstraint(148f, 40f)
            size = FixedSizeConstraint(502f, 310f)
            color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.88f))
            radius = CascadeGeometricRadius(5f)

            effect(BackdropBlurEffect {
                blur = 12f
                color = CascadeGeometricColor(MochaColorScheme.Mantle.alpha(0.40f))
                radius = CascadeGeometricRadius(5f)
            })

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
                inset = false
            })

            attach(panel)
        }

        modules = scrollable {
            position = FixedPositionConstraint(0f, 0f)
            size = FixedSizeConstraint(205f, 310f)
            attach(right0)
        }

        divider = rectangle {
            position = FixedPositionConstraint(205f, 6f)
            size = FixedSizeConstraint(1f, 298f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.alpha(0.35f))
            interact = false
            attach(right0)
        }

        right = scrollable {
            position = FixedPositionConstraint(206f, 0f)
            size = FixedSizeConstraint(296f, 310f)
            attach(right0)
        }

        ConfigCategories.fn()
        ConfigModules.fn()

        tooltip.attach(scene)
    }

    fun show(text0: String) {
        text.text = text0.parse()
        tooltip.visible = true
    }

    fun hide() {
        tooltip.visible = false
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x40000000)
    }

    override fun onClose() {
        hide()
        super.onClose()
    }

    private fun help() {
        val divider = ("§8§m" + MinecraftChat.repeat("-")).literal()

        MinecraftMessages.display(divider)
        MinecraftMessages.display(MinecraftChat.center("§bAthen Commands"))
        MinecraftMessages.display(divider)

        val commands = listOf(
            "/athen config" to "Open the configuration menu",
            "/athen hud" to "Open the HUD editor",
            "/athen simulate terminals" to "Terminal simulator",
            "/athen radial help" to "Info about radial menu",
            "/athen visuals help" to "Info about visual words replacement",
            "/athen carry help" to "Info about slayer carry commands",
            "/athen dcarry help" to "Info about dungeon carry commands",
            "/athen kcarry help" to "Info about kuudra carry commands",
            "/athen clear chat" to "Clear the chat history",
            "/athen stats <name>" to "View stats for any player",
            "/athen times slayers" to "Shows the slayer kill times",
            "/athen times kuudra <tier>" to "Shows the kuudra pbs",
            "/athen toggle feature <featureKey>" to "Toggles the specified feature!",
            "/athen irc help" to "View all IRC commands"
        )

        for ((c, d) in commands) {
            MinecraftMessages.display("  <${MochaColorScheme.Green.argb}>$c <dark_gray>- <gray>$d")
        }

        MinecraftMessages.display(divider)
    }
}
