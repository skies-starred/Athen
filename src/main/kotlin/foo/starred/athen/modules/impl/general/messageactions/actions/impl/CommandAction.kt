@file:Suppress("ConstPropertyName")

package foo.starred.athen.modules.impl.general.messageactions.actions.impl

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionField
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionType
import foo.starred.kontext.abstraction.chat.command.MinecraftCommands

@LoadableAthenModule
class CommandAction(val command: String) : IMessageAction {
    private val empty = command.isEmpty()

    override val id: Int = id0
    override val name: String = name0
    override val serializable: Map<String, String> = mapOf("command" to command)

    override fun run() {
        if (empty) return
        MinecraftCommands.client(command)
    }

    override fun resolve(text: String, match: MatchResult?): IMessageAction {
        if (match == null) return this

        var v = this.command
        for (i in match.groupValues.indices.reversed()) {
            v = v.replace("$$i", if (i == 0) text else match.groupValues[i])
        }

        return CommandAction(v)
    }

    companion object {
        const val id0 = 1
        const val name0 = "Command"

        init {
            IMessageAction.register(MessageActionType(id0, name0, listOf(MessageActionField("command", "Command", "Command to execute"))) {
                CommandAction(it["command"] ?: "")
            })
        }
    }
}
