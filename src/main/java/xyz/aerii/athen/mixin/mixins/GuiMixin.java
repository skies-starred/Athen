package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.aerii.athen.events.GuiEvent;
import xyz.aerii.athen.modules.impl.render.ItemNamePosition;
import xyz.aerii.athen.modules.impl.render.RenderOptimiser;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void athen$render$pre(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new GuiEvent.Render.Pre(guiGraphics).post();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSleepOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void athen$render$main(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new GuiEvent.Render.Main(guiGraphics).post();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void athen$render$post(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        new GuiEvent.Render.Post(guiGraphics).post();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void athen$renderSlot$pre(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        if (new GuiEvent.Slots.Render.Hotbar.Pre(guiGraphics, stack, x, y).post()) ci.cancel();
    }

    @Inject(method = "renderSlot", at = @At("TAIL"), cancellable = true)
    private void athen$renderSlot$post(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        if (new GuiEvent.Slots.Render.Hotbar.Post(guiGraphics, stack, x, y).post()) ci.cancel();
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void athen$renderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!RenderOptimiser.getEffects()) return;
        ci.cancel();
    }

    @ModifyArgs(
            method = "renderSelectedItemName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"
            )
    )
    private void athen$renderSelectedItemName(Args args) {
        if (!ItemNamePosition.INSTANCE.getEnabled()) return;

        int width = args.get(4);

        args.set(2, ItemNamePosition.x() - (width / 2));
        args.set(3, ItemNamePosition.y());
    }
}