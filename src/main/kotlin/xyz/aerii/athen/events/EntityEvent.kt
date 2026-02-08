package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import xyz.aerii.athen.events.core.Event

sealed class EntityEvent {
    data class Load(
        val entity: Entity
    ) : Event()

    data class Unload(
        val entity: Entity
    ) : Event()

    data class Death(
        val entity: Entity
    ) : Event()

    data class ComponentAttach(
        val component: Component,
        val infoLineEntity: Entity
    ) : Event()

    data class NameChange(
        val component: Component,
        val infoLineEntity: Entity
    ) : Event()
}