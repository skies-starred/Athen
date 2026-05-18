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

enum class VoidgloomDrops(
    override val display: String,
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val drop: SlayerDropChance,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    NULL_SPHERE("Null sphere", Items.FIREWORK_STAR, SlayerDropGrade.GUARANTEED, SlayerDropChance(null, 10_000, 100.0, SlayerDropTable.TOKEN)),
    TWILIGHT_ARROW_POISON("Twilight arrow poison", Items.PURPLE_DYE, SlayerDropGrade.OCCASIONAL, SlayerDropChance(3_300, 1_800, 15.1515, SlayerDropTable.MAIN)),
    ENDERSNAKE_RUNE("Endersnake rune", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(9_438, 800, 5.2977, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwMzYzMjkyMiwKICAicHJvZmlsZUlkIiA6ICI1ZjU5NmViY2JlOTQ0NmQxYmI0M2JlNGYzZjRiOGJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWlsMHNzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2MzYTlhY2JiN2QzZDQ5YjFkNTRkMjYxMTExMDRkMGRhNTdkOGI0YWIzNzg4NWI0YmJkMjQwYWM3MTA3NGNhZDIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
    SUMMONING_EYE("Summoning eye", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(74_250, 80, 0.6734, SlayerDropTable.MAIN)),
    ENCHANTMENT_MANA_STEAL("Mana Steal 1", Items.ENCHANTED_BOOK, SlayerDropGrade.RARE, SlayerDropChance(11_183, 600, 4.4709, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "mana_steal:1"),
    TRANSMISSION_TUNER("Transmission tuner", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(22_366, 300, 2.2355, SlayerDropTable.MAIN)),
    NULL_ATOM("Null atom", Items.OAK_BUTTON, SlayerDropGrade.RARE, SlayerDropChance(10_120, 700, 4.9404, SlayerDropTable.MAIN)),
    HAZMAT_ENDERMAN("Hazmat enderman", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(32_202, 220, 1.5527, SlayerDropTable.MAIN)),
    POCKET_ESPRESSO_MACHINE("Pocket espresso machine", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(128_809, 55, 0.3882, SlayerDropTable.MAIN)),
    ENCHANTMENT_SMARTY_PANTS("Smarty Pants 1", Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(28_338, 250, 1.7644, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "smarty_pants:1"),
    END_RUNE("End rune", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(75_505, 100, 0.6622, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTYyODYzNTU3NDI0OSwKICAicHJvZmlsZUlkIiA6ICI0ZTMwZjUwZTdiYWU0M2YzYWZkMmE3NDUyY2ViZTI5YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJfdG9tYXRvel8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2IxMWZiOTBkYjdmNTdiZWI0MzU5NTQwMTNiMWM3ZWY3NzZjNmJkOTZjYmYzMzA4YWE4ZWJhYzI5NTkxZWJiZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    HANDY_BLOOD_CHALICE("Handy blood chalice", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(283_380, 25, 0.1764, SlayerDropTable.MAIN)),
    SINFUL_DICE("Sinful dice", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(108_992, 65, 0.4587, SlayerDropTable.MAIN)),
    EXCEEDINGLY_RARE_ENDER_ARTIFACT_UPGRADE("Ender artifact upgrade", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(1_771_125, 4, 0.0282, SlayerDropTable.MAIN)),
    PET_SKIN_ENDERMAN_SLAYER("Void conqueror enderman skin", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(302_020, 25, 0.1656, SlayerDropTable.EXTRA)),
    ETHERWARP_MERGER("Etherwarp merger", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(118_075, 60, 0.4235, SlayerDropTable.MAIN)),
    JUDGEMENT_CORE("Judgement core", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(885_562, 8, 0.0565, SlayerDropTable.MAIN)),
    ENCHANT_RUNE("Enchant rune", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(1_078_642, 7, 0.0464, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTY3MDQ3OTgyMTg3NCwKICAicHJvZmlsZUlkIiA6ICJmODFhNzJhZWZjMjY0MjU0YTQ5NzE0OWYzMjJiZjJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEZXJsYW5fODgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlmZmFjZWM2ZWU1YTIzZDljYjI0YTJmZTlkYzE1YjI0NDg4ZjVmNzEwMDY5MjQ1NjBiZjEyMTQ4NDIxYWU2ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    ENDSTONE_IDOL("Endstone idol", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(3_542_250, 2, 0.0141, SlayerDropTable.MAIN)),
    DYE_BYZANTIUM("Byzantium dye", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(75_000_000, 1, 0.0002, SlayerDropTable.MAIN));

    companion object : ISlayerDropParser<VoidgloomDrops> {
        override val set0: Set<VoidgloomDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<VoidgloomDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<VoidgloomDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}