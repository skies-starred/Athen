package xyz.aerii.athen.events

import xyz.aerii.athen.events.core.Event

sealed class TickEvent {
    data object Client : Event()

    data object Server : Event()
}
