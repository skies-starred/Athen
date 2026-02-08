package xyz.aerii.athen.events

import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonInfo
import xyz.aerii.athen.events.core.Event

sealed class InputEvent {
    sealed class Keyboard {
        data class Press(
            val keyEvent: KeyEvent
        ) : Event()

        data class Release(
            val keyEvent: KeyEvent
        ) : Event()
    }

    sealed class Mouse {
        data class Press(
            val buttonInfo: MouseButtonInfo
        ) : Event()

        data class Release(
            val buttonInfo: MouseButtonInfo
        ) : Event()
    }
}