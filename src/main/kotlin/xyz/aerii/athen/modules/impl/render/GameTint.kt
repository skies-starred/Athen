package xyz.aerii.athen.modules.impl.render

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.library.handlers.Observable
import xyz.aerii.library.handlers.Observable.Companion.and
import java.awt.Color

@Load
object GameTint : Module(
    "Game tint",
    "Tints the game screen in the color of your choice!",
    Category.RENDER
) {
    private val color by config.colorPicker("Tint color", Color(0, 0, 0, 25))
    private val last = config.switch("Tint HUDs", true).custom("hudTint")
    private val gui = config.switch("Tint GUIs", true).custom("screenTint")

    private val _state = Observable(false)
    private val state = _state.and(gui.state).map { !(it) }

    init {
        on<GuiEvent.Open.Any> {
            _state.value = true
        }

        on<GuiEvent.Close.Any> {
            _state.value = false
        }

        on<GuiEvent.Render.Pre>(-100) {
            graphics.tint()
        }.runWhen(state and last.state.map { !it })

        on<GuiEvent.Render.Post>(-100) {
            graphics.tint()
        }.runWhen(state and last.state)

        on<GuiEvent.Render.Screen.Post> {
            graphics.tint()
        }.runWhen(_state and gui.state)
    }

    private fun GuiGraphics.tint() {
        drawRectangle(0, 0, guiWidth(), guiHeight(), color)
    }
}