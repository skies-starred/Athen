package xyz.aerii.athen.modules.impl.render.radial.base.actions

data class ActionType(
    val id: Int,
    val name: String,
    val fn: (String) -> IAction
)