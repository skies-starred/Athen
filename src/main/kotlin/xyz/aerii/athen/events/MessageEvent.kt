package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.Event
import xyz.aerii.athen.handlers.Typo.stripped

sealed class MessageEvent {
    sealed class Chat {
        data class Intercept(val message: Component) : CancellableEvent()

        data class Receive(val message: Component) : Event() {
            val stripped = message.stripped()
        }
    }

    data class ActionBar(val message: Component) : Event()
}