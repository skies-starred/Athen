package foo.starred.athen.modules.impl.general.messageactions.actions.base

import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionType
import foo.starred.athen.modules.impl.general.messageactions.actions.impl.NoAction

interface IMessageAction {
    val id: Int
    val name: String
    val serializable: Map<String, String> get() = emptyMap()

    fun run()
    fun resolve(text: String, match: MatchResult?): IMessageAction

    companion object {
        private val registry = mutableMapOf<Int, MessageActionType>()

        fun register(a: MessageActionType) {
            registry[a.id] = a
        }

        fun get(id: Int): MessageActionType? =
            registry[id]

        fun create(id: Int, data: Map<String, String>): IMessageAction =
            registry[id]?.create?.invoke(data) ?: NoAction

        fun all(): List<MessageActionType> =
            registry.values.sortedBy { it.id }
    }
}
