package xyz.aerii.athen.events

import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonKey
import xyz.aerii.athen.api.dungeon.enums.DungeonPlayer
import xyz.aerii.athen.events.core.Event

sealed class DungeonEvent {
    data class Start(
        val floor: DungeonFloor
    ) : Event()

    data class End(
        val floor: DungeonFloor
    ) : Event()

    data class Enter(
        val floor: DungeonFloor
    ) : Event()

    sealed class Player {
        data class Death(
            val player: DungeonPlayer
        ) : Event()
    }

    sealed class Terminal {
        data object Open : Event()

        data object Close : Event()

        data class Update(
            val slot: Int,
            val item: ItemStack
        ) : Event()
    }

    data class KeyPickUp(
        val key: DungeonKey
    ) : Event()
}
