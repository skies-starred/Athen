package foo.starred.athen.mixin.mixins;

import foo.starred.athen.events.GuiEvent;
import foo.starred.athen.events.PlayerEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
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

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void athen$onRenderSlot$pre(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (new GuiEvent.Slots.Render.Any.Pre(graphics, slot).post()) ci.cancel();
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void athen$onRenderSlot$post(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        new GuiEvent.Slots.Render.Any.Post(graphics, slot).post();
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void athen$slotClick(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (slotId == -999 && containerInput == ContainerInput.PICKUP) {
            if (new PlayerEvent.Drop(this.menu.getCarried(), true).post()) ci.cancel();
        }

        if (new GuiEvent.Slots.Input.Click(slot, slotId, buttonNum, containerInput).post()) ci.cancel();
    }

    @Inject(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getHoveredSlot(DD)Lnet/minecraft/world/inventory/Slot;"))
    private void athen$renderContents$0(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        athen$previousHoveredSlot = hoveredSlot;
    }

    @Inject(method = "extractContents", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hoveredSlot:Lnet/minecraft/world/inventory/Slot;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void athen$renderContents$1(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (hoveredSlot == athen$previousHoveredSlot) return;

        if (hoveredSlot != null) new GuiEvent.Slots.Input.Hover(hoveredSlot).post();
        else new GuiEvent.Slots.Input.Unhover(athen$previousHoveredSlot).post();

        athen$previousHoveredSlot = hoveredSlot;
    }
}
