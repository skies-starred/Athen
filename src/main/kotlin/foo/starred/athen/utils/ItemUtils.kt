package foo.starred.athen.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

fun ItemStack.customData(): CompoundTag? {
    return get(DataComponents.CUSTOM_DATA)?.copyTag()
}

fun ItemStack.glint(): Boolean {
    return components.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) != null
}

fun ItemStack.id(): String? {
    return customData()?.getString("id")?.getOrNull()
}

fun ItemStack.uuid(): String? {
    return customData()?.getString("uuid")?.getOrNull()
}

fun ItemStack.texture(): String? {
    return get(DataComponents.PROFILE)?.partialProfile()?.properties?.get("textures")?.firstOrNull()?.value
}

fun ItemStack.lore(): List<Component>? {
    return get(DataComponents.LORE)?.styledLines()
}

fun ItemStack.enchants(): List<String> {
    val tag = customData()?.getCompound("enchantments")?.getOrNull() ?: return emptyList()

    return ArrayList<String>(tag.size()).apply {
        for (k in tag.keySet()) add(k.lowercase(Locale.ROOT))
    }
}

fun ItemStack.enchants0(): List<String> {
    val tag = customData()?.getCompound("enchantments")?.getOrNull() ?: return emptyList()

    return ArrayList<String>(tag.size()).apply {
        for (k in tag.keySet()) {
            val k = k.lowercase(Locale.ROOT)
            val i = tag.getInt(k).getOrDefault(0)
            add("$k:$i")
        }
    }
}
