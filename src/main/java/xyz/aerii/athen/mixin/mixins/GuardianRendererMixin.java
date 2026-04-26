package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.accessors.EntityRenderStateAccessor;
import xyz.aerii.athen.modules.impl.slayer.EndermanLaserHider;

@Mixin(GuardianRenderer.class)
public class GuardianRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/GuardianRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"), cancellable = true)
    private void athen$submit(GuardianRenderState guardianRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!EndermanLaserHider.INSTANCE.getEnabled()) return;
        final Guardian a = (Guardian) ((EntityRenderStateAccessor) guardianRenderState).athen$getEntity();

        if (a == null) return;
        if (!EndermanLaserHider.fn(a)) return;

        ci.cancel();
    }
}
