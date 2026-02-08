package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.aerii.athen.events.GuiEvent;
import xyz.aerii.athen.modules.impl.render.ItemNamePosition;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void athen$render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new GuiEvent.Render.Pre(guiGraphics).post();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void athen$renderPost(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new GuiEvent.Render.Post(guiGraphics).post();
    }

    @ModifyArgs(
            method = "renderSelectedItemName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"
            )
    )
    private void athen$renderSelectedItemName(Args args) {
        if (!ItemNamePosition.INSTANCE.getReact().getValue()) return;

        int width = args.get(4);

        args.set(2, ItemNamePosition.x() - (width / 2));
        args.set(3, ItemNamePosition.y());
    }
}