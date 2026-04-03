package xyz.aerii.athen.modules.impl.render.radial.base.actions

import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.NoAction

interface IAction {
    val id: Int
    val name: String
    val serializable: String

    fun run()

    companion object {
        private val registry = mutableMapOf<Int, ActionType>()

        fun register(a: ActionType) {
            registry[a.id] = a
        }

        fun create(id: Int, data: String): IAction {
            return registry[id]?.fn?.invoke(data) ?: NoAction
        }

        fun all(): List<ActionType> {
            return registry.values.sortedBy { it.id }
        }
    }
}