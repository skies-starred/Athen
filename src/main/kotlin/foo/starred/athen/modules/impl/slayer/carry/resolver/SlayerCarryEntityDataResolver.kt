package foo.starred.athen.modules.impl.slayer.carry.resolver

import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryPlayer
import foo.starred.kontext.api.entity.resolver.base.IKontextEntityDataResolver
import net.minecraft.world.entity.Entity

object SlayerCarryEntityDataResolver : IKontextEntityDataResolver<SlayerCarryPlayer>() {
    override fun resolve(entity: Entity): SlayerCarryPlayer? {
        return null
    }
}
