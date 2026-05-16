@file:Suppress("Unused")

package xyz.aerii.athen.api.slayers.enums.drop.impl

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDrop
import xyz.aerii.athen.api.slayers.enums.drop.base.ISlayerDropParser
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropGrade
import xyz.aerii.athen.api.slayers.enums.drop.data.SlayerDropParserType

enum class InfernoDrops(
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val display: String,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    DERELICT_ASHE(Items.GUNPOWDER, SlayerDropGrade.GUARANTEED, "Derelict ashe"),
    ENCHANTED_BLAZE_POWDER(Items.BLAZE_POWDER, SlayerDropGrade.OCCASIONAL, "Enchanted blaze powder"),
    LAVATEARS_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Lavatears rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDcyMTAxMiwKICAicHJvZmlsZUlkIiA6ICJmNmYxY2IxMmYzNDU0MDRlYjZlNjU2NGE2ZDlmMjU2NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJBdXJlbGl1c0dlbWluaSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84YzhjY2Q1Zjg2M2Q4MmJiMDk3YjkyNmJjNWY0Y2NhOTdiMTlmNDZlMTFiM2EzYTU5ZDAwMWFkYjg5ODg2NzczIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    WISPS_ICE_FLAVORED_WATER(Items.SPLASH_POTION, SlayerDropGrade.RARE, "Wisp's ice water"),
    ARROW_BUNDLE_MAGMA(Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, "Bundle of magma arrows"),
    MANA_DISINTEGRATOR(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Mana disintegrator"),
    SCORCHED_BOOKS(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Scorched books"),
    KELVIN_INVERTER(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Kelvin inverter"),
    BLAZE_ROD_DISTILLATE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Blaze rod distillate"),
    GLOWSTONE_DUST_DISTILLATE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Glowstone dust distillate"),
    MAGMA_CREAM_DISTILLATE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Magma cream distillate"),
    NETHER_STALK_DISTILLATE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Nether stalk distillate"),
    CRUDE_GABAGOOL_DISTILLATE(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Gabagool distillate"),
    SCORCHED_POWER_CRYSTAL(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Scorched power crystal"),
    ARCHFIEND_DICE(Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, "Archfiend dice"),
    ENCHANTMENT_FIRE_ASPECT(Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, "Fire aspect 3", SlayerDropParserType.ENCHANT, "fire_aspect:3"),
    FIERY_BURST_RUNE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Fiery burst rune", SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTY1Nzk5NDkxODg4NiwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQ2MjBlNGUzZDNhYmZlZDZhZDgxYTU4YTU2YmNkMDg1ZDllOWVmYzgwM2NhYmIyMWZhNmM5ZTM5NjllMmQyZSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    FLAWED_OPAL_GEM(Items.PLAYER_HEAD, SlayerDropGrade.RARE, "Flawed opal gemstone"),
    ENCHANTMENT_ULTIMATE_REITERATE(Items.ENCHANTED_BOOK, SlayerDropGrade.RARE, "Duplex 1"),
    HIGH_CLASS_ARCHFIEND_DICE(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "High class archfiend dice"),
    WILSONS_ENGINEERING_PLANS(Items.PAPER, SlayerDropGrade.PRAY_RNGESUS, "Wilson's engineering plans"),
    SUBZERO_INVERTER(Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, "Subzero inverter"),
    FLAME_DYE(Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, "Flame dye");

    companion object : ISlayerDropParser<InfernoDrops> {
        override val set0: Set<InfernoDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<InfernoDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<InfernoDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}