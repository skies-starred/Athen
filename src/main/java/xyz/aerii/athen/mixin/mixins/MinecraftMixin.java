package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.PlayerEvent;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void athen$startUseItem(CallbackInfo ci) {
        if (player != null) if (new PlayerEvent.Interact(player.getMainHandItem()).post()) ci.cancel();
    }
}