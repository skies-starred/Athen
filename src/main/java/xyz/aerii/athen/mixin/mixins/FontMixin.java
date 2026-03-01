package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.aerii.athen.modules.impl.render.VisualWords;

@Mixin(Font.class)
public class FontMixin {
    //? if >= 1.21.11 {
    /*@ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    *///? } else {
    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    //? }
    private FormattedCharSequence athen$prepareSequence(FormattedCharSequence seq) {
        return VisualWords.fn(seq);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence athen$widthSequence(FormattedCharSequence seq) {
        return VisualWords.fn(seq);
    }
}