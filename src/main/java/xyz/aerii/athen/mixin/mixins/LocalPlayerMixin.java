package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.events.PlayerEvent;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void athen$drop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        final LocalPlayer a = athen$self();
        final ItemStack c = a.inventoryMenu.getSlot(a.getInventory().getSelectedSlot() + 36).getItem();

        if (c == ItemStack.EMPTY) return;
        if (new PlayerEvent.Drop(c, false).post()) cir.setReturnValue(false);
    }

    @Unique
    private LocalPlayer athen$self() {
        return (LocalPlayer) (Object) this;
    }
}
