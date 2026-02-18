@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.api.kuudra.enums

import net.minecraft.core.BlockPos
import xyz.aerii.athen.utils.markerAABB

object KuudraSupply {
    private const val y = 78

    const val supply = "ewogICJ0aW1lc3RhbXAiIDogMTU5NDAyOTYxNjQyNCwKICAicHJvZmlsZUlkIiA6ICJkZGVkNTZlMWVmOGI0MGZlOGFkMTYyOTIwZjdhZWNkYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEaXNjb3JkQXBwIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI0YmJmZDlkODRmNDI0NTZjZDAyYTRiYWE1Y2QwNTRiY2VkMGRkYjJkMWM4MzIxYzgzZTVkNjY3Y2Q4NTU3NWEiCiAgICB9CiAgfQp9"
    const val fuel = "ewogICJ0aW1lc3RhbXAiIDogMTcyMDAyOTIzMDk5OSwKICAicHJvZmlsZUlkIiA6ICJkM2Y5MjEyMjY3YzM0YzEwYWNjOWZkNGI5MDFkYjI0ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJkYXl3ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDcyZGViMWFiMDAzM2I0MmIwYTEyZWZjZjQ4M2YwZmJhMjZkYzUxZGVkMzkxOWViYWRiNzBmOTY1N2ExZjYxIgogICAgfQogIH0KfQ=="

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