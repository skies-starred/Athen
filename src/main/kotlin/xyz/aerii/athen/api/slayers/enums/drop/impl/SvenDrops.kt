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

enum class SvenDrops(
    override val display: String,
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val drop: SlayerDropChance,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    WOLF_TOOTH("Wolf tooth", Items.GHAST_TEAR, SlayerDropGrade.GUARANTEED, SlayerDropChance(null, 10_000, 100.0, SlayerDropTable.TOKEN)),
    HAMSTER_WHEEL("Hamster wheel", Items.OAK_TRAPDOOR, SlayerDropGrade.OCCASIONAL, SlayerDropChance(3_000, 2_000, 16.6667, SlayerDropTable.MAIN)),
    SPIRIT_RUNE("Spirit rune", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(7_917, 833, 6.3154, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDE1NTc5MSwKICAicHJvZmlsZUlkIiA6ICI4YjA2ZmU5ZGNjNjg0NDNmYWNmM2QzODA0NWNkNTMyNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDYXN0aWVsY3ZyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M3MzhiOGFmOGQ3Y2UxYTI2ZGM2ZDQwMTgwYjM1ODk0MDNlMTFlZjM2YTY2ZDdjNDU5MDAzNzczMjgyOTU0MmUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
    ENCHANTMENT_CRITICAL("Critical 6", Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(61_634, 100, 0.8112, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "critical:6"),
    FURBALL("Furball", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(30_637, 200, 1.632, SlayerDropTable.MAIN)),
    RED_CLAW_EGG("Red claw egg", Items.MOOSHROOM_SPAWN_EGG, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(410_900, 15, 0.1217, SlayerDropTable.MAIN)),
    COUTURE_RUNE("Couture rune", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(219_833, 30, 0.2274, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwMzU0NzI1NywKICAicHJvZmlsZUlkIiA6ICJhNTdmZDE5MGZmM2U0YjBkYTEzMmY2OGUzOTU3ZjViMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJ4SGFubmFoNyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MzRmYjMyMDMyMzNlZmJhZTgyNjI4YmQ0ZmNhNzM0OGNkMDcxZTViN2I1MjQwN2YxZDFkMjc5NGUzMTc5OWZmIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    GRIZZLY_BAIT("Grizzly bait", Items.SALMON, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(880_500, 7, 0.0568, SlayerDropTable.MAIN)),
    OVERFLUX_CAPACITOR("Overflux capacitor", Items.QUARTZ, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(1_232_700, 5, 0.0406, SlayerDropTable.MAIN)),
    DYE_CELESTE("Celeste dye", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(75_000_000, 1, 0.0002, SlayerDropTable.MAIN));

    companion object : ISlayerDropParser<SvenDrops> {
        override val set0: Set<SvenDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<SvenDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<SvenDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}