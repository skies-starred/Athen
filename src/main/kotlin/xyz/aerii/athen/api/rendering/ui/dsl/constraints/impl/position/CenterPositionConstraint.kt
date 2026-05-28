package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.IPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class CenterPositionConstraint : IPositionConstraint {
    override fun x(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return parent.x + (parent.width - element.width) / 2
    }

    override fun y(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        return parent.y + (parent.height - element.height) / 2
    }
}