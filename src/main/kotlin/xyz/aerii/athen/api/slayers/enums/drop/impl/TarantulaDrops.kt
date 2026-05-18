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

enum class TarantulaDrops(
    override val display: String,
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val drop: SlayerDropChance,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    TARANTULA_WEB("Tarantula web", Items.STRING, SlayerDropGrade.GUARANTEED, SlayerDropChance(null, 10_000, 100.0, SlayerDropTable.TOKEN)),
    TOXIC_ARROW_POISON("Toxic arrow poison", Items.LIME_DYE, SlayerDropGrade.OCCASIONAL, SlayerDropChance(3_277, 1_800, 15.2542, SlayerDropTable.MAIN)),
    BITE_RUNE("Bite rune", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(7_657, 833, 6.5292, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDQ3MDQ3NywKICAicHJvZmlsZUlkIiA6ICIzOTVkZTJlYjVjNjU0ZmRkOWQ2NDAwY2JhNmNmNjFhNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFyZXN0ZXZlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQzYTFhZDRmY2M0MmZiNjNjNjgxMzI4ZTQyZDYzYzgzY2ExOTNiMzMzYWYyYTQyNjcyOGEyNWE4Y2M2MDA2OTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
    DARKNESS_WITHIN_RUNE("Darkness within rune", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(74_930, 100, 0.6673, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwMzQyNTYyNywKICAicHJvZmlsZUlkIiA6ICI2ZjhlYWI1MTVmNTc0MmRhOWYxZDYzMzY1ODAxMDU4YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJDaW5kZXJGb3hfMjAwNiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iOGQwNWZkNGZjNmZkMWNiMzJjY2JhYmU4NzA0MGEyOTZiNTE0MjdiNGZhNWNlNTdiZTViNDExZDg2ZTIzNGM4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    SPIDER_CATALYST("Spider catalyst", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(24_250, 25, 2.0619, SlayerDropTable.MAIN)),
    TARANTULA_SILK("Tarantula silk", Items.COBWEB, SlayerDropGrade.OCCASIONAL, SlayerDropChance(3_513, 2_000, 14.2318, SlayerDropTable.MAIN)),
    ENCHANTMENT_BANE_OF_ARTHROPODS("Bane of Arthropods 6", Items.ENCHANTED_BOOK, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(120_269, 50, 0.4157, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "bane_of_arthrpods:6"),
    TARANTULA_CATALYST("Tarantula catalyst", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(117_108, 60, 0.427, SlayerDropTable.MAIN)),
    FLY_SWATTER("Fly swatter", Items.GOLDEN_SHOVEL, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(234_216, 30, 0.2135, SlayerDropTable.MAIN)),
    VIAL_OF_VENOM("Vial of venom", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(351_325, 20, 0.1423, SlayerDropTable.MAIN)),
    TARANTULA_TALISMAN("Tarantula talisman", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(234_216, 30, 0.2135, SlayerDropTable.MAIN)),
    DIGESTED_MOSQUITO("Digested mosquito", Items.ROTTEN_FLESH, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(702_650, 10, 0.0712, SlayerDropTable.MAIN)),
    SHRIVELED_WASP("Shriveled wasp", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(351_325, 20, 0.1423, SlayerDropTable.MAIN)),
    ENSNARED_SNAIL("Ensnared snail", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(1_171_083, 6, 0.0427, SlayerDropTable.MAIN)),
    PRIMORDIAL_EYE("Primordial eye", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(3_513_250, 2, 0.0142, SlayerDropTable.MAIN)),
    DYE_BRICK_RED("Brick red dye", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(75_000_000, 1, 0.0004, SlayerDropTable.MAIN));

    companion object : ISlayerDropParser<TarantulaDrops> {
        override val set0: Set<TarantulaDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<TarantulaDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<TarantulaDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}