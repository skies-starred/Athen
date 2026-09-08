package foo.starred.athen.mixin.mixins;

import foo.starred.athen.modules.impl.render.ContainerScale;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static foo.starred.snowbird.api.ClientKt.client;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow
    private int cachedGuiScale;

    @Shadow
    @Final
    private Map<Object, OversizedItemRenderer> oversizedItemRenderers;

    @Shadow
    protected abstract void invalidateItemAtlas();

    @Inject(method = "getGuiScaleInvalidatingItemAtlasIfChanged", at = @At("HEAD"), cancellable = true)
    private void athen$getGuiScaleInvalidatingItemAtlasIfChanged(CallbackInfoReturnable<Integer> cir) {
        if (!ContainerScale.getBool()) {
            return;
        }

        int scale = (int) Math.ceil(client.getWindow().getGuiScale() * ContainerScale.INSTANCE.getScale());
        if (scale == this.cachedGuiScale) {
            cir.setReturnValue(scale);
            return;
        }

        this.invalidateItemAtlas();

        for (OversizedItemRenderer renderer : this.oversizedItemRenderers.values()) {
            renderer.invalidateTexture();
        }

        this.cachedGuiScale = scale;
        cir.setReturnValue(scale);
    }
}
