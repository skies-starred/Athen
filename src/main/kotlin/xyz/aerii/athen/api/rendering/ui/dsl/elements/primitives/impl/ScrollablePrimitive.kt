package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveScrollable
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.MouseEvent

open class ScrollablePrimitive : IPrimitiveElement<ScrollablePrimitive>(), IPrimitiveScrollable {
    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0
    override var color: Int = -1

    val content: Int
        get() = children.maxOfOrNull { (it.y - y) + it.height } ?: 0

    val maxScroll: Int
        get() = (content - height).coerceAtLeast(0)

    var scroll: Int = 0
        private set

    init {
        on<MouseEvent.Scroll> {
            scroll = (scroll - amount.toInt() * 10).coerceIn(0, maxScroll)
            cancel()
        }
    }

    override fun render(graphics: GuiGraphics) {
        if (!visible) return

        graphics.enableScissor(x, y, x + width, y + height)
        graphics.pose().pushMatrix()
        graphics.pose().translate(0f, -scroll.toFloat())

        super.render(graphics)

        graphics.pose().popMatrix()
        graphics.disableScissor()
    }

    override fun find(x: Double, y: Double): IPrimitiveElement<*>? {
        if (!contains(x, y)) return null

        val oy = y + scroll
        for (c in children.asReversed()) {
            val a = c.find(x, oy) ?: continue
            return a
        }

        return this
    }

    companion object {
        val NONE = ScrollablePrimitive()

        inline fun scrollable(block: ScrollablePrimitive.() -> Unit): ScrollablePrimitive {
            return ScrollablePrimitive().apply(block)
        }
    }
}