package foo.starred.athen.modules.impl.general.messageactions.data

import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionEntry
import foo.starred.athen.modules.impl.general.messageactions.ui.data.MessageActionsCategoryEntry
import it.unimi.dsi.fastutil.objects.ObjectArrayList

data class MessageActionsData(
    val actions: ObjectArrayList<MessageActionEntry> = ObjectArrayList(),
    val categories: ObjectArrayList<MessageActionsCategoryEntry> = ObjectArrayList()
)
