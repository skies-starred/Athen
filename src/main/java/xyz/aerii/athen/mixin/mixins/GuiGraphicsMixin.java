package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.modules.impl.general.ItemTweaks;
import xyz.aerii.athen.modules.impl.render.CustomTooltip;

import java.util.List;

@Mixin(value = GuiGraphics.class, priority = Integer.MAX_VALUE)
public class GuiGraphicsMixin {
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("TAIL")
    )
    private void athen$renderStarCount(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        ItemTweaks.renderStarCount((GuiGraphics) (Object) this, font, stack, x, y);
    }

    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL")
    )
    private void athen$renderStarCount(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        ItemTweaks.renderStarCount((GuiGraphics) (Object) this, font, stack, x, y);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void athen$renderTooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, ResourceLocation background, CallbackInfo ci) {
        if (!CustomTooltip.INSTANCE.getEnabled()) return;

        CustomTooltip.render((GuiGraphics)(Object)this, font, components, x, y, positioner);
        ci.cancel();
    }
}
