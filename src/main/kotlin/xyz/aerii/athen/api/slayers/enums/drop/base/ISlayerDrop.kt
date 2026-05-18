package xyz.aerii.athen.api.slayers.enums.drop.base

import net.minecraft.world.item.Item
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropChance
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

interface ISlayerDrop {
    val display: String
    val grade: SlayerDropGrade
    val drop: SlayerDropChance
    val parser: SlayerDropParserType
    val item: Item
    val str2: String?
}