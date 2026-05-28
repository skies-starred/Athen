@file:Suppress("Unchecked_Cast")

package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces

import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

interface IPrimitiveLayoutResolver<T> : IPrimitiveSelf<T> where T : IPrimitiveElement<T> {
    fun layout() {
        val self = self

        for (child in self.children) {
            if (!child.visible) continue
            child.constrain(self)
            child.layout()
        }
    }
}