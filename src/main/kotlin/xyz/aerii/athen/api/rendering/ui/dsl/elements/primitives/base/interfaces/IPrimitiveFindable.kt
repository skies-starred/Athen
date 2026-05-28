package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces

import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

interface IPrimitiveFindable<T> : IPrimitiveSelf<T> where T : IPrimitiveElement<T> {
    fun contains(x: Double, y: Double): Boolean {
        return x >= self.x && y >= self.y && x < self.x + self.width && y < self.y + self.height
    }

    fun find(x: Double, y: Double): IPrimitiveElement<*>? {
        if (!self.visible) return null

        for (c in self.children.asReversed()) {
            val a = c.find(x, y) ?: continue
            return a
        }

        if (self.interact && contains(x, y)) {
            return self
        }

        return null
    }
}