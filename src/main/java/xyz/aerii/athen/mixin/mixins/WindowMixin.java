package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.hud.Resolute;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "setGuiScale", at = @At("RETURN"))
    private void athen$setGuiScale(int i, CallbackInfo ci) {
        Resolute.update();
    }
}