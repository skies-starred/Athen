package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.handlers.Smoothie;
import xyz.aerii.athen.modules.impl.Dev;
import xyz.aerii.athen.modules.impl.ModSettings;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique
    private final static String player = Smoothie.getClient().getUser().getName();

    @Inject(method = "isUpsideDownName", at = @At("HEAD"), cancellable = true)
    private static void athen$isUpsideDown(String name, CallbackInfoReturnable<Boolean> cir) {
        if (!Dev.cool.contains(name)) return;
        if (!name.equals(player) || ModSettings.getUpsideDown()) cir.setReturnValue(true);
    }
}