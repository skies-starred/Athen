package foo.starred.athen.modules.impl.general.messageactions.actions.data

import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction

data class MessageActionType(
    val id: Int,
    val name: String,
    val fields: List<MessageActionField>,
    val create: (Map<String, String>) -> IMessageAction
) {
    constructor(id: Int, name: String, fn: () -> IMessageAction) : this(id, name, emptyList(), { fn() })
}
