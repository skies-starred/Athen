package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.size

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.ISizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.data.FillAxis
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class FillAxisSizeConstraint(val axis: FillAxis, val fixed: Int, val padding: Int = 0) : ISizeConstraint {
    override fun width(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        if (axis == FillAxis.HORIZONTAL) return (parent.width - padding * 2).coerceAtLeast(0)
        return fixed
    }

    override fun height(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        if (axis == FillAxis.VERTICAL) return (parent.height - padding * 2).coerceAtLeast(0)
        return fixed
    }
}