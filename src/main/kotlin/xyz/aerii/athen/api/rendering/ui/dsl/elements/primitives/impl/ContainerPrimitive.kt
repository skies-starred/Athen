package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl

import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement

open class ContainerPrimitive : IPrimitiveElement<ContainerPrimitive>() {
    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0
    override var color: Int = -1

    companion object {
        val NONE = ContainerPrimitive()

        inline fun container(block: ContainerPrimitive.() -> Unit): ContainerPrimitive {
            return ContainerPrimitive().apply(block)
        }
    }
}