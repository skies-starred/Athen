@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.dungeon.terminals

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.DungeonEvent
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Scurry.rawX
import xyz.aerii.athen.handlers.Scurry.rawY
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.render.animations.springValue
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Load
object TerminalClick : Module(
    "Terminal click",
    "Lines between when you clicked the mouse button in terminals",
    Category.DUNGEONS
) {
    private data class Click(val x: Float, val y: Float, val button: Int)
    private val clicks = mutableListOf<Click>()
    private var render = React(true)
    private var opacity = springValue(0f, 0.15f)

    private val radius by config.slider("Radius", 4, 1, 10)
    private val thickness by config.slider("Thickness", 2, 1, 10)
    private val `color$mouse$left` by config.colorPicker("Left mouse color", Color(Catppuccin.Mocha.Mauve.argb))
    private val `color$mouse$right` by config.colorPicker("Right mouse color", Color(Catppuccin.Mocha.Peach.argb))

    init {
        on<GuiEvent.Input.Mouse.Press> {
            clicks.add(Click(rawX, rawY, keyEvent.button()))
        }.runWhen(TerminalAPI.terminalOpen)

        on<GuiEvent.Render.Post> {
            val cs = clicks.toList()
            if (cs.isEmpty()) return@on

            val alpha = opacity.value
            if (alpha == 0f) return@on

            NVGSpecialRenderer.draw(graphics, 0, 0, client.window.width, client.window.height) {
                NVGRenderer.push()
                NVGRenderer.globalAlpha(alpha)

                for (i in 0 until cs.size - 1) {
                    val c1 = cs[i]
                    val c2 = cs[i + 1]
                    val color = if (c1.button == 0) `color$mouse$left`.rgb else `color$mouse$right`.rgb

                    NVGRenderer.drawLine(c1.x, c1.y, c2.x, c2.y, thickness.toFloat(), color)
                }

                for (c in cs) {
                    val color = if (c.button == 0) `color$mouse$left`.rgb else `color$mouse$right`.rgb
                    NVGRenderer.drawCircle(c.x, c.y, radius.toFloat(), color)
                }

                NVGRenderer.pop()
            }
        }.runWhen(render)

        on<DungeonEvent.Terminal.Close> {
            render.value = true

            Chronos.Time after 100.milliseconds then {
                opacity.value = 1f
            }

            Chronos.Time after 3.seconds then {
                opacity.value = 0f
            }

            Chronos.Time after 4.seconds then {
                reset()
            }
        }

        on<DungeonEvent.Terminal.Open> {
            reset()
            opacity.value = 0f
        }
    }

    private fun reset() {
        render.value = false
        clicks.clear()
    }
}