@file:Suppress("FunctionName")

package xyz.aerii.athen.accessors

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import xyz.aerii.athen.handlers.Typo.stripped
import java.lang.ref.WeakReference

interface EntityAccessor {
    fun `athen$attachments`(): MutableList<WeakReference<Entity>>
    fun `athen$attach`(): Entity?
    fun `athen$attach`(entity: Entity)
}

val Entity.parent: Entity?
    get() = (this as? EntityAccessor)?.`athen$attach`()

val Entity.attached: List<Entity>
    get() = (this as? EntityAccessor)?.`athen$attachments`()?.mapNotNull { it.get() } ?: emptyList()

val Entity.attachedNames: List<Component>
    get() = attached.mapNotNull { it.customName }

val Entity.attachedStripped: List<String>
    get() = attached.mapNotNull { it.customName?.stripped() }