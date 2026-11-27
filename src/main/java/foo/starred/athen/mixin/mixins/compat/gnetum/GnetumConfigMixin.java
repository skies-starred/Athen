package foo.starred.athen.mixin.mixins.compat.gnetum;

import foo.starred.athen.modules.impl.render.radial.RadialMenu;
import kotlin.Unit;
import me.decce.gnetum.GnetumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GnetumConfig.class, remap = false)
public class GnetumConfigMixin {
    @Unique
    private int i = -1;

    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true)
    private void athen$isEnabled(CallbackInfoReturnable<Boolean> cir) {
        if (i == -1) athen$bool();
        if (i == 1) cir.setReturnValue(false);
    }

    @Unique
    private void athen$bool() {
        i = 0;
        RadialMenu.INSTANCE.getOpen().observe(b -> {
            i = b ? 1 : 0;
            return Unit.INSTANCE;
        });
    }
}
