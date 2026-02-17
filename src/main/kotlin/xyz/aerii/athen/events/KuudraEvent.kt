package xyz.aerii.athen.events

import xyz.aerii.athen.events.core.Event

sealed class KuudraEvent {
    data object Start : Event()

    sealed class End {
        data object Success : Event()

        data object Defeat : Event()

        data object Any : Event()
    }
}