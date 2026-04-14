package xyz.aerii.athen.handlers

import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.library.handlers.time.AbstractChronos

@Priority
object Chronos : AbstractChronos() {
    init {
        on<TickEvent.Client.Start> {
            client0()
        }

        on<TickEvent.Client.End> {
            client1()
        }

        on<TickEvent.Server> {
            server()
        }
    }
}