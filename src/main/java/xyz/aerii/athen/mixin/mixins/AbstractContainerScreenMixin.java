package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.GuiEvent;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Unique
    @Nullable
    private Slot athen$previousHoveredSlot = null;

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

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void athen$slotClick(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (new GuiEvent.Slots.Click(slot, slotId, mouseButton, type).post()) ci.cancel();
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getHoveredSlot(DD)Lnet/minecraft/world/inventory/Slot;"))
    private void athen$renderContents$0(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        athen$previousHoveredSlot = hoveredSlot;
    }

    @Inject(
            method = "renderContents",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hoveredSlot:Lnet/minecraft/world/inventory/Slot;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void athen$renderContents$1(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (hoveredSlot == athen$previousHoveredSlot) return;

        if (hoveredSlot != null) new GuiEvent.Slots.Hover(hoveredSlot).post();
        athen$previousHoveredSlot = hoveredSlot;
    }
}