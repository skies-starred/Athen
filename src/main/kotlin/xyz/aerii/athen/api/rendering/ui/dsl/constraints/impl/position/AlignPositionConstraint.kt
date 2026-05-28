package xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.position

import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.IPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.impl.data.PositionAlignment
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

class AlignPositionConstraint(val horizontal: PositionAlignment = PositionAlignment.START, val vertical: PositionAlignment = PositionAlignment.START, val x: Int = 0, val y: Int = 0) : IPositionConstraint {
    override fun x(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        val base = when (horizontal) {
            PositionAlignment.START -> parent.x
            PositionAlignment.CENTER -> parent.x + (parent.width - element.width) / 2
            PositionAlignment.END -> parent.x + parent.width - element.width
        }

        return base + x
    }

    override fun y(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int {
        val base = when (vertical) {
            PositionAlignment.START -> parent.y
            PositionAlignment.CENTER -> parent.y + (parent.height - element.height) / 2
            PositionAlignment.END -> parent.y + parent.height - element.height
        }

        return base + y
    }
}