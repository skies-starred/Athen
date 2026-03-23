package xyz.aerii.athen.modules.impl.general.keybinds

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class CategoryEntry(
    val name: String,
    val enabled: Boolean = true
) {
    companion object {
        val CODEC: Codec<CategoryEntry> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.STRING.fieldOf("name").forGetter(CategoryEntry::name),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CategoryEntry::enabled)
            ).apply(inst) { name, enabled -> CategoryEntry(name, enabled) }
        }
    }
}
