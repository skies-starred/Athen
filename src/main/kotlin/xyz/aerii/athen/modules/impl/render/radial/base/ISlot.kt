package xyz.aerii.athen.modules.impl.render.radial.base

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.utils.extentions.createSkull
import xyz.aerii.athen.modules.impl.render.radial.base.actions.IAction
import xyz.aerii.athen.modules.impl.render.radial.base.actions.impl.NoAction

class ISlot(
    var name: String,
    var action: IAction = NoAction,
    var sub: List<ISlot> = emptyList(),
    itemId: String = "barrier",
    text: String? = null,
) {
    private var _item: ItemStack? = null

    var itemId: String = itemId
        set(value) {
            if (field == value) return
            field = value
            _item = null
        }

    var text: String? = text
        set(value) {
            if (field == value) return
            field = value
            _item = null
        }

    val item: ItemStack
        get() {
            _item?.let { return it }
            val r = runCatching { BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(itemId)).orElse(Items.BARRIER) }.getOrDefault(Items.BARRIER)
            val t = text
            return (if (r == Items.PLAYER_HEAD && t != null) createSkull(t) else r.defaultInstance).also { _item = it }
        }
}