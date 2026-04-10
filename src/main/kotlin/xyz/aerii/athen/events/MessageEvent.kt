package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.Event
import xyz.aerii.library.utils.stripped

sealed class MessageEvent {
    sealed class Chat {
        data class Intercept(val message: Component) : CancellableEvent() {
            val stripped = message.stripped()
        }

        data class Receive(val message: Component) : Event() {
            val stripped = message.stripped()
        }
    }

    sealed class Title {
        data class Main(val message: Component) : CancellableEvent()

        data class Sub(val message: Component) : CancellableEvent()
    }

    data class ActionBar(val message: Component) : Event()
}