package foo.starred.athen.modules.impl.slayer.laserhider.resolver

import foo.starred.athen.modules.impl.slayer.laserhider.impl.EndermanLaserHider.fn
import foo.starred.kontext.api.entity.resolver.base.IKontextEntityDataResolver
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Guardian

object EndermanLaserEntityDataResolver : IKontextEntityDataResolver<Boolean>() {
    override fun resolve(entity: Entity): Boolean? {
        return (entity as? Guardian)?.fn()
    }
}
