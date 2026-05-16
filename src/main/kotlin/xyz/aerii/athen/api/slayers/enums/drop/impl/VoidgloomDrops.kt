@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

enum class VoidgloomDrops(
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val display: String,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    NULL_SPHERE(Items.FIREWORK_STAR, SlayerDropGrade.GUARANTEED, "Null sphere"),
    TWILIGHT_ARROW_POISON(Items.PURPLE_DYE, SlayerDropGrade.OCCASIONAL, "Twilight arrow poison"),
    ENDERSNAKE_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Endersnake rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwMzYzMjkyMiwKICAicHJvZmlsZUlkIiA6ICI1ZjU5NmViY2JlOTQ0NmQxYmI0M2JlNGYzZjRiOGJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWlsMHNzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2MzYTlhY2JiN2QzZDQ5YjFkNTRkMjYxMTExMDRkMGRhNTdkOGI0YWIzNzg4NWI0YmJkMjQwYWM3MTA3NGNhZDIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
    SUMMONING_EYE(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Summoning eye"),
    ENCHANTMENT_MANA_STEAL(Items.ENCHANTED_BOOK, SlayerDropGrade.RARE, "Mana Steal 1", SlayerDropParserType.ENCHANT, "mana_steal:1"),
    TRANSMISSION_TUNER(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Transmission tuner"),
    NULL_ATOM(Items.OAK_BUTTON, SlayerDropGrade.RARE, "Null atom"),
    HAZMAT_ENDERMAN(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Hazmat enderman"),
    POCKET_ESPRESSO_MACHINE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Pocket espresso machine"),
    ENCHANTMENT_SMARTY_PANTS(Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, "Smarty Pants 1", SlayerDropParserType.ENCHANT, "smarty_pants:1"),
    END_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "End rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTYyODYzNTU3NDI0OSwKICAicHJvZmlsZUlkIiA6ICI0ZTMwZjUwZTdiYWU0M2YzYWZkMmE3NDUyY2ViZTI5YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJfdG9tYXRvel8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2IxMWZiOTBkYjdmNTdiZWI0MzU5NTQwMTNiMWM3ZWY3NzZjNmJkOTZjYmYzMzA4YWE4ZWJhYzI5NTkxZWJiZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    HANDY_BLOOD_CHALICE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Handy blood chalice"),
    SINFUL_DICE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Sinful dice"),
    EXCEEDINGLY_RARE_ENDER_ARTIFACT_UPGRADE(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Ender artifact upgrade"),
    PET_SKIN_ENDERMAN_SLAYER(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Void conqueror enderman skin"),
    ETHERWARP_MERGER(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Etherwarp merger"),
    JUDGEMENT_CORE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Judgement core"),
    ENCHANT_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Enchant rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTY3MDQ3OTgyMTg3NCwKICAicHJvZmlsZUlkIiA6ICJmODFhNzJhZWZjMjY0MjU0YTQ5NzE0OWYzMjJiZjJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEZXJsYW5fODgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlmZmFjZWM2ZWU1YTIzZDljYjI0YTJmZTlkYzE1YjI0NDg4ZjVmNzEwMDY5MjQ1NjBiZjEyMTQ4NDIxYWU2ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    ENDSTONE_IDOL(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Endstone idol"),
    DYE_BYZANTIUM(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Byzantium dye");

    companion object : ISlayerDropParser<VoidgloomDrops> {
        override val set0: Set<VoidgloomDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<VoidgloomDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<VoidgloomDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}