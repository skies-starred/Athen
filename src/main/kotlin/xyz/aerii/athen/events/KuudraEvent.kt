package xyz.aerii.athen.events

import xyz.aerii.athen.events.core.Event

sealed class KuudraEvent {
    data object Start : Event()

    data object End : Event()
}