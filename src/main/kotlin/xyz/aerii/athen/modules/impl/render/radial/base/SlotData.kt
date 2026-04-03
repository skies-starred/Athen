package xyz.aerii.athen.modules.impl.render.radial.base

import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction

data class SlotData(val name: String, val itemId: String, val at: Int, val av: String, val sub: List<SlotData>, val text: String? = null)

fun ISlot.toData(): SlotData = SlotData(
    name = name,
    itemId = itemId,
    at = action.id,
    av = action.serializable,
    sub = sub.map { it.toData() },
    text = text
)

fun SlotData.toSlot(): ISlot = ISlot(
    name = name,
    itemId = itemId,
    action = IAction.create(at, av),
    sub = sub.map { it.toSlot() },
    text = text
)
