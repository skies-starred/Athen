package foo.starred.athen.mixin.mixins;

import com.mojang.blaze3d.platform.Window;
import foo.starred.athen.events.InputEvent;
import foo.starred.athen.modules.impl.render.ContainerScale;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static foo.starred.snowbird.api.ClientKt.client;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void athen$onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (action == 1) {
            if (new InputEvent.Mouse.Press(rawButtonInfo).post()) ci.cancel();
        } else if (action == 0) {
            new InputEvent.Mouse.Release(rawButtonInfo).post();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void athen$onMove(long handle, double xpos, double ypos, CallbackInfo ci) {
        if (!(new InputEvent.Mouse.Move(xpos, ypos).post())) return;

        ci.cancel();
        this.xpos = xpos;
        this.ypos = ypos;
    }

    @Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("HEAD"), cancellable = true)
    private static void athen$getScaledXPos(Window window, double x, CallbackInfoReturnable<Double> cir) {
        if (!ContainerScale.getBool()) return;

        cir.setReturnValue(athen$correct(x * athen$x(window), ContainerScale.getX0()));
    }

    @Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("HEAD"), cancellable = true)
    private static void athen$getScaledYPos(Window window, double y, CallbackInfoReturnable<Double> cir) {
        if (!ContainerScale.getBool()) return;

        cir.setReturnValue(athen$correct(y * athen$y(window), ContainerScale.getY0()));
    }

    @Inject(method = "xpos()D", at = @At("HEAD"), cancellable = true)
    private void athen$xpos(CallbackInfoReturnable<Double> cir) {
        if (!ContainerScale.getBool()) return;

        double x0 = athen$x(client.getWindow());
        double x1 = athen$correct(this.xpos * x0, ContainerScale.getX0());
        cir.setReturnValue(x1 / x0);
    }

    @Inject(method = "ypos()D", at = @At("HEAD"), cancellable = true)
    private void athen$ypos(CallbackInfoReturnable<Double> cir) {
        if (!ContainerScale.getBool()) return;

        double y0 = athen$y(client.getWindow());
        double y1 = athen$correct(this.ypos * y0, ContainerScale.getY0());
        cir.setReturnValue(y1 / y0);
    }

    @Unique
    private static double athen$x(Window window) {
        return window.getGuiScaledWidth() / (double) window.getScreenWidth();
    }

    @Unique
    private static double athen$y(Window window) {
        return window.getGuiScaledHeight() / (double) window.getScreenHeight();
    }

    @Unique
    private static double athen$correct(double value, double anchor) {
        return anchor + (value - anchor) / ContainerScale.INSTANCE.getScale();
    }
}
