package foo.starred.athen.modules.impl.general.messageactions.actions.data

import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.api.dungeon.data.DungeonClass
import foo.starred.athen.api.dungeon.data.DungeonFloor

data class MessageActionEntry(
    val pattern: String,
    val match: MessageMatchType,
    val action: IMessageAction,
    val enabled: Boolean = true,
    val category: String = "",
    val cancel: Boolean = false,
    val delay: Double = 0.0,
    val floors: MutableSet<DungeonFloor> = mutableSetOf(),
    val classes: MutableSet<DungeonClass> = mutableSetOf()
)


