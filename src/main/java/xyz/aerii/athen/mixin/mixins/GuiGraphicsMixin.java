package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.modules.impl.general.ItemTweaks;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("TAIL")
    )
    private void athen$renderStarCount(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        ItemTweaks.INSTANCE.renderStarCount((GuiGraphics) (Object) this, font, stack, x, y);
    }

    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL")
    )
    private void athen$renderStarCount(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        ItemTweaks.INSTANCE.renderStarCount((GuiGraphics) (Object) this, font, stack, x, y);
    }
}
