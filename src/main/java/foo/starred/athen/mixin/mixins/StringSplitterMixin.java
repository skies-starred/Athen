package foo.starred.athen.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import foo.starred.athen.modules.impl.render.VisualWords;
import foo.starred.snowbird.api.text.replacer.AbstractTextReplacer;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(value = StringSplitter.class, priority = 2000)
public abstract class StringSplitterMixin {
    @Unique
    private static final long[] athen$widths = new long[4096];

    @Unique
    private static final StringBuilder athen$sb = new StringBuilder();

    @Unique
    private static int athen$hash0;

    @Shadow
    public abstract float stringWidth(FormattedCharSequence text);

    @ModifyReturnValue(method = "stringWidth(Lnet/minecraft/network/chat/FormattedText;)F", at = @At("RETURN"))
    private float athen$stringWidth(float original, FormattedText text) {
        if (text == null) return original;
        if (!VisualWords.INSTANCE.getEnabled()) return original;
        if (VisualWords.words.getMap0().isEmpty()) return original;

        final String string = athen$extract(text);
        final int hash0 = athen$hash0;
        final int hash1 = (string.hashCode() ^ hash0) & 4095;

        final int version = VisualWords.words.getVersion();
        final AbstractTextReplacer.Companion.Entry entry = VisualWords.words.getEntries()[hash1];

        if (entry.version != version || entry.style != hash0 || !string.equals(entry.string)) {
            return original;
        }

        final int version0 = version ^ string.hashCode() ^ hash0;
        final long packed = athen$widths[hash1];
        if ((int) (packed >>> 32) == version0 && packed != 0L) {
            return Float.intBitsToFloat((int) packed);
        }

        final float width = this.stringWidth(entry.sequence);
        athen$widths[hash1] = ((long) version0 << 32) | (Float.floatToIntBits(width) & 0xFFFFFFFFL);
        return width;
    }

    @Unique
    private static String athen$extract(FormattedText text) {
        if (text instanceof Component component) {
            athen$hash0 = athen$hash(component);
            return component.getString();
        }

        athen$sb.setLength(0);
        athen$hash0 = 0;

        text.visit((style, str) -> {
            athen$sb.append(str);
            athen$hash0 = 31 * athen$hash0 + athen$hash(style);
            return Optional.empty();
        }, Style.EMPTY);

        return athen$sb.toString();
    }

    @Unique
    private static int athen$hash(Component component) {
        int hash = athen$hash(component.getStyle());
        final List<Component> siblings = component.getSiblings();

        for (Component sibling : siblings) {
            hash = 31 * hash + athen$hash(sibling);
        }

        return hash;
    }

    @Unique
    private static int athen$hash(Style style) {
        if (style.isEmpty()) return 0;
        int flags = (style.isBold() ? 1 : 0) | (style.isItalic() ? 2 : 0) | (style.isUnderlined() ? 4 : 0) | (style.isStrikethrough() ? 8 : 0) | (style.isObfuscated() ? 16 : 0);
        int color = style.getColor() != null ? style.getColor().getValue() : -1;
        return flags ^ (color * 31);
    }
}
