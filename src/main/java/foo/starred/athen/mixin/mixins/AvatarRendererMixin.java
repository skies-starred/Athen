package foo.starred.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import foo.starred.athen.ducks.entity.EntityRenderStateDuck;
import foo.starred.athen.modules.impl.render.CustomScale;
import foo.starred.athen.modules.impl.render.RenderTweaks;
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

import static foo.starred.snowbird.api.ClientKt.client;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void athen$scale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (!CustomScale.INSTANCE.getEnabled()) return;

        Entity entity = ((EntityRenderStateDuck) state).athen$getEntity();
        if (entity == null) return;
        if (!CustomScale.fn(entity)) return;

        final float s = CustomScale.INSTANCE.getScale() * 0.9375f;
        poseStack.scale(s, s, s);
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void athen$shouldShowName(Avatar entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!RenderTweaks.getNametag()) return;

        final LocalPlayer a = client.player;
        if (a == null) return;
        if (a != entity) return;

        cir.setReturnValue(true);
    }
}
