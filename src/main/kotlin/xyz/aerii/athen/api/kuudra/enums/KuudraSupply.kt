@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.api.kuudra.enums

import net.minecraft.core.BlockPos
import xyz.aerii.athen.utils.markerAABB

object KuudraSupply {
    private const val y = 78

    object Shop : ISupply(BlockPos(-98, y, -113))
    object Equals : ISupply(BlockPos(-99, y, -100))
    object Cannon : ISupply(BlockPos(-110, y, -107))
    object X : ISupply(BlockPos(-106, y, -113))
    object Triangle : ISupply(BlockPos(-94, y, -106))
    object Slash : ISupply(BlockPos(-107, y, -100))

    val every = mutableListOf(Shop, Equals, Cannon, X, Triangle, Slash)

    fun reset() =
        every.forEach(ISupply::reset)

    fun at(pos: BlockPos): ISupply? =
        every.find { it.buildPos == pos }

    open class ISupply(val buildPos: BlockPos) {
        val buildAABB = buildPos.markerAABB()

        var built = false
        var active = false

        fun reset() {
            built = false
            active = false
        }

        override fun toString(): String {
            return "ISupply(pos=$buildPos, built=$built, active=$active)"
        }
    }
}