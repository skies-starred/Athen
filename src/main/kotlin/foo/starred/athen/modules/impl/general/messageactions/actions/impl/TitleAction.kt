@file:Suppress("ConstPropertyName")

package foo.starred.athen.modules.impl.general.messageactions.actions.impl

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionField
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionType
import foo.starred.kontext.abstraction.title.MinecraftTitle
import foo.starred.kontext.api.text.parser.impl.parse

@LoadableAthenModule
class TitleAction(val text: String, val stay: Int = 70) : IMessageAction {
    private val parsed = text.parse()
    private val empty = text.isEmpty()

    override val id: Int = id0
    override val name: String = name0
    override val serializable: Map<String, String> = mapOf("text" to text, "stay" to stay.toString())

    override fun run() {
        if (empty) return
        MinecraftTitle.display(parsed, fadeIn = 10, stay = stay, fadeOut = 10)
    }

    override fun resolve(text: String, match: MatchResult?): IMessageAction {
        if (match == null) return this

        var v = this.text
        for (i in match.groupValues.indices.reversed()) {
            v = v.replace("$$i", if (i == 0) text else match.groupValues[i])
        }

        return TitleAction(v, stay)
    }

    companion object {
        const val id0 = 4
        const val name0 = "Title"

        init {
            IMessageAction.register(MessageActionType(id0, name0, listOf(MessageActionField("text", "Title", "Title text"), MessageActionField("stay", "Duration (ticks)", "70", "70", true))) {
                TitleAction(it["text"] ?: "", it["stay"]?.toIntOrNull() ?: 70)
            })
        }
    }
}
