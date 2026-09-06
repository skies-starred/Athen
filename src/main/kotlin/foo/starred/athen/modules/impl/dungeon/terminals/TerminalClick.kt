@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.dungeon.terminals

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.dungeon.terminals.TerminalAPI
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.config.Category
import foo.starred.athen.events.DungeonEvent
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.cascade.graphics.extensions.circle.circle
import foo.starred.cascade.graphics.extensions.stroke.stroke
import foo.starred.snowbird.api.data.Observable
import foo.starred.snowbird.utils.mouseSX
import foo.starred.snowbird.utils.mouseSY
import org.joml.Matrix3x2f
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@Load
object TerminalClick : Module(
    "Terminal click",
    "Lines between when you clicked the mouse button in terminals",
    Category.DUNGEONS
) {
    private data class Click(val x: Float, val y: Float, val button: Int)
    private val clicks = mutableListOf<Click>()
    private var render = Observable(true)

    private val radius by config.slider("Radius", 4, 1, 10)
    private val thickness by config.slider("Thickness", 2, 1, 10)
    private val `color$mouse$left` by config.colorPicker("Left mouse color", Color(Catppuccin.Mocha.Lavender.argb, true))
    private val `color$mouse$right` by config.colorPicker("Right mouse color", Color(Catppuccin.Mocha.Peach.argb, true))

    init {
        on<GuiEvent.Input.Mouse.Press> {
            clicks.add(Click(mouseSX, mouseSY, keyEvent.button()))
        }.runWhen(TerminalAPI.opened)

        on<GuiEvent.Render.Any.Post> {
            val cs = clicks.toList()
            if (cs.isEmpty()) return@on

            val pose = Matrix3x2f(graphics.pose())
            val scissor = graphics.scissorStack.peek()

            for (i in 0 until cs.size - 1) {
                val c1 = cs[i]
                val c2 = cs[i + 1]
                val color = (if (c1.button == 0) `color$mouse$left`.rgb else `color$mouse$right`.rgb)

                graphics.stroke(c1.x, c1.y, c2.x, c2.y, color, thickness.toFloat(), pose, scissor)
            }

            for (c in cs) {
                val color = (if (c.button == 0) `color$mouse$left`.rgb else `color$mouse$right`.rgb)
                graphics.circle(c.x, c.y, radius.toFloat(), color, pose, scissor)
            }
        }.runWhen(render)

        on<DungeonEvent.Terminal.Close> {
            render.value = true

            Scheduler.schedule(4.seconds) {
                reset()
            }
        }

        on<DungeonEvent.Terminal.Open> {
            reset()
        }
    }

    private fun reset() {
        render.value = false
        clicks.clear()
    }
}
