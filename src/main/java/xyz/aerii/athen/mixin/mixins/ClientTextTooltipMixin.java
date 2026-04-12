package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.aerii.athen.modules.impl.render.tooltip.custom.CustomTooltip;

@Mixin(ClientTextTooltip.class)
public class ClientTextTooltipMixin {
    @ModifyArg(method = "renderText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"), index = 5)
    private boolean tooltipShadow(boolean shadow) {
        return CustomTooltip.INSTANCE.getEnabled() ? CustomTooltip.INSTANCE.getText$shadow() : shadow;
    }
}