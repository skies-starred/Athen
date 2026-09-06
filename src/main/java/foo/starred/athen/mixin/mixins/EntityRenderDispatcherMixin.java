package foo.starred.athen.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import foo.starred.athen.ducks.entity.EntityRenderStateDuck;
import foo.starred.athen.events.WorldRenderEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void athen$extractEntity(Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState renderState = cir.getReturnValue();
        ((EntityRenderStateDuck) renderState).athen$setEntity(entity);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void athen$submit$pre(EntityRenderState renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        Entity entity = ((EntityRenderStateDuck) renderState).athen$getEntity();
        if (new WorldRenderEvent.Entity(renderState, poseStack, camera, entity).post()) ci.cancel();
    }
}
