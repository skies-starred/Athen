package xyz.aerii.athen.mixin.mixins.compat.exordium;

import dev.tr7zw.exordium.components.BufferInstance;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.modules.impl.render.radial.impl.RadialMenu;

@Mixin(value = BufferInstance.class, remap = false)
public class BufferInstanceMixin {
    @Unique
    private int i = -1;

    @Inject(method = "skipGuiRendering", at = @At("HEAD"), cancellable = true)
    private void athen$skipGuiRendering(CallbackInfoReturnable<Boolean> cir) {
        if (i == -1) athen$bool();
        if (i == 1) cir.setReturnValue(false);
    }

    @Inject(method = "renderBuffer", at = @At("HEAD"), cancellable = true)
    private void athen$renderBuffer(CallbackInfoReturnable<Boolean> cir) {
        if (i == -1) athen$bool();
        if (i == 1) cir.setReturnValue(false);
    }

    @Unique
    private void athen$bool() {
        RadialMenu.INSTANCE.getOpen().onChange(b -> {
            i = b ? 1 : 0;
            return Unit.INSTANCE;
        });
    }
}