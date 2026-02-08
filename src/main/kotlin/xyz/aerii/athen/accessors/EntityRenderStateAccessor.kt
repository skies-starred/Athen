@file:Suppress("FunctionName")

package xyz.aerii.athen.accessors

import net.minecraft.world.entity.Entity

interface EntityRenderStateAccessor {
    fun `athen$getEntity`(): Entity?
    fun `athen$setEntity`(entity: Entity?)
}