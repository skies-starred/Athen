package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.GuiEvent;
import xyz.aerii.athen.modules.impl.render.tooltip.custom.CustomTooltip;

import java.util.List;

@Mixin(value = GuiGraphics.class, priority = Integer.MAX_VALUE)
public class GuiGraphicsMixin {
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void athen$renderItem(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        new GuiEvent.Items.Render.Pre(self(), stack, x, y).post();
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void athen$renderItemDecorations(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        new GuiEvent.Items.Render.Post(self(), stack, x, y).post();
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void athen$renderTooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, ResourceLocation background, CallbackInfo ci) {
        if (!CustomTooltip.INSTANCE.getEnabled()) return;

        CustomTooltip.render(self(), font, components, x, y, positioner);
        ci.cancel();
    }

    @Unique
    private GuiGraphics self() {
        return (GuiGraphics) (Object) this;
    }
}
