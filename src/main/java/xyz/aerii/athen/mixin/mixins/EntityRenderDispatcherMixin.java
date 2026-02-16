package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.accessors.EntityRenderStateAccessor;
import xyz.aerii.athen.events.WorldRenderEvent;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void athen$extractEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState renderState = cir.getReturnValue();
        ((EntityRenderStateAccessor) renderState).athen$setEntity(entity);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void athen$submit$pre(EntityRenderState renderState, CameraRenderState cameraRenderState, double camX, double camY, double camZ, PoseStack poseStack, SubmitNodeCollector nodeCollector, CallbackInfo ci) {
        Entity entity = ((EntityRenderStateAccessor) renderState).athen$getEntity();
        if (new WorldRenderEvent.Entity.Pre(renderState, poseStack, cameraRenderState, entity).post()) ci.cancel();
    }

    @Inject(method = "submit", at = @At(value = "RETURN"))
    private void athen$submit$post(EntityRenderState renderState, CameraRenderState cameraRenderState, double camX, double camY, double camZ, PoseStack poseStack, SubmitNodeCollector nodeCollector, CallbackInfo ci) {
        Entity entity = ((EntityRenderStateAccessor) renderState).athen$getEntity();
        new WorldRenderEvent.Entity.Post(renderState, poseStack, cameraRenderState, entity).post();
    }
}