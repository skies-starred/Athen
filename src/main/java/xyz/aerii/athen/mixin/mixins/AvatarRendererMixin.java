package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.accessors.EntityRenderStateAccessor;
import xyz.aerii.athen.modules.impl.render.CustomScale;
import xyz.aerii.athen.modules.impl.render.RenderTweaks;

import static xyz.aerii.library.api.ClientKt.getClient;

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

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void athen$shouldShowName(Avatar avatar, double d, CallbackInfoReturnable<Boolean> cir) {
        if (!RenderTweaks.getNametag()) return;

        final LocalPlayer a = getClient().player;
        if (a == null) return;
        if (a != avatar) return;

        cir.setReturnValue(true);
    }
}
