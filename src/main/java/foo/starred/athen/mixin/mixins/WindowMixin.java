package foo.starred.athen.mixin.mixins;

import com.mojang.blaze3d.platform.Window;
import foo.starred.athen.hud.HUDManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "setGuiScale", at = @At("RETURN"))
    private void athen$setGuiScale(int guiScale, CallbackInfo ci) {
        HUDManager.INSTANCE.compute();
    }
}
