package xyz.aerii.athen.api.rendering.ui.dsl.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl.ContainerPrimitive
import xyz.aerii.library.api.client
import xyz.aerii.library.api.nextTick
import xyz.aerii.library.handlers.parser.parse

open class PrimitiveScreen(title: String = "Primitive Screen [Athen]") : Screen(title.parse()) {
    val scene = ContainerPrimitive().apply {
        width = this@PrimitiveScreen.width
        height = this@PrimitiveScreen.height
    }

    override fun init() {
        scene.width = width
        scene.height = height
        scene.layout()
    }

    //? if >= 26.1 {
    /*final override fun extractRenderState(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        scene.render(graphics)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    *///?} else {
    final override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        scene.render(graphics)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    //?}

    final override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        return scene.mousePress(event.x(), event.y(), event.button()) || super.mouseClicked(event, isDoubleClick)
    }

    final override fun mouseReleased(event: MouseButtonEvent): Boolean {
        return scene.mouseRelease(event.x(), event.y(), event.button())
    }

    final override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        return scene.mouseScroll(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    final override fun mouseMoved(mouseX: Double, mouseY: Double) {
        scene.mouseMove(mouseX, mouseY)
        super.mouseMoved(mouseX, mouseY)
    }

    final override fun keyPressed(event: KeyEvent): Boolean {
        return scene.keyPress(event.key()) || super.keyPressed(event)
    }

    final override fun keyReleased(event: KeyEvent): Boolean {
        return scene.keyRelease(event.key()) || super.keyReleased(event)
    }

    final override fun charTyped(event: CharacterEvent): Boolean {
        return scene.keyType(event.codepoint().toChar()) || super.charTyped(event)
    }

    fun open() {
        nextTick {
            client.setScreen(this@PrimitiveScreen)
        }
    }
}