package xyz.aerii.athen.events

import xyz.aerii.athen.events.core.Event

sealed class GameEvent : Event() {
    data object Start : GameEvent()

    data object Stop : GameEvent()
}
