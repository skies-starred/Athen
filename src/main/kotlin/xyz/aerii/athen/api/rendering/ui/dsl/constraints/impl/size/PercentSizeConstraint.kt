package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.ISizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class PercentSizeConstraint(val w: Float, val h: Float) : ISizeConstraint {
    override fun width(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return (parent.width * w).toInt()
    }

    override fun height(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return (parent.height * h).toInt()
    }
}