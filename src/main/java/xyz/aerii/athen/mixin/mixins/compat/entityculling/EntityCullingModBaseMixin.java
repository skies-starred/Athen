package xyz.aerii.athen.mixin.mixins.compat.entityculling;

import dev.tr7zw.entityculling.EntityCullingModBase;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.handlers.Texter;
import xyz.aerii.athen.handlers.Typo;
import xyz.aerii.athen.modules.impl.ModSettings;

@Mixin(value = EntityCullingModBase.class, remap = false)
public class EntityCullingModBaseMixin {

    @Inject(method = "clientTick", at = @At("HEAD"))
    private void disableTickCulling(CallbackInfo ci) {
        if (!ModSettings.getDisableTickCulling()) return;

        EntityCullingModBase instance = (EntityCullingModBase) (Object) this;
        if (instance.config != null && instance.config.tickCulling) {
            instance.config.tickCulling = false;
            Component literal = Texter.onHover(Component.literal("Disabled tick culling in the mod \"Entity culling\"!"), "This was done to improve compatibility with our slayer features.");
            Typo.modMessage(literal);
        }
    }
}