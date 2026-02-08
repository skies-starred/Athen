@file:Suppress("FunctionName", "CAST_NEVER_SUCCEEDS")

package xyz.aerii.athen.accessors

import net.minecraft.world.item.ItemStack

interface ItemStackAccessor {
    fun `athen$invalidate`()
}

fun ItemStack.invalidate() = (this as? ItemStackAccessor)?.`athen$invalidate`()