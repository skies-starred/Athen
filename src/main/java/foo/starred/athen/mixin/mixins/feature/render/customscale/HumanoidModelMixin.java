package foo.starred.athen.mixin.mixins.feature.render.customscale;

import foo.starred.athen.modules.impl.render.CustomScale;
import foo.starred.kontext.api.entity.duck.EntityRenderStateDuck;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void athen$setupAnim(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)) return;
        if (!CustomScale.INSTANCE.getEnabled()) return;

        final int style = CustomScale.INSTANCE.getChibi();
        if (style == 0) return;

        final Entity entity = EntityRenderStateDuck.Companion.get(avatarState);
        if (entity == null) return;
        if (!CustomScale.fn(entity)) return;

        final HumanoidModel<?> self = athen$self();
        final float factor = CustomScale.INSTANCE.getChibiness();
        final float scale = style == 1 ? factor : (1.0f / factor);

        self.head.xScale = scale;
        self.head.yScale = scale;
        self.head.zScale = scale;
    }

    @Unique
    private HumanoidModel<?> athen$self() {
        return (HumanoidModel<?>) (Object) this;
    }
}
