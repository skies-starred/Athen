package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.GuiEvent;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void athen$renderWithTooltipAndSubtitles$pre(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen self = self();
        if ((self instanceof AbstractContainerScreen<?>)) if (new GuiEvent.Render.Container.Pre(guiGraphics).post()) ci.cancel();
        if (new GuiEvent.Render.Screen.Pre(guiGraphics).post()) ci.cancel();
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
    private void athen$renderWithTooltipAndSubtitles$post(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        new GuiEvent.Render.Screen.Post(guiGraphics).post();
    }

    @Unique
    private Screen self() {
        return (Screen) (Object) this;
    }
}
