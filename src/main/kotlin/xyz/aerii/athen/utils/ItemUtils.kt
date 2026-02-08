package xyz.aerii.athen.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.extentions.tag
import java.util.*
import kotlin.jvm.optionals.getOrNull

fun ItemStack.enchants(): List<String> {
    val tag = this.tag?.getCompound("enchantments")?.getOrNull() ?: return emptyList()

    return ArrayList<String>(tag.size()).apply {
        for (k in tag.keySet()) add(k.lowercase(Locale.ROOT))
    }
}

fun ItemStack.hasGlint() = componentsPatch.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)?.isPresent == true