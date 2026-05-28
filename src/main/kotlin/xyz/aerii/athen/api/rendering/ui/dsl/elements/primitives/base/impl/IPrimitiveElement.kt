@file:Suppress("Unused", "Unchecked_cast")

package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.IPositionConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.constraints.base.ISizeConstraint
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveChildren
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveConstrainable
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveEvents
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveFindable
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveInteractable
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveLayoutResolver
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces.IPrimitiveVisible
import xyz.aerii.athen.api.rendering.ui.dsl.events.base.UIEvent
import java.util.concurrent.CopyOnWriteArrayList

abstract class IPrimitiveElement<T : IPrimitiveElement<T>> : IPrimitiveChildren<T>, IPrimitiveConstrainable<T>, IPrimitiveEvents<T>, IPrimitiveFindable<T>, IPrimitiveInteractable<T>, IPrimitiveLayoutResolver<T>, IPrimitiveVisible<T> {
    private var _root: IPrimitiveElement<*>? = null

    abstract var x: Int
    abstract var y: Int
    abstract var width: Int
    abstract var height: Int
    abstract var color: Int

    override val listeners: MutableMap<Class<out UIEvent>, MutableList<UIEvent.() -> Unit>> = mutableMapOf()
    override val children: CopyOnWriteArrayList<IPrimitiveElement<*>> = CopyOnWriteArrayList()

    override val root: IPrimitiveElement<*>
        get() = _root ?: generateSequence(this as IPrimitiveElement<*>) { it.parent }.last().also { _root = it }

    override val self: T
        get() = this as T

    override var parent: IPrimitiveElement<*>? = null
        set(value) {
            field = value
            _root = null
        }

    override var size: ISizeConstraint? = null
        set(value) {
            field = value
            root.layout()
        }

    override var position: IPositionConstraint? = null
        set(value) {
            field = value
            root.layout()
        }

    override var visible: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            root.layout()
        }

    override var focused: IPrimitiveElement<*>? = null
    override var interact: Boolean = true
    override var hovered: Boolean = false

    open fun render(graphics: GuiGraphics) {
        if (!visible) return
        for (c in children) c.render(graphics)
    }

    inline fun <reified E : UIEvent> on(noinline listener: E.() -> Unit): T {
        return on(E::class.java, listener)
    }
}