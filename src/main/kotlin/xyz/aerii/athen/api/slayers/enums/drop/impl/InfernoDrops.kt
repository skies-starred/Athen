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

enum class InfernoDrops(
    override val display: String,
    override val item: Item,
    override val grade: SlayerDropGrade,
    override val drop: SlayerDropChance,
    override val parser: SlayerDropParserType = SlayerDropParserType.ITEM_ID,
    override val str2: String? = null
) : ISlayerDrop {
    DERELICT_ASHE("Derelict ashe", Items.GUNPOWDER, SlayerDropGrade.GUARANTEED, SlayerDropChance(null, 10_000, 100.0, SlayerDropTable.TOKEN)),
    ENCHANTED_BLAZE_POWDER("Enchanted blaze powder", Items.BLAZE_POWDER, SlayerDropGrade.OCCASIONAL, SlayerDropChance(2_351, 2_700, 21.2598, SlayerDropTable.EXTRA)),
    LAVATEARS_RUNE("Lavatears rune", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(21_660, 450, 2.3084, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTcxOTUwNDcyMTAxMiwKICAicHJvZmlsZUlkIiA6ICJmNmYxY2IxMmYzNDU0MDRlYjZlNjU2NGE2ZDlmMjU2NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJBdXJlbGl1c0dlbWluaSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84YzhjY2Q1Zjg2M2Q4MmJiMDk3YjkyNmJjNWY0Y2NhOTdiMTlmNDZlMTFiM2EzYTU5ZDAwMWFkYjg5ODg2NzczIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
    WISPS_ICE_FLAVORED_WATER("Wisp's ice water", Items.SPLASH_POTION, SlayerDropGrade.RARE, SlayerDropChance(14_270, 500, 3.5039, SlayerDropTable.MAIN)),
    ARROW_BUNDLE_MAGMA("Bundle of magma arrows", Items.PLAYER_HEAD, SlayerDropGrade.OCCASIONAL, SlayerDropChance(4_756, 1_500, 10.5116, SlayerDropTable.MAIN)),
    MANA_DISINTEGRATOR("Mana disintegrator", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(10_192, 700, 4.9054, SlayerDropTable.MAIN)),
    SCORCHED_BOOKS("Scorched books", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(17_837, 400, 2.8031, SlayerDropTable.MAIN)),
    KELVIN_INVERTER("Kelvin inverter", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(14_270, 500, 3.5039, SlayerDropTable.MAIN)),
    BLAZE_ROD_DISTILLATE("Blaze rod distillate", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(null, 840, 4.6952, SlayerDropTable.MAIN)),
    GLOWSTONE_DUST_DISTILLATE("Glowstone dust distillate", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(null, 840, 4.6952, SlayerDropTable.MAIN)),
    MAGMA_CREAM_DISTILLATE("Magma cream distillate", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(null, 840, 4.6952, SlayerDropTable.MAIN)),
    NETHER_STALK_DISTILLATE("Nether stalk distillate", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(null, 840, 4.6952, SlayerDropTable.MAIN)),
    CRUDE_GABAGOOL_DISTILLATE("Gabagool distillate", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(10_649, 840, 4.6952, SlayerDropTable.MAIN)),
    SCORCHED_POWER_CRYSTAL("Scorched power crystal", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(12_558, 600, 3.9814, SlayerDropTable.MAIN)),
    ARCHFIEND_DICE("Archfiend dice", Items.PLAYER_HEAD, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(37_675, 200, 1.3271, SlayerDropTable.MAIN)),
    ENCHANTMENT_FIRE_ASPECT("Fire aspect 3", Items.ENCHANTED_BOOK, SlayerDropGrade.EXTRAORDINARY, SlayerDropChance(32_508, 250, 1.5381, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "fire_aspect:3"),
    FIERY_BURST_RUNE("Fiery burst rune", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(243_675, 40, 0.2052, SlayerDropTable.EXTRA), SlayerDropParserType.TEXTURE, "ewogICJ0aW1lc3RhbXAiIDogMTY1Nzk5NDkxODg4NiwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQ2MjBlNGUzZDNhYmZlZDZhZDgxYTU4YTU2YmNkMDg1ZDllOWVmYzgwM2NhYmIyMWZhNmM5ZTM5NjllMmQyZSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
    FLAWED_OPAL_GEM("Flawed opal gemstone", Items.PLAYER_HEAD, SlayerDropGrade.RARE, SlayerDropChance(14_776, 550, 3.3838, SlayerDropTable.MAIN)),
    ENCHANTMENT_ULTIMATE_REITERATE("Duplex 1", Items.ENCHANTED_BOOK, SlayerDropGrade.RARE, SlayerDropChance(23_220, 350, 2.1533, SlayerDropTable.MAIN), SlayerDropParserType.ENCHANT, "duplex:1"),
    HIGH_CLASS_ARCHFIEND_DICE("High class archfiend dice", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(194_939, 50, 0.2565, SlayerDropTable.EXTRA)),
    WILSONS_ENGINEERING_PLANS("Wilson's engineering plans", Items.PAPER, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(478_058, 17, 0.1046, SlayerDropTable.MAIN)),
    SUBZERO_INVERTER("Subzero inverter", Items.PLAYER_HEAD, SlayerDropGrade.PRAY_RNGESUS, SlayerDropChance(478_058, 17, 0.1046, SlayerDropTable.MAIN)),
    FLAME_DYE("Flame dye", Items.PLAYER_HEAD, SlayerDropGrade.RNGESUS_INCARNATE, SlayerDropChance(75_000_000, 1, 0.0002, SlayerDropTable.MAIN));

    companion object : ISlayerDropParser<InfernoDrops> {
        override val set0: Set<InfernoDrops> = entries.filter { it.parser == SlayerDropParserType.ITEM_ID }.toSet()
        override val set1: Set<InfernoDrops> = entries.filter { it.parser == SlayerDropParserType.TEXTURE }.toSet()
        override val set2: Set<InfernoDrops> = entries.filter { it.parser == SlayerDropParserType.ENCHANT }.toSet()
    }
}