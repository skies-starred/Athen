package xyz.aerii.athen.api.slayers.enums.drop.data

data class SlayerDropChance(
    val xp: Long?,
    val weight: Int,
    val chance: Double,
    val table: SlayerDropTable
)