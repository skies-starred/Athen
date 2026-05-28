package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.interfaces

import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.KeyEvent
import xyz.aerii.athen.api.rendering.ui.dsl.events.impl.MouseEvent

interface IPrimitiveInteractable<T> : IPrimitiveSelf<T> where T : IPrimitiveElement<T> {
    var focused: IPrimitiveElement<*>?
    var interact: Boolean
    var hovered: Boolean

    fun mousePress(x: Double, y: Double, button: Int): Boolean {
        val a = self.find(x, y).also { self.root.focused = it } ?: return false
        return a.post(MouseEvent.Press(x, y, button, a))
    }

    fun mouseRelease(x: Double, y: Double, button: Int): Boolean {
        val a = self.find(x, y) ?: return false
        return a.post(MouseEvent.Release(x, y, button, a))
    }

    fun mouseScroll(x: Double, y: Double, amount: Double): Boolean {
        val a = self.find(x, y) ?: return false
        val b = a.post(MouseEvent.Scroll(x, y, amount, a))
        if (b) return true

        var c = a.parent
        while (c != null) {
            if (c is IPrimitiveScrollable) return c.post(MouseEvent.Scroll(x, y, amount, c))
            c = c.parent
        }

        return false
    }

    fun mouseMove(x: Double, y: Double) {
        val a = self.find(x, y)

        self.forEach {
            val b = it == a
            if (it.hovered == b) return@forEach

            it.hovered = b
            if (it.hovered) it.post(MouseEvent.Move.Enter(x, y, it))
            else it.post(MouseEvent.Move.Exit(x, y, it))
        }

        a?.post(MouseEvent.Move.Any(x, y, a))
    }

    fun keyPress(key: Int): Boolean {
        return focused?.post(KeyEvent.Press(key)) ?: false
    }

    fun keyRelease(key: Int): Boolean {
        return focused?.post(KeyEvent.Release(key)) ?: false
    }

    fun keyType(char: Char): Boolean {
        return focused?.post(KeyEvent.Type(char)) ?: false
    }
}