package xyz.aerii.athen.events

import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.Event

sealed class KuudraEvent {
    data object Start : Event()

    sealed class End {
        data object Success : Event()

        data object Defeat : Event()

        data object Any : Event()
    }

    sealed class Supply {
        data class Progress(
            val progress: Int,
            val message: String
        ) : CancellableEvent()

        data object Drop : Event()

        data object Pickup : Event()
    }

    sealed class Phase {
        data object Supply : Event()

        data object Build : Event()

        data object Fuel : Event()

        data object Stun : Event()

        data object DPS : Event()

        data object Skip : Event()

        data object Kill : Event()

        data class Any(val new: KuudraPhase) : Event()
    }
}