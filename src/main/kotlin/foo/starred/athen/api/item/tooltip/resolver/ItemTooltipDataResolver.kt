package foo.starred.athen.api.item.tooltip.resolver

import foo.starred.kontext.api.item.resolver.base.IKontextItemDataResolver
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

object ItemTooltipDataResolver : IKontextItemDataResolver<Pair<List<Component>, List<Component>>>() {
    override fun resolve(stack: ItemStack): Pair<List<Component>, List<Component>>? {
        return null
    }
}
