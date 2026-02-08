package xyz.aerii.athen.mixin.mixins;

import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.events.PacketEvent;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class PacketProcessorMixin {

    @Shadow
    @Final
    private Packet<?> packet;

    @Inject(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"), cancellable = true)
    private void athen$handle(CallbackInfo ci) {
        if (new PacketEvent.Process(packet).post()) ci.cancel();
    }
}
