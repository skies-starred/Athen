package foo.starred.athen.modules.impl.slayer.bigdrops.resolver

import foo.starred.athen.modules.impl.slayer.bigdrops.impl.BigSlayerDrops.fn
import foo.starred.kontext.api.entity.resolver.base.IKontextEntityDataResolver
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity

object BigSlayerDropsEntityDataResolver : IKontextEntityDataResolver<Boolean>(cache = true) {
    override fun resolve(entity: Entity): Boolean? {
        return (entity as? ItemEntity)?.fn()
    }
}
