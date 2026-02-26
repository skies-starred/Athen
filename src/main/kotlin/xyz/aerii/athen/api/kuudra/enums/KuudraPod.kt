package xyz.aerii.athen.api.kuudra.enums

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import xyz.aerii.athen.utils.markerAABB

enum class KuudraPod(val from: BlockPos, val to: BlockPos, val pos: BlockPos = BlockPos.ZERO) {
    Left(BlockPos(-150, 31, -173), BlockPos(-154, 24, -170), BlockPos(-153, 27, -173)),
    Middle(BlockPos(-153, 31, -153), BlockPos(-156, 25, -157), BlockPos(-156, 28, -157)),
    Right(BlockPos(-168, 31, -166), BlockPos(-170, 24, -169), BlockPos(-168, 27, -169));

    val aabb: AABB = AABB(
        minOf(from.x, to.x).toDouble(),
        minOf(from.y, to.y).toDouble(),
        minOf(from.z, to.z).toDouble(),
        maxOf(from.x, to.x) + 1.0,
        maxOf(from.y, to.y) + 1.0,
        maxOf(from.z, to.z) + 1.0
    )

    val aabb0: AABB =
        pos.markerAABB()
}