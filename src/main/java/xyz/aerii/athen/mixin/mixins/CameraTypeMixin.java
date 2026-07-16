package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.aerii.athen.modules.impl.render.CameraHelper;

@Mixin(CameraType.class)
public class CameraTypeMixin {
    @Inject(method = "cycle", at = @At("HEAD"), cancellable = true)
    private void athen$cycle(CallbackInfoReturnable<CameraType> cir) {
        if (!CameraHelper.INSTANCE.getFront()) return;
        final CameraType self = athen$self();

        cir.setReturnValue(switch (self) {
            case FIRST_PERSON -> CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_BACK, THIRD_PERSON_FRONT -> CameraType.FIRST_PERSON;
        });
    }

    @Unique
    private CameraType athen$self() {
        return (CameraType) (Object) this;
    }
}
