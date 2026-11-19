package foo.starred.athen.modules.impl.general.wardrobe.data

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.modules.impl.general.wardrobe.impl.WardrobeKeybinds
import foo.starred.kontext.abstraction.client.extension.boundKey
import foo.starred.kontext.abstraction.client.extension.client
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items

data class WardrobeKeybindSlot(
    val index: Int,
    val keybind: () -> InputConstants.Key
) {
    val hotbar by lazy {
        val index = index - 36
        client.options.keyHotbarSlots[index]
    }

    val slot: Slot?
        get() = client.player?.containerMenu?.slots?.getOrNull(index)

    val equipped: Boolean
        //~ if >= 26.2 'Items.LIME_DYE' -> 'Items.DYE.lime()'
        get() = slot?.item?.item == Items.LIME_DYE

    fun value(hotbar: Boolean): InputConstants.Key {
        return if (hotbar) this.hotbar.boundKey() else this.keybind()
    }

    companion object {
        val ALL = listOf(
            WardrobeKeybindSlot(36) { WardrobeKeybinds.key0 },
            WardrobeKeybindSlot(37) { WardrobeKeybinds.key1 },
            WardrobeKeybindSlot(38) { WardrobeKeybinds.key2 },
            WardrobeKeybindSlot(39) { WardrobeKeybinds.key3 },
            WardrobeKeybindSlot(40) { WardrobeKeybinds.key4 },
            WardrobeKeybindSlot(41) { WardrobeKeybinds.key5 },
            WardrobeKeybindSlot(42) { WardrobeKeybinds.key6 },
            WardrobeKeybindSlot(43) { WardrobeKeybinds.key7 },
            WardrobeKeybindSlot(44) { WardrobeKeybinds.key8 }
        )
    }
}
