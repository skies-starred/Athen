package xyz.aerii.athen.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import tech.thatgravyboat.skyblockapi.utils.extentions.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.tag
import java.util.*
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

private fun ItemStack.data(): CompoundTag =
    getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()

fun ItemStack.glint() =
    //? if >= 26.1 {
    /*components.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) != null
    *///? } else {
    componentsPatch.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)?.isPresent == true
    //? }

fun ItemStack.etherwarp(): Boolean =
    data().getBoolean("ethermerge").orElse(false)!! || getSkyBlockId() == "ETHERWARP_CONDUIT"

fun ItemStack.enchants(): List<String> {
    val tag = this.tag?.getCompound("enchantments")?.getOrNull() ?: return emptyList()

    return ArrayList<String>(tag.size()).apply {
        for (k in tag.keySet()) add(k.lowercase(Locale.ROOT))
    }
}

fun ItemStack.enchants0(): List<String> {
    val tag = this.tag?.getCompound("enchantments")?.getOrNull() ?: return emptyList()

    return ArrayList<String>(tag.size()).apply {
        for (k in tag.keySet()) {
            val k = k.lowercase(Locale.ROOT)
            val i = tag.getInt(k).getOrDefault(0)
            add("$k:$i")
        }
    }
}