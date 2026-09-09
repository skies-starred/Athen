package foo.starred.athen.modules.impl.general.messageactions.actions.data

import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction

data class MessageActionEntry(
    val pattern: String,
    val match: MessageMatchType,
    val action: IMessageAction,
    val enabled: Boolean = true,
    val category: String = "",
    val cancel: Boolean = false,
    val delay: Double = 0.0
)
