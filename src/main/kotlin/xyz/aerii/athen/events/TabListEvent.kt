package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import xyz.aerii.athen.events.core.Event

sealed class TabListEvent {
    data class Change(
        val old: List<List<String>>,
        val new: List<List<Component>>,
    ) : Event()
}
