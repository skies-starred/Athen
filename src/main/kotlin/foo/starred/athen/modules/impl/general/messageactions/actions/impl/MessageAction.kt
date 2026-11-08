@file:Suppress("ConstPropertyName")

package foo.starred.athen.modules.impl.general.messageactions.actions.impl

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionField
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionType
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages

@LoadableAthenModule
class MessageAction(val message: String) : IMessageAction {
    private val empty = message.isEmpty()

    override val id: Int = id0
    override val name: String = name0
    override val serializable: Map<String, String> = mapOf("message" to message)

    override fun run() {
        if (empty) return
        MinecraftMessages.send(message)
    }

    override fun resolve(text: String, match: MatchResult?): IMessageAction {
        if (match == null) return this

        var v = this.message
        for (i in match.groupValues.indices.reversed()) {
            v = v.replace("$$i", if (i == 0) text else match.groupValues[i])
        }

        return MessageAction(v)
    }

    companion object {
        const val id0 = 2
        const val name0 = "Message"

        init {
            IMessageAction.register(MessageActionType(id0, name0, listOf(MessageActionField("message", "Title", "Title text"))) {
                MessageAction(it["message"] ?: "")
            })
        }
    }
}
