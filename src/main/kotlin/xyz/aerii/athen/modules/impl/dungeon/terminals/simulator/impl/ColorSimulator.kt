package xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.impl

import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.ITerminalSim
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.SimulatorMenu
import xyz.aerii.athen.utils.hasGlint
import kotlin.random.Random

class ColorSimulator(
    private val t: Pair<DyeColor, List<Item>> = all.random()
) : ITerminalSim(
    TerminalType.COLORS,
    component = "Select all the ${t.first.name.replace("LIGHT_GRAY", "SILVER").replace("_", " ")} items!".literal()
) {
    override fun s(): Map<Int, ItemStack> = buildMap {
        val g = listOf(10..16, 19..25, 28..34, 37..43).random().random()

        val a = ArrayList<Item>()
        for (p in all) if (p != t) a.addAll(p.second)

        for (row in 1..4) for (col in 1..7) {
            val idx = row * 9 + col
            val pool = if (idx == g || Random.nextInt(4) == 0) t.second else a
            put(idx, ItemStack(pool[Random.nextInt(pool.size)]))
        }
    }

    override fun click(slot: Slot, button: Int) {
        val stack = slot.item?.takeIf { it.item in t.second } ?: return "Invalid item! Does not match the color!".modMessage()
        mapOf(slot.containerSlot to stack.apply { set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true) }).a()
        if (slots.none { it.item?.item in t.second && !it.item.hasGlint() }) SimulatorMenu.a()
    }
}

private val all = listOf(
    DyeColor.WHITE to listOf(Items.WHITE_STAINED_GLASS, Items.WHITE_WOOL, Items.WHITE_CONCRETE, Items.BONE_MEAL, Items.WHITE_DYE),
    DyeColor.BLUE to listOf(Items.BLUE_STAINED_GLASS, Items.BLUE_WOOL, Items.BLUE_CONCRETE, Items.LAPIS_LAZULI, Items.BLUE_DYE),
    DyeColor.BLACK to listOf(Items.BLACK_STAINED_GLASS, Items.BLACK_WOOL, Items.BLACK_CONCRETE, Items.INK_SAC, Items.BLACK_DYE),
    DyeColor.BROWN to listOf(Items.BROWN_STAINED_GLASS, Items.BROWN_WOOL, Items.BROWN_CONCRETE, Items.COCOA_BEANS, Items.BROWN_DYE),
    DyeColor.RED to listOf(Items.RED_STAINED_GLASS, Items.RED_WOOL, Items.RED_CONCRETE, Items.RED_DYE),
    DyeColor.LIME to listOf(Items.LIME_STAINED_GLASS, Items.LIME_WOOL, Items.LIME_CONCRETE, Items.LIME_DYE),
    DyeColor.GREEN to listOf(Items.GREEN_STAINED_GLASS, Items.GREEN_WOOL, Items.GREEN_CONCRETE, Items.GREEN_DYE),
    DyeColor.YELLOW to listOf(Items.YELLOW_STAINED_GLASS, Items.YELLOW_WOOL, Items.YELLOW_CONCRETE, Items.YELLOW_DYE),
    DyeColor.ORANGE to listOf(Items.ORANGE_STAINED_GLASS, Items.ORANGE_WOOL, Items.ORANGE_CONCRETE, Items.ORANGE_DYE),
    DyeColor.MAGENTA to listOf(Items.MAGENTA_STAINED_GLASS, Items.MAGENTA_WOOL, Items.MAGENTA_CONCRETE, Items.MAGENTA_DYE),
    DyeColor.CYAN to listOf(Items.CYAN_STAINED_GLASS, Items.CYAN_WOOL, Items.CYAN_CONCRETE, Items.CYAN_DYE),
    DyeColor.PINK to listOf(Items.PINK_STAINED_GLASS, Items.PINK_WOOL, Items.PINK_CONCRETE, Items.PINK_DYE),
    DyeColor.GRAY to listOf(Items.GRAY_STAINED_GLASS, Items.GRAY_WOOL, Items.GRAY_CONCRETE, Items.GRAY_DYE),
    DyeColor.LIGHT_GRAY to listOf(Items.LIGHT_GRAY_STAINED_GLASS, Items.LIGHT_GRAY_WOOL, Items.LIGHT_GRAY_CONCRETE, Items.LIGHT_GRAY_DYE),
    DyeColor.PURPLE to listOf(Items.PURPLE_STAINED_GLASS, Items.PURPLE_WOOL, Items.PURPLE_CONCRETE, Items.PURPLE_DYE)
)