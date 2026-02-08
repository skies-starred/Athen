package xyz.aerii.athen.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.accessors.ItemStackAccessor;
import xyz.aerii.athen.events.GuiEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin implements ItemStackAccessor {
    @Unique
    private final ThreadLocal<List<Component>> athen$capturedList = new ThreadLocal<>();

    @Unique
    private List<Component> athen$cachedVanilla = null;

    @Unique
    private List<Component> athen$cachedModified = null;

    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addDetailsToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;Ljava/util/function/Consumer;)V"))
    private void athen$getTooltipLines(Item.TooltipContext tooltipContext, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir, @Local List<Component> list) {
        this.athen$capturedList.set(list);
    }

    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z", shift = At.Shift.AFTER, ordinal = 0))
    private void athen$addDetailsToTooltip(Item.TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Player player, TooltipFlag tooltipFlag, Consumer<Component> consumer, CallbackInfo ci) {
        List<Component> list = this.athen$capturedList.get();
        if (list == null) return;

        boolean hasChanged = this.athen$cachedVanilla == null || !this.athen$cachedVanilla.equals(list);

        ItemStack stack = (ItemStack) (Object) this;

        if (hasChanged) {
            List<Component> mutableCopy = new ArrayList<>(list);
            new GuiEvent.Tooltip.Update(stack, mutableCopy).post();

            this.athen$cachedVanilla = new ArrayList<>(list);
            this.athen$cachedModified = new ArrayList<>(mutableCopy);

            list.clear();
            list.addAll(mutableCopy);
        } else {
            list.clear();
            list.addAll(this.athen$cachedModified);
        }

        this.athen$capturedList.remove();
    }

    @Override
    public void athen$invalidate() {
        this.athen$cachedVanilla = null;
        this.athen$cachedModified = null;
    }
}