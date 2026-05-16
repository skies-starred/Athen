@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

enum class VampireDrops(
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val display: String,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    COVEN_SEAL(Items.NETHER_WART, SlayerDropGrade.GUARANTEED, "Coven seal"),
    ENCHANTED_BOOK_BUNDLE_QUANTUM(Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, "Bundle of quantum book"),
    SOULTWIST_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, "Soultwist rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTY4MTUxOTM1Mjc5NCwKICAicHJvZmlsZUlkIiA6ICI4N2YzOGM1MWE4Yzc0MmNmYTY2YTgxNWExZTI2NzMzYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZWR3YXJzQ3V0aWUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjVmZmRmYmQ0OTBmYzczMTBkNjFhMWM0YzM1YTRlMGNkMmY5ZmNjYzEyMzljNmE0YmNkN2RlYzA1ZTI1ZWE2NyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    BUBBA_BLISTER(Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, "Bubba blister"),
    CHOCOLATE_CHIP(Items.COOKIE, SlayerDropGrade.OCCASIONAL, "Chocolate chip"),
    GUARDIAN_LUCKY_BLOCK(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Guardian lucky block"),
    MCGRUBBER_BURGER(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "McGrubber burger"),
    UNFANGED_VAMPIRE_PART(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Unfanged vampire part"),
    ENCHANTMENT_ULTIMATE_THE_ONE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Bundle of The One book"),
    DYE_SANGRIA(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Sangria dye");

    companion object : ISlayerDropParser<VampireDrops> {
        override val set0: Set<VampireDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<VampireDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<VampireDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}