package foo.starred.athen.mixin.mixins;

import foo.starred.athen.events.GuiEvent;
import foo.starred.athen.modules.impl.render.ContainerScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void athen$renderWithTooltipAndSubtitles$pre(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (new GuiEvent.Render.Screen.Pre(graphics).post()) ci.cancel();
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void athen$extractBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!ContainerScale.getBool()) return;

        ci.cancel();
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void athen$renderWithTooltipAndSubtitles$post(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        new GuiEvent.Render.Screen.Post(graphics).post();
    }
}
