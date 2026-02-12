package xyz.aerii.athen.events

import xyz.aerii.athen.events.core.Event
import xyz.aerii.athen.handlers.Chronos

sealed class TickEvent {
    data object Client : Event() {
        val ticks: Int
            get() = Chronos.Ticker.tickClient
    }

    data object Server : Event() {
        val ticks: Int
            get() = Chronos.Ticker.tickServer
    }
}
