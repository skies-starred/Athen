package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
//~ if >= 26.1 'CameraRenderState' -> 'level.CameraRenderState'
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.ducks.entity.EntityRenderStateDuck;
import xyz.aerii.athen.ducks.entity.item.ItemEntityDuck;
import xyz.aerii.athen.modules.impl.slayer.BigSlayerDrops;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    //~ if >= 26.1 'state/CameraRenderState;)V' -> 'state/level/CameraRenderState;)V'
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void athen$submit(ItemEntityRenderState itemEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!BigSlayerDrops.INSTANCE.getEnabled()) return;

        final Entity a = ((EntityRenderStateDuck) itemEntityRenderState).athen$getEntity();
        if (a == null) return;
        if (!(a instanceof ItemEntity b)) return;

        final int c = ((ItemEntityDuck) b).athen$big();
        if (c != 1) return;

        final float d = BigSlayerDrops.INSTANCE.getScale();
        poseStack.scale(d, d, d);
    }
}