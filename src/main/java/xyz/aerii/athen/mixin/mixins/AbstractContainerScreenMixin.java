package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.GuiEvent;
import xyz.aerii.athen.events.PlayerEvent;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Unique
    @Nullable
    private Slot athen$previousHoveredSlot = null;

    //~ if >= 26.1 'renderSlot' -> 'extractSlot'
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void athen$onRenderSlot$pre(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (new GuiEvent.Slots.Render.Pre(guiGraphics, slot).post()) ci.cancel();
    }

    //~ if >= 26.1 'renderSlot' -> 'extractSlot'
    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void athen$onRenderSlot$post(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        new GuiEvent.Slots.Render.Post(guiGraphics, slot).post();
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void athen$slotClick(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (slotId == -999 && type == ClickType.PICKUP) {
            if (new PlayerEvent.Drop(this.menu.getCarried(), true).post()) ci.cancel();
        }

        if (new GuiEvent.Slots.Click(slot, slotId, mouseButton, type).post()) ci.cancel();
    }

    //~ if >= 26.1 'renderContents' -> 'extractContents'
    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getHoveredSlot(DD)Lnet/minecraft/world/inventory/Slot;"))
    private void athen$renderContents$0(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        athen$previousHoveredSlot = hoveredSlot;
    }

    //~ if >= 26.1 'renderContents' -> 'extractContents'
    @Inject(method = "renderContents", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hoveredSlot:Lnet/minecraft/world/inventory/Slot;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void athen$renderContents$1(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (hoveredSlot == athen$previousHoveredSlot) return;

        if (hoveredSlot != null) new GuiEvent.Slots.Hover(hoveredSlot).post();
        else new GuiEvent.Slots.Unhover(athen$previousHoveredSlot).post();

        athen$previousHoveredSlot = hoveredSlot;
    }
}