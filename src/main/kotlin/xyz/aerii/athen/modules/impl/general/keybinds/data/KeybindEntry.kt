package xyz.aerii.athen.modules.impl.general.keybinds.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class KeybindEntry(
    val keys: List<Int>,
    val command: String,
    val enabled: Boolean = true,
    val category: String = "",
    val condition: KeybindCondition = KeybindCondition()
) {
    companion object {
        val CODEC: Codec<KeybindEntry> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.INT.listOf().fieldOf("keys").forGetter(KeybindEntry::keys),
                Codec.STRING.fieldOf("command").forGetter(KeybindEntry::command),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(KeybindEntry::enabled),
                Codec.STRING.optionalFieldOf("category", "").forGetter(KeybindEntry::category),
                KeybindCondition.CODEC.optionalFieldOf("condition", KeybindCondition())
                    .forGetter(KeybindEntry::condition)
            ).apply(inst, ::KeybindEntry)
        }
    }
}