@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropChance
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropTable

enum class VampireDrops(
    override val display: String,
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val drop: SlayerDropChance,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    COVEN_SEAL("Coven seal", Items.NETHER_WART, SlayerDropGrade.GUARANTEED, SlayerDropChance(null, 100, 100.0, SlayerDropTable.TOKEN)),
    ENCHANTED_BOOK_BUNDLE_QUANTUM("Bundle of quantum book", Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, SlayerDropChance(1_687, 20, 13.3333, SlayerDropTable.MAIN)),
    SOULTWIST_RUNE("Soultwist rune", Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, SlayerDropChance(1_912, 20, 11.7647, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTY4MTUxOTM1Mjc5NCwKICAicHJvZmlsZUlkIiA6ICI4N2YzOGM1MWE4Yzc0MmNmYTY2YTgxNWExZTI2NzMzYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZWR3YXJzQ3V0aWUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjVmZmRmYmQ0OTBmYzczMTBkNjFhMWM0YzM1YTRlMGNkMmY5ZmNjYzEyMzljNmE0YmNkN2RlYzA1ZTI1ZWE2NyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    BUBBA_BLISTER("Bubba blister", Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, SlayerDropChance(2_250, 15, 10.0, SlayerDropTable.MAIN)),
    CHOCOLATE_CHIP("Chocolate chip", Items.COOKIE, SlayerDropGrade.OCCASIONAL, SlayerDropChance(2_250, 15, 10.0, SlayerDropTable.MAIN)),
    GUARDIAN_LUCKY_BLOCK("Guardian lucky block", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(3_600, 10, 6.25, SlayerDropTable.MAIN)),
    MCGRUBBER_BURGER("McGrubber burger", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(18_450, 2, 1.2195, SlayerDropTable.MAIN)),
    UNFANGED_VAMPIRE_PART("Unfanged vampire part", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(18_450, 2, 1.2195, SlayerDropTable.MAIN)),
    ENCHANTMENT_ULTIMATE_THE_ONE("Bundle of The One book", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(12_525, 3, 1.7964, SlayerDropTable.MAIN)),
    DYE_SANGRIA("Sangria dye", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(1_687, 1, 0.01, SlayerDropTable.MAIN));

    companion object : ISlayerDropParser<VampireDrops> {
        override val set0: Set<VampireDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<VampireDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<VampireDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}