package xyz.aerii.athen.events

import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.events.core.CancellableEvent

sealed class PlayerEvent {
    data class Interact(val item: ItemStack) : CancellableEvent()
}