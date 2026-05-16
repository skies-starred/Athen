@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

enum class SvenDrops(
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val display: String,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    WOLF_TOOTH(Items.GHAST_TEAR, SlayerDropGrade.GUARANTEED, "Wolf tooth"),
    HAMSTER_WHEEL(Items.OAK_TRAPDOOR, SlayerDropGrade.OCCASIONAL, "Hamster wheel"),
    SPIRIT_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Spirit rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDE1NTc5MSwKICAicHJvZmlsZUlkIiA6ICI4YjA2ZmU5ZGNjNjg0NDNmYWNmM2QzODA0NWNkNTMyNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDYXN0aWVsY3ZyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M3MzhiOGFmOGQ3Y2UxYTI2ZGM2ZDQwMTgwYjM1ODk0MDNlMTFlZjM2YTY2ZDdjNDU5MDAzNzczMjgyOTU0MmUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
    ENCHANTMENT(Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, "Critical 6", SlayerDropParserType.ENCHANT, "critical:6"),
    FURBALL(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Furball"),
    RED_CLAW_EGG(Items.MOOSHROOM_SPAWN_EGG, SlayerDropGrade.PRAY_RNGESUS, "Red claw egg"),
    COUTURE_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Couture rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwMzU0NzI1NywKICAicHJvZmlsZUlkIiA6ICJhNTdmZDE5MGZmM2U0YjBkYTEzMmY2OGUzOTU3ZjViMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJ4SGFubmFoNyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MzRmYjMyMDMyMzNlZmJhZTgyNjI4YmQ0ZmNhNzM0OGNkMDcxZTViN2I1MjQwN2YxZDFkMjc5NGUzMTc5OWZmIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    GRIZZLY_BAIT(Items.SALMON, SlayerDropGrade.PRAY_RNGESUS, "Grizzly bait"),
    OVERFLUX_CAPACITOR(Items.QUARTZ, SlayerDropGrade.PRAY_RNGESUS, "Overflux capacitor"),
    DYE_CELESTE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Celeste dye");

    companion object : ISlayerDropParser<SvenDrops> {
        override val set0: Set<SvenDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<SvenDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<SvenDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}