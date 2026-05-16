@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

enum class RevenantDrops(
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val display: String,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    REVENANT_FLESH(Items.ROTTEN_FLESH, SlayerDropGrade.GUARANTEED, "Revenant flesh"),
    FOUL_FLESH(Items.CHARCOAL, SlayerDropGrade.OCCASIONAL, "Foul flesh"),
    PESTILENCE_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Pestilence rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDEyOTIyMiwKICAicHJvZmlsZUlkIiA6ICIxNzM1MGE5OWQ3MzQ0NDBjYTY0YzJjMDU3YTNjMWM4ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJHaWxkZWRoZXJvNTY5MSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hOGM0ODExMzk1ZmJmN2Y2MjBmMDVjYzMxNzVjZWYxNTE1YWFmNzc1YmEwNGEwMTA0NTAyN2YwNjkzYTkwMTQ3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    UNDEAD_CATALYST(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Undead catalyst"),
    ENCHANTMENT_SMITE(Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, "Smite 6", SlayerDropParserType.ENCHANT, "smite:6"),
    BEHEADED_HORROR(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS,"Beheaded horror"),
    REVENANT_CATALYST(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Revenant catalyst"),
    SNAKE_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Snake rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcwOTMwNjAwOTQ3NywKICAicHJvZmlsZUlkIiA6ICI1ZjQ5N2JmZDQwODU0NjRhOTNiMTRjN2Y3OTc5ZGYyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUVEs5ODciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmM0YTY1YzY4OWIyZDM2NDA5MTAwYTYwYzJhYjhkM2QwYTY3Y2U5NGVlYTNjMWY3YWM5NzRmZDg5MzU2OGI1ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    FESTERING_MAGGOT(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Festering maggot"),
    REVENANT_VISCERA(Items.COOKED_PORKCHOP, SlayerDropGrade.OCCASIONAL, "Revenant viscera"),
    SCYTHE_BLADE(Items.DIAMOND, SlayerDropGrade.PRAY_RNGESUS, "Scythe blade"),
    SEVERED_HAND(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Severed hand"),
    SHARD_OF_THE_SHREDDED(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Shard of the shredded"),
    WARDEN_HEART(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Warden heart"),
    DYE_MATCHA(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Matcha dye");

    companion object : ISlayerDropParser<RevenantDrops> {
        override val set0: Set<RevenantDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<RevenantDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<RevenantDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}