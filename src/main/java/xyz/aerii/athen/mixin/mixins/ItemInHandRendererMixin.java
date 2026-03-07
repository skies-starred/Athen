package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.modules.impl.render.RenderOptimiser;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(
            method = "renderPlayerArm",
            at = @At("HEAD"),
            cancellable = true
    )
    private void athen$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, float f, float g, HumanoidArm humanoidArm, CallbackInfo ci) {
        if (RenderOptimiser.getArm()) ci.cancel();
    }
}
