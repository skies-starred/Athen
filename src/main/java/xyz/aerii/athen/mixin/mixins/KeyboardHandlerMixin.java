package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.InputEvent;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void athen$keyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1) {
            if (new InputEvent.Keyboard.Press(event).post()) ci.cancel();
        } else if (action == 0) {
            if (new InputEvent.Keyboard.Release(event).post()) ci.cancel();
        }
    }
}
