package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.aerii.athen.modules.impl.render.RenderOptimiser;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @ModifyVariable(method = "getBuffer", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static FogRenderer.FogMode athen$getBuffer(FogRenderer.FogMode fogMode) {
        if (RenderOptimiser.getFog()) return FogRenderer.FogMode.NONE;
        return fogMode;
    }
}