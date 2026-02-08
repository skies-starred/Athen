package xyz.aerii.athen.mixin.mixins;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.hud.internal.HUDElement;
import xyz.aerii.athen.hud.internal.HUDManager;

@Mixin(Window.class)
public class WindowMixin {
    
    @Inject(method = "setGuiScale", at = @At("RETURN"))
    private void athen$onSetGuiScale(int i, CallbackInfo ci) {
        for (HUDElement element : HUDManager.INSTANCE.getElements().values()) element.refreshScale();
    }
}
