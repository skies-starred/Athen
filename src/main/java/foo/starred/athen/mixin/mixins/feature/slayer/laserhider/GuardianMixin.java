package foo.starred.athen.mixin.mixins.feature.slayer.laserhider;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foo.starred.athen.modules.impl.slayer.laserhider.impl.EndermanLaserHider;
import foo.starred.athen.modules.impl.slayer.laserhider.resolver.EndermanLaserEntityDataResolver;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Guardian.class)
public class GuardianMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", ordinal = 1))
    private void athen$aiStep(Level instance, ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd, Operation<Void> original) {
        if (!EndermanLaserHider.INSTANCE.getEnabled()) {
            original.call(instance, particle, x, y, z, xd, yd, zd);
            return;
        }

        Guardian self = (Guardian) (Object) this;
        if (Boolean.TRUE.equals(EndermanLaserEntityDataResolver.INSTANCE.get(self))) return;

        original.call(instance, particle, x, y, z, xd, yd, zd);
    }
}
