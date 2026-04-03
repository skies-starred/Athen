package xyz.aerii.athen.modules.impl.render.radial.base.actions.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.modules.impl.render.radial.base.actions.ActionType
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction

@Load
object NoAction : IAction {
    override val id: Int = 0
    override val name: String = "None"
    override val serializable: String = ""

    override fun run() {}

    init {
        IAction.register(ActionType(id, name) { NoAction })
    }
}