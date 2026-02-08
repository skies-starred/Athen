package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.events.GuiEvent;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    //? >= 1.21.11 {
    /*private void athen$onRenderSlot$pre(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    *///? } else {
    private void athen$onRenderSlot$pre(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
    //? }
        if (new GuiEvent.Slots.Render.Pre(guiGraphics, slot).post()) ci.cancel();
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    //? >= 1.21.11 {
    /*private void athen$onRenderSlot$post(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    *///? } else {
    private void athen$onRenderSlot$post(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
    //? }
        new GuiEvent.Slots.Render.Post(guiGraphics, slot).post();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void athen$keyPress(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (new GuiEvent.Input.Key.Press(event).post()) cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void athen$mouseClick(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (new GuiEvent.Input.Mouse.Press(event).post()) cir.setReturnValue(true);
    }
}
