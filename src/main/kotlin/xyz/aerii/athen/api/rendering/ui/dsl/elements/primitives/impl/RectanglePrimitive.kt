package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement
import xyz.aerii.athen.api.rendering.ui.effects.outline.outline
import xyz.aerii.athen.api.rendering.ui.shapes.rectangle.rectangle

open class RectanglePrimitive : IPrimitiveElement<RectanglePrimitive>() {
    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0
    override var color: Int = -1

    var border: Boolean = false
    var borderInset: Boolean = true
    var borderWidth: Int = 1
    var borderColor: Int = -1

    override fun render(graphics: GuiGraphics) {
        if (!visible) return

        graphics.rectangle(x, y, width, height, color)
        if (border) graphics.outline(x, y, width, height, borderWidth, borderColor, borderInset)

        super.render(graphics)
    }
}