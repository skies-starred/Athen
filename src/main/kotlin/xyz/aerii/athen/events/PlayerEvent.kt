package xyz.aerii.athen.events

import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.events.core.Event

sealed class PlayerEvent {
    data class HotbarChange(
        val slot: Int,
        val item: ItemStack
    ) : Event()
}
