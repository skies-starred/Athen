@file:Suppress("Unused")

package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces

import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

interface IPrimitiveVisible<T> : IPrimitiveSelf<T> where T : IPrimitiveElement<T> {
    var visible: Boolean
}