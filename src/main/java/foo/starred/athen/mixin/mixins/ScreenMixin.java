package foo.starred.athen.mixin.mixins;

import foo.starred.athen.events.GuiEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void athen$renderWithTooltipAndSubtitles$pre(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (new GuiEvent.Render.Screen.Pre(guiGraphics).post()) ci.cancel();
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void athen$renderWithTooltipAndSubtitles$post(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        new GuiEvent.Render.Screen.Post(guiGraphics).post();
    }

    @Unique
    private Screen self() {
        return (Screen) (Object) this;
    }
}
