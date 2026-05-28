package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.IPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.data.PositionAnchor
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class AnchorPositionConstraint(val fn: () -> IPrimitiveElement<*>, val anchor: PositionAnchor, val x: Int = 0, val y: Int = 0) : IPositionConstraint {
    override fun x(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        val t = fn()
        return when (anchor) {
            PositionAnchor.LEFT -> t.x - element.width + x
            PositionAnchor.RIGHT -> t.x + t.width + x
            PositionAnchor.ABOVE, PositionAnchor.BELOW -> t.x + x
        }
    }

    override fun y(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        val t = fn()
        return when (anchor) {
            PositionAnchor.ABOVE -> t.y - element.height + y
            PositionAnchor.BELOW -> t.y + t.height + y
            PositionAnchor.LEFT, PositionAnchor.RIGHT -> t.y + y
        }
    }
}