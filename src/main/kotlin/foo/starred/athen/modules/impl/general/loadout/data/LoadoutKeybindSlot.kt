@file:Suppress("PropertyName")

package foo.starred.athen.modules.impl.general.loadout.data

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.modules.impl.general.loadout.impl.LoadoutKeybinds
import foo.starred.kontext.abstraction.client.extension.boundKey
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot

data class LoadoutKeybindSlot(
    val ordinal: Int,
    val index: Int,
    val keybind: () -> InputConstants.Key
) {
    val hotbar by lazy {
        client.options.keyHotbarSlots[ordinal.coerceAtMost(8)]
    }

    val slot: Slot?
        get() = client.player?.containerMenu?.slots?.getOrNull(index)

    val equipped: Boolean
        get() {
            val lore = slot?.item?.get<GenericKontextItemDataResolver.LORE, List<Component>>() ?: return false
            return lore.getOrNull(lore.lastIndex - 1)?.stripped()?.isEmpty() ?: false
        }

    fun value(hotbar: Boolean): InputConstants.Key {
        if (hotbar && index >= 41) return InputConstants.UNKNOWN
        return if (hotbar) this.hotbar.boundKey() else this.keybind()
    }

    companion object {
        val ALL = listOf(
            LoadoutKeybindSlot(0, 14) { LoadoutKeybinds.key0 },
            LoadoutKeybindSlot(1, 15) { LoadoutKeybinds.key1 },
            LoadoutKeybindSlot(2, 16) { LoadoutKeybinds.key2 },
            LoadoutKeybindSlot(3, 23) { LoadoutKeybinds.key3 },
            LoadoutKeybindSlot(4, 24) { LoadoutKeybinds.key4 },
            LoadoutKeybindSlot(5, 25) { LoadoutKeybinds.key5 },
            LoadoutKeybindSlot(6, 32) { LoadoutKeybinds.key6 },
            LoadoutKeybindSlot(7, 33) { LoadoutKeybinds.key7 },
            LoadoutKeybindSlot(8, 34) { LoadoutKeybinds.key8 },
            LoadoutKeybindSlot(9, 41) { LoadoutKeybinds.key9 },
            LoadoutKeybindSlot(10, 42) { LoadoutKeybinds.key10 },
            LoadoutKeybindSlot(11, 43) { LoadoutKeybinds.key11 }
        )
    }
}
