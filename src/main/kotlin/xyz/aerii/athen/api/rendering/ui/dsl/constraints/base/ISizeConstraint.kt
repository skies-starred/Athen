package xyz.aerii.athen.api.rendering.ui.dsl.constraints.base

import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

interface ISizeConstraint {
    fun width(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int
    fun height(element: IPrimitiveElement<*>, parent: IPrimitiveElement<*>): Int
}