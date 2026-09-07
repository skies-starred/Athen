package foo.starred.athen.mixin.mixins.compat.caxton;

import foo.starred.athen.modules.impl.render.VisualWords;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.flirora.caxton.layout.Run;

@Mixin(value = Run.class, remap = false)
public class CaxtonRunMixin {
    @ModifyVariable(method = "splitIntoRuns", at = @At("HEAD"), argsOnly = true)
    private static FormattedCharSequence athen$splitIntoRuns(FormattedCharSequence text) {
        if (!VisualWords.INSTANCE.getEnabled()) return text;
        if (VisualWords.words.getMap0().isEmpty()) return text;

        return VisualWords.words.fn(text);
    }
}
