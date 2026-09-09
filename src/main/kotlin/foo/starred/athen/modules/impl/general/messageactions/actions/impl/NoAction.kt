package foo.starred.athen.modules.impl.general.messageactions.actions.impl

import foo.starred.athen.annotations.Load
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionType

@Load
object NoAction : IMessageAction {
    override val id = 0
    override val name = "None"

    override fun run() {
    }

    override fun resolve(text: String, match: MatchResult?): IMessageAction {
        return this
    }

    init {
        IMessageAction.register(MessageActionType(id, name) { NoAction })
    }
}
