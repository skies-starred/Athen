package xyz.aerii.athen.api.kuudra.enums

import net.minecraft.core.BlockPos

object KuudraSupply {
    object Shop : ISupply(BlockPos(-98, 78, -113))
    object Equals : ISupply(BlockPos(-99, 78, -100))
    object Cannon : ISupply(BlockPos(-110, 78, -107))
    object X : ISupply(BlockPos(-106, 78, -113))
    object Triangle : ISupply(BlockPos(-94, 78, -106))
    object Slash : ISupply(BlockPos(-107, 78, -100))

    val every = mutableListOf(Shop, Equals, Cannon, X, Triangle, Slash)

    fun reset() =
        every.forEach(ISupply::reset)

    fun at(pos: BlockPos): ISupply? =
        every.find { it.buildPos == pos }

    open class ISupply(val buildPos: BlockPos) {
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