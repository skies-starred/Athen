package foo.starred.athen.mixin.mixins.feature.render.customscale;

import com.mojang.blaze3d.vertex.PoseStack;
import foo.starred.athen.modules.impl.render.CustomScale;
import foo.starred.kontext.api.entity.duck.EntityRenderStateDuck;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void athen$scale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (!CustomScale.INSTANCE.getEnabled()) return;

        final Entity entity = EntityRenderStateDuck.Companion.get(state);
        if (entity == null) return;
        if (!CustomScale.fn(entity)) return;

        final float scale0 = CustomScale.INSTANCE.getScale();
        if (scale0 == 1f) return;

        final float scale1 = scale0 * 0.9375f;
        poseStack.scale(scale1, scale1, scale1);
    }
}
