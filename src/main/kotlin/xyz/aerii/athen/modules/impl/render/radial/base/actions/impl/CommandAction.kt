package xyz.aerii.athen.modules.impl.render.radial.base.actions.impl

import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction

class CommandAction(val command: String) : IAction {
    override fun run() {
        if (command.isEmpty()) return
        command.command()
    }
}