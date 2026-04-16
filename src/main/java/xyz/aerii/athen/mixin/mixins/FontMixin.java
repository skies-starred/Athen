package xyz.aerii.athen.mixin.mixins;

import kotlin.Unit;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.aerii.athen.modules.impl.render.VisualWords;

@Mixin(Font.class)
public class FontMixin {
    @Unique
    private static boolean bool;

    static {
        bool = VisualWords.INSTANCE.getEnabled();
        VisualWords.INSTANCE.getObservable().onChange(b -> {
            bool = b;
            return Unit.INSTANCE;
        });
    }

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private String athen$prepareText$string(String text) {
        return bool ? VisualWords.words.fn(text) : text;
    }

    //~ if >= 1.21.11 'FFIZI' -> 'FFIZZI'
    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence athen$prepareText$sequence(FormattedCharSequence seq) {
        return bool ? VisualWords.words.fn(seq) : seq;
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence athen$width$sequence(FormattedCharSequence seq) {
        return bool ? VisualWords.words.fn(seq) : seq;
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText athen$width$text(FormattedText text) {
        return bool && text instanceof Component ? VisualWords.words.fn((Component) text) : text;
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String athen$width$string(String text) {
        return bool ? VisualWords.words.fn(text) : text;
    }
}