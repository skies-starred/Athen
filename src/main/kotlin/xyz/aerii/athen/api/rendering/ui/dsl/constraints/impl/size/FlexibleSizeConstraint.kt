package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.ISizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class FlexibleSizeConstraint(val padding: Int = 0) : ISizeConstraint {
    override fun width(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return fn(element, { it.x - element.x }, { it.width })
    }

    override fun height(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return fn(element, { it.y - element.y }, { it.height })
    }

    private inline fun fn(element: IPrimitiveElement<*>, offset: (IPrimitiveElement<*>) -> Int, size: (IPrimitiveElement<*>) -> Int): Int {
        var a = 0

        for (child in element.children) {
            if (!child.visible) continue
            child.constrain(element)

            val b = offset(child)
            val c = size(child)
            val d = b + c
            if (d > a) a = d
        }

        return a + padding
    }
}