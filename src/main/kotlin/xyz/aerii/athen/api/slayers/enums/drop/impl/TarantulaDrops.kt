@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

enum class TarantulaDrops(
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val display: String,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    TARANTULA_WEB(Items.STRING, SlayerDropGrade.GUARANTEED, "Tarantula web"),
    TOXIC_ARROW_POISON(Items.LIME_DYE, SlayerDropGrade.OCCASIONAL, "Toxic arrow poison"),
    BITE_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Bite rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDQ3MDQ3NywKICAicHJvZmlsZUlkIiA6ICIzOTVkZTJlYjVjNjU0ZmRkOWQ2NDAwY2JhNmNmNjFhNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFyZXN0ZXZlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQzYTFhZDRmY2M0MmZiNjNjNjgxMzI4ZTQyZDYzYzgzY2ExOTNiMzMzYWYyYTQyNjcyOGEyNWE4Y2M2MDA2OTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
    DARKNESS_WITHIN_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Darkness within rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwMzQyNTYyNywKICAicHJvZmlsZUlkIiA6ICI2ZjhlYWI1MTVmNTc0MmRhOWYxZDYzMzY1ODAxMDU4YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJDaW5kZXJGb3hfMjAwNiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iOGQwNWZkNGZjNmZkMWNiMzJjY2JhYmU4NzA0MGEyOTZiNTE0MjdiNGZhNWNlNTdiZTViNDExZDg2ZTIzNGM4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    SPIDER_CATALYST(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Spider catalyst"),
    TARANTULA_SILK(Items.COBWEB, SlayerDropGrade.OCCASIONAL, "Tarantula silk"),
    ENCHANTMENT_BANE_OF_ARTHROPODS(Items.ENCHANTED_BOOK, SlayerDropGrade.PRAY_RNGESUS, "Bane of Arthropods 6", SlayerDropParserType.ENCHANT, "bane_of_arthrpods:6"),
    TARANTULA_CATALYST(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Tarantula catalyst"),
    FLY_SWATTER(Items.GOLDEN_SHOVEL, SlayerDropGrade.PRAY_RNGESUS, "Fly swatter"),
    VIAL_OF_VENOM(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Vial of venom"),
    TARANTULA_TALISMAN(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Tarantula talisman"),
    DIGESTED_MOSQUITO(Items.ROTTEN_FLESH, SlayerDropGrade.PRAY_RNGESUS, "Digested mosquito"),
    SHRIVELED_WASP(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Shriveled wasp"),
    ENSNARED_SNAIL(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Ensnared snail"),
    PRIMORDIAL_EYE(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Primordial eye"),
    DYE_BRICK_RED(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Brick red dye");

    companion object : ISlayerDropParser<TarantulaDrops> {
        override val set0: Set<TarantulaDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<TarantulaDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<TarantulaDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}