package xyz.aerii.athen.mixin.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.aerii.athen.events.GuiEvent;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void athen$render(Screen instance, GuiGraphics guiGraphics, int i, int j, float f, Operation<Void> original) {
        if (instance instanceof AbstractContainerScreen<?>) if (new GuiEvent.Container.Render.Pre(guiGraphics).post()) return;
        original.call(instance, guiGraphics, i, j, f);
    }
}
