package foo.starred.athen.modules.impl.general.pet.data

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.modules.impl.general.pet.impl.PetKeybinds
import foo.starred.kontext.abstraction.client.extension.boundKey
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot

data class PetKeybindsSlot(
    val ordinal: Int,
    val index: Int,
    val keybind: () -> InputConstants.Key
) {
    val hotbar by lazy {
        client.options.keyHotbarSlots[ordinal]
    }

    val slot: Slot?
        get() = client.player?.containerMenu?.slots?.getOrNull(index)

    val equipped: Boolean
        get() {
            val lore = slot?.item?.get<GenericKontextItemDataResolver.LORE, List<Component>>() ?: return false
            return lore.getOrNull(lore.lastIndex - 2)?.stripped() == "Click to despawn!"
        }

    fun value(hotbar: Boolean): InputConstants.Key {
        return if (hotbar) this.hotbar.boundKey() else this.keybind()
    }

    companion object {
        val ALL = listOf(
            PetKeybindsSlot(0, 10) { PetKeybinds.key0 },
            PetKeybindsSlot(1, 11) { PetKeybinds.key1 },
            PetKeybindsSlot(2, 12) { PetKeybinds.key2 },
            PetKeybindsSlot(3, 13) { PetKeybinds.key3 },
            PetKeybindsSlot(4, 14) { PetKeybinds.key4 },
            PetKeybindsSlot(5, 15) { PetKeybinds.key5 },
            PetKeybindsSlot(6, 16) { PetKeybinds.key6 },
            PetKeybindsSlot(7, 19) { PetKeybinds.key7 },
            PetKeybindsSlot(8, 20) { PetKeybinds.key8 },
        )
    }
}
