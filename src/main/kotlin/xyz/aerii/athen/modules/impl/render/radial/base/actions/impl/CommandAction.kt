@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.modules.impl.render.radial.base.actions.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Typo.clientCommand
import xyz.aerii.athen.modules.impl.render.radial.base.actions.ActionType
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction

@Load
class CommandAction(val command: String) : IAction {
    override val id: Int = int
    override val name: String = str
    override val serializable: String = command

    override fun run() {
        if (command.isEmpty()) return
        command.clientCommand()
    }

    companion object {
        const val int = 1
        const val str = "Command"

        init {
            IAction.register(ActionType(int, str) { CommandAction(it) })
        }
    }
}