package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.modules.impl.render.RenderOptimiser;

@Mixin(LavaFogEnvironment.class)
public class LavaFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void athen$setupFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!RenderOptimiser.getLava()) return;
        fogData.environmentalStart = Float.MAX_VALUE;
        fogData.environmentalEnd = Float.MAX_VALUE;
        ci.cancel();
    }
}
