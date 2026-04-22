package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.GuiEvent;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void athen$setScreen(Screen guiScreen, CallbackInfo ci) {
        if (guiScreen == null) {
            Screen old = this.screen;
            if (old == null) return;

            new GuiEvent.Close.Any(old).post();
            if (old instanceof AbstractContainerScreen<?> c) new GuiEvent.Close.Container(c).post();

            return;
        }

        new GuiEvent.Open.Any(guiScreen).post();
        if (guiScreen instanceof AbstractContainerScreen<?> c) new GuiEvent.Open.Container(c).post();
    }
}