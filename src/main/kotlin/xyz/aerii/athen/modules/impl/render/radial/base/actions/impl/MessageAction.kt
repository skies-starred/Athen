@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.modules.impl.render.radial.base.actions.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Typo.message
import xyz.aerii.athen.modules.impl.render.radial.base.actions.ActionType
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction

@Load
class MessageAction(val message: String) : IAction {
    override val id: Int = int
    override val name: String = str
    override val serializable: String = message

    override fun run() {
        if (message.isEmpty()) return
        message.message()
    }

    companion object {
        const val int = 2
        const val str = "Message"

        init {
            IAction.register(ActionType(int, str) { MessageAction(it) })
        }
    }
}