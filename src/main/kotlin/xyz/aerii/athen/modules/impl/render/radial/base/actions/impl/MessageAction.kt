package xyz.aerii.athen.modules.impl.render.radial.base.actions.impl

import xyz.aerii.athen.handlers.Typo.message
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction

class MessageAction(val message: String) : IAction {
    override fun run() {
        if (message.isEmpty()) return
        message.message()
    }
}