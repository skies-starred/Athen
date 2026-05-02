package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.modules.impl.render.RenderOptimiser;

@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {
    //~ if >= 1.21.11 'renderEffects"' -> 'render"'
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void athen$renderEffects(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!RenderOptimiser.getEffects()) return;

        ci.cancel();
    }
}
