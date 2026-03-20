package xyz.aerii.athen.modules.impl.render.radial.base

import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.CommandAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.MessageAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.NoAction

data class SlotData(val name: String, val itemId: String, val at: Int, val av: String, val sub: List<SlotData>, val text: String? = null)

fun ISlot.toData(): SlotData = SlotData(
    name = name,
    itemId = itemId,
    at = when (action) {
        is CommandAction -> 1
        is MessageAction -> 2
        else -> 0
    },
    av = when (val a = action) {
        is CommandAction -> a.command
        is MessageAction -> a.message
        else -> ""
    },
    sub = sub.map { it.toData() },
    text = text
)

fun SlotData.toSlot(): ISlot = ISlot(
    name = name,
    itemId = itemId,
    action = when (at) {
        1 -> CommandAction(av)
        2 -> MessageAction(av)
        else -> NoAction
    },
    sub = sub.map { it.toSlot() },
    text = text
)
