package xyz.aerii.athen.events

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.Event

sealed class GuiEvent {
    sealed class Render {
        data class Pre(
            val graphics: GuiGraphics
        ) : Event()

        data class Post(
            val graphics: GuiGraphics
        ) : Event()
    }

    sealed class Container {
        sealed class Render {
            data class Pre(
                val graphics: GuiGraphics
            ) : CancellableEvent()
        }
    }

    sealed class Slots {
        sealed class Render {
            data class Pre(
                val graphics: GuiGraphics,
                val slot: Slot
            ) : CancellableEvent()

            data class Post(
                val graphics: GuiGraphics,
                val slot: Slot
            ) : Event()
        }

        data class Click (
            val slot: Slot?,
            val slotId: Int,
            val mouseButton: Int,
            val clickType: ClickType
        ) : CancellableEvent()
    }

    sealed class Tooltip {
        data class Render(
            val item: ItemStack,
            val tooltip: MutableList<Component>
        ) : Event()

        data class Update(
            val item: ItemStack,
            val tooltip: MutableList<Component>
        ) : Event()
    }

    sealed class Input {
        sealed class Key {
            data class Press(
                val keyEvent: KeyEvent
            ) : CancellableEvent()
        }

        sealed class Mouse {
            data class Press(
                val keyEvent: MouseButtonEvent
            ) : CancellableEvent()
        }
    }
}
