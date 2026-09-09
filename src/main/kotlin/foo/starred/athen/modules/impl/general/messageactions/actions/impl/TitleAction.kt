@file:Suppress("ConstPropertyName")

package foo.starred.athen.modules.impl.general.messageactions.actions.impl

import foo.starred.athen.annotations.Load
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionField
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionType
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.showTitle

@Load
class TitleAction(val text: String, val stay: Int = 70) : IMessageAction {
    private val parsed = text.parse()
    private val empty = text.isEmpty()

    override val id: Int = int
    override val name: String = str
    override val serializable: Map<String, String> = mapOf("text" to text, "stay" to stay.toString())

    override fun run() {
        if (empty) return
        parsed.showTitle(fadeIn = 10, stay = stay, fadeOut = 10)
    }

    override fun resolve(text: String, match: MatchResult?): IMessageAction {
        if (match == null) return this

        var v = this.text
        for (i in match.groupValues.indices.reversed()) {
            v = if (i == 0) text else v.replace("$$i", match.groupValues[i])
        }

        return TitleAction(v, stay)
    }

    companion object {
        const val int = 4
        const val str = "Title"

        init {
            IMessageAction.register(
                MessageActionType(
                    int,
                    str,
                    fields = listOf(MessageActionField("text", "Title", "Title text"), MessageActionField("stay", "Duration (ticks)", "70", "70", true))
                ) {
                    TitleAction(it["text"] ?: "", it["stay"]?.toIntOrNull() ?: 70)
                }
            )
        }
    }
}
