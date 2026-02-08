package xyz.aerii.athen.events

import net.minecraft.world.entity.Entity
import xyz.aerii.athen.api.skyblock.SlayerInfo
import xyz.aerii.athen.events.core.Event

sealed class SlayerEvent {
    data class Cleanup(
        val type: CleanupType
    ) : Event()

    sealed class Boss {
        data class Spawn(
            val entity: Entity,
            val slayerInfo: SlayerInfo
        ) : Event()

        data class Death(
            val entity: Entity,
            val slayerInfo: SlayerInfo
        ) : Event()
    }

    sealed class Miniboss {
        data class Spawn(
            val entity: Entity,
            val slayerInfo: SlayerInfo
        ) : Event()

        data class Death(
            val entity: Entity,
            val slayerInfo: SlayerInfo
        ) : Event()
    }

    sealed class Demon {
        data class Spawn(
            val entity: Entity,
            val slayerInfo: SlayerInfo
        ) : Event()

        data class Death(
            val entity: Entity,
            val slayerInfo: SlayerInfo
        ) : Event()
    }

    sealed class Quest {
        data object Start : Event()

        data object End : Event()
    }

    enum class CleanupType {
        QuestFail,
        ServerChange
        ;
    }
}