package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import xyz.aerii.athen.events.core.Event
import xyz.aerii.athen.handlers.Typo.stripped

sealed class MessageEvent {
    data class Chat(val message: Component) : Event() {
        val stripped = message.stripped()
    }

    data class ActionBar(val message: Component) : Event()
}