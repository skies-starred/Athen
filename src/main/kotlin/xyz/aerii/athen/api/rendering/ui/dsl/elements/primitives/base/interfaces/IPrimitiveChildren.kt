@file:Suppress("Unused")

package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement
import java.util.concurrent.CopyOnWriteArrayList

interface IPrimitiveChildren<T> : IPrimitiveSelf<T> where T : IPrimitiveElement<T> {
    val children: CopyOnWriteArrayList<IPrimitiveElement<*>>
    val root: IPrimitiveElement<*>
    var parent: IPrimitiveElement<*>?

    fun children(graphics: GuiGraphics) {
        for (c in children) c.render(graphics)
    }

    fun adopt(a: IPrimitiveElement<*>): T {
        a.parent?.children?.remove(a)

        a.parent = self
        children.add(a)

        root.layout()
        return self
    }

    fun attach(a: IPrimitiveElement<*>): T {
        a.adopt(self)
        return self
    }

    fun forEach(reversed: Boolean = false, block: (IPrimitiveElement<*>) -> Unit) {
        block(self)

        val a = if (reversed) children.asReversed() else children
        for (b in a) b.forEach(reversed, block)
    }
}