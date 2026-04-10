package xyz.aerii.athen.api.kuudra.enums

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.monster.Giant
import net.minecraft.world.phys.AABB
import xyz.aerii.library.api.ZERO_AABB
import kotlin.math.cos
import kotlin.math.sin

class AbstractSupply(val entity: Giant) {
    private var x1: Double = 0.0
    private var z1: Double = 0.0

    private var x0: Double = 0.0
    private var z0: Double = 0.0

    private val x: Double
        get() = entity.x + x0

    private val z: Double
        get() = entity.z + z0

    var nearby: Boolean = false
        internal set

    var blockPos: BlockPos = BlockPos.ZERO
        private set

    var aabb: AABB = ZERO_AABB
        private set

    var radAABB: AABB = ZERO_AABB
        private set

    init {
        val rad = Math.toRadians(entity.yRot + 130.0)
        x0 = 3.7 * cos(rad)
        z0 = 3.7 * sin(rad)
        pos()
    }

    fun pos() {
        if (x1 == entity.x && z1 == entity.z) return

        x1 = entity.x
        z1 = entity.z

        aabb = fn()
        radAABB = fn().inflate(4.0, 5.0, 4.0)
        blockPos = fn0()
    }

    private fun fn(): AABB = AABB(
        x - 1.0, 74.5, z,
        x + 2.0, 76.5, z + 3.0
    )

    private fun fn0(): BlockPos {
        val x = (aabb.minX + aabb.maxX) / 2.0
        val y = (aabb.minY + aabb.maxY) / 2.0
        val z = (aabb.minZ + aabb.maxZ) / 2.0
        return BlockPos(x.toInt(), y.toInt(), z.toInt())
    }
}