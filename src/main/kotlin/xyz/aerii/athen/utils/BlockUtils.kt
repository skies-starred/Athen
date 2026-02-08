package xyz.aerii.athen.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB

fun BlockPos.markerAABB() = AABB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)