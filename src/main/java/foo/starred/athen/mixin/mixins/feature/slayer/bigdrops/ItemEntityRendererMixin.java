package foo.starred.athen.mixin.mixins.feature.slayer.bigdrops;

import com.mojang.blaze3d.vertex.PoseStack;
import foo.starred.athen.modules.impl.slayer.bigdrops.impl.BigSlayerDrops;
import foo.starred.athen.modules.impl.slayer.bigdrops.resolver.BigSlayerDropsEntityDataResolver;
import foo.starred.kontext.api.entity.duck.EntityRenderStateDuck;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    //~ if >= 26.3 'mulPose(Lorg/joml/Quaternionfc;)V' -> 'rotate(Lcom/mojang/math/Axis;F)V'
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void athen$submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!BigSlayerDrops.INSTANCE.getEnabled()) return;

        ItemEntity item = (ItemEntity) EntityRenderStateDuck.Companion.get(state);
        if (item == null) return;
        if (!Boolean.TRUE.equals(BigSlayerDropsEntityDataResolver.INSTANCE.get(item))) return;

        float scale = BigSlayerDrops.INSTANCE.getScale();
        poseStack.scale(scale, scale, scale);
    }
}
