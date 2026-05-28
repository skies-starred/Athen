package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.ISizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class FillSizeConstraint(val padding: Int = 0) : ISizeConstraint {
    override fun width(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return (parent.width - padding * 2).coerceAtLeast(0)
    }

    override fun height(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return (parent.height - padding * 2).coerceAtLeast(0)
    }
}