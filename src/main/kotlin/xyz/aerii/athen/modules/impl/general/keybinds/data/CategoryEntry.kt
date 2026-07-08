package xyz.aerii.athen.modules.impl.general.keybinds.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.aerii.athen.ui.base.ICategoryEntry

data class CategoryEntry(
    override val name: String,
    override val enabled: Boolean = true
) : ICategoryEntry {
    companion object {
        val CODEC: Codec<CategoryEntry> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.STRING.fieldOf("name").forGetter(CategoryEntry::name),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CategoryEntry::enabled)
            ).apply(inst) { name, enabled -> CategoryEntry(name, enabled) }
        }
    }
}