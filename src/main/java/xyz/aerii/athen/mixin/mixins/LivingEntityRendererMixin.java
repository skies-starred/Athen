package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.modules.impl.Dev;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "isUpsideDownName", at = @At("HEAD"), cancellable = true)
    private static void athen$isUpsideDown(String name, CallbackInfoReturnable<Boolean> cir) {
        if (Dev.people.getCool().contains(name)) {
            cir.setReturnValue(true);
        }
    }
}
