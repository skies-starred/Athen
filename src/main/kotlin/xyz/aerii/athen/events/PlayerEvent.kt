package xyz.aerii.athen.events

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.events.core.CancellableEvent

sealed class PlayerEvent {
    data class Drop(
        val item: ItemStack?,
        val gui: Boolean
    ) : CancellableEvent()

    sealed class Interact {
        data object None : CancellableEvent()

        data class Block(val pos: BlockPos)  : CancellableEvent()

        data class Entity(val entity: net.minecraft.world.entity.Entity) : CancellableEvent()

        data object Any : CancellableEvent()
    }

    sealed class Attack {
        data class Block(val pos: BlockPos) : CancellableEvent()

        data class Entity(val entity: net.minecraft.world.entity.Entity) : CancellableEvent()

        data object Any : CancellableEvent()
    }
}