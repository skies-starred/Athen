package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.accessors.EntityRenderStateAccessor;
import xyz.aerii.athen.modules.impl.render.CustomScale;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void athen$scale(AvatarRenderState avatarRenderState, PoseStack poseStack, CallbackInfo ci) {
        if (!CustomScale.INSTANCE.getEnabled()) return;
        Entity entity = ((EntityRenderStateAccessor) avatarRenderState).athen$getEntity();
        if (entity == null) return;
        if (!CustomScale.fn(entity)) return;
        float s = CustomScale.INSTANCE.getScale() * 0.9375f;
        poseStack.scale(s, s, s);
    }
}
