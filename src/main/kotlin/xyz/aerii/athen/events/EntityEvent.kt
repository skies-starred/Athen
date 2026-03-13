package xyz.aerii.athen.events

import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import tech.thatgravyboat.skyblockapi.api.events.entity.EntityAttributesUpdateEvent
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

    sealed class Update {
        data class Attach(
            val component: Component,
            val entity: Entity
        ) : Event() {
            @Deprecated("Use entity.")
            val infoLineEntity: Entity
                get() = entity
        }

        data class Named(
            val component: Component,
            val entity: Entity
        ) : Event() {
            @Deprecated("Use entity.")
            val infoLineEntity: Entity
                get() = entity
        }

        data class Health(
            val entity: LivingEntity,
            val old: Float?,
            val new: Float
        ) : Event()

        data class Equipment(
            val entity: LivingEntity
        ) : Event()

        data class Attributes(
            val entity: LivingEntity,
            val changed: Map<Holder<Attribute>, EntityAttributesUpdateEvent.ChangedAttribute>,
        ) : Event()
    }
}