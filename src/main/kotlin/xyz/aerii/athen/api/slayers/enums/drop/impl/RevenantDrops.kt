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

enum class RevenantDrops(
    override val display: String,
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val drop: SlayerDropChance,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    REVENANT_FLESH("Revenant flesh", Items.ROTTEN_FLESH, SlayerDropGrade.GUARANTEED, SlayerDropChance(null, 10_000, 100.0, SlayerDropTable.TOKEN)),
    FOUL_FLESH("Foul flesh", Items.CHARCOAL, SlayerDropGrade.OCCASIONAL, SlayerDropChance(3_093, 2_000, 16.1616, SlayerDropTable.MAIN)),
    PESTILENCE_RUNE("Pestilence rune", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(7_977, 833, 6.2679, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDEyOTIyMiwKICAicHJvZmlsZUlkIiA6ICIxNzM1MGE5OWQ3MzQ0NDBjYTY0YzJjMDU3YTNjMWM4ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJHaWxkZWRoZXJvNTY5MSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hOGM0ODExMzk1ZmJmN2Y2MjBmMDVjYzMxNzVjZWYxNTE1YWFmNzc1YmEwNGEwMTA0NTAyN2YwNjkzYTkwMTQ3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    UNDEAD_CATALYST("Undead catalyst", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(24_750, 250, 2.0202, SlayerDropTable.MAIN)),
    ENCHANTMENT_SMITE("Smite 6", Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(124_370, 50, 0.402, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "smite:6"),
    BEHEADED_HORROR("Beheaded horror", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(310_925, 20, 0.1608, SlayerDropTable.MAIN)),
    REVENANT_CATALYST("Revenant catalyst", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(49_500, 125, 1.0101, SlayerDropTable.MAIN)),
    SNAKE_RUNE("Snake rune", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(332_250, 20, 0.1505, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcwOTMwNjAwOTQ3NywKICAicHJvZmlsZUlkIiA6ICI1ZjQ5N2JmZDQwODU0NjRhOTNiMTRjN2Y3OTc5ZGYyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUVGs5ODciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmM0YTY1YzY4OWIyZDM2NDA5MTAwYTYwYzJhYjhkM2QwYTY3Y2U5NGVlYTNjMWY3YWM5NzRmZDg5MzU2OGI1ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    FESTERING_MAGGOT("Festering maggot", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(367_424, 20, 0.1361, SlayerDropTable.EXTRA)),
    REVENANT_VISCERA("Revenant viscera", Items.COOKED_PORKCHOP, SlayerDropGrade.OCCASIONAL, SlayerDropChance(3_674, 2_000, 13.6082, SlayerDropTable.MAIN)),
    SCYTHE_BLADE("Scythe blade", Items.DIAMOND, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(489_900, 15, 0.1021, SlayerDropTable.MAIN)),
    SEVERED_HAND("Severed hand", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(1_049_785, 7, 0.0476, SlayerDropTable.MAIN)),
    SHARD_OF_THE_SHREDDED("Shard of the shredded", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(918_562, 8, 0.1021, SlayerDropTable.MAIN)),
    WARDEN_HEART("Warden heart", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(3_674_250, 2, 0.0186, SlayerDropTable.MAIN)),
    DYE_MATCHA("Matcha dye", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(75_000_000, 1, 0.0008, SlayerDropTable.MAIN));

    companion object : ISlayerDropParser<RevenantDrops> {
        override val set0: Set<RevenantDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<RevenantDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<RevenantDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}