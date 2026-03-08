@file:Suppress("unused")

package xyz.aerii.athen.utils

import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Texter.literal
import java.util.*

@JvmField
val EMPTY_UUID = UUID(0, 0)

@JvmField
val EMPTY_COMPONENT = "".literal() as Component

@JvmField
val ALWAYS_TRUE = React(true).immutable()

@JvmField
val ALWAYS_FALSE = React(false).immutable()

@JvmField
val ZERO_PAIR = 0 to 0

@JvmField
val ZERO_AABB = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)