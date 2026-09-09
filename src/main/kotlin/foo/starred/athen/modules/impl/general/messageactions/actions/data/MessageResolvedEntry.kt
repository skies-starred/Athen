package foo.starred.athen.modules.impl.general.messageactions.actions.data

import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.utils.regex
import kotlin.time.Duration.Companion.seconds

class MessageResolvedEntry(src: MessageActionEntry) {
    private var last: MatchResult? = null

    val source = src.copy(pattern = src.pattern.trim())
    val groups = source.match == REGEX && source.action.serializable.values.any { '$' in it }

    val regex = if (source.match == REGEX) source.pattern.regex() else null
    val action = source.action
    val delay = source.delay.seconds

    fun matches(text: String, set: Set<String>): Boolean {
        if (!source.enabled) return false
        if (source.category.isNotEmpty() && source.category in set) return false

        return when (source.match) {
            CONTAINS -> text.contains(source.pattern, ignoreCase = true)
            EXACT -> text == source.pattern
            REGEX -> regex?.find(text).also { last = it } != null
        }
    }

    fun action(text: String): IMessageAction {
        if (!groups) return action
        return action.resolve(text, last)
    }
}
