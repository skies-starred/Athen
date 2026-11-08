package foo.starred.athen.modules.impl.general.keybinds.data

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.kontext.abstraction.input.impl.MouseInputState

data class KeybindEntry(
    val keys: List<InputConstants.Key>,
    val command: String,
    val enabled: Boolean = true,
    val category: String = "",
    val condition: KeybindCondition = KeybindCondition()
) {
    companion object {
        private val KEY: Codec<InputConstants.Key> = Codec.either(Codec.STRING, Codec.INT).xmap(
            { either ->
                either.map(
                    { string -> runCatching { InputConstants.getKey(string) }.getOrDefault(InputConstants.UNKNOWN) },
                    { number ->
                        when (number) {
                            in 0..7 -> MouseInputState.vanilla(number)
                            in -8..-1 -> MouseInputState.vanilla(-number - 1)
                            else -> KeyboardInputState.vanilla(number)
                        }
                    }
                )
            },
            { key -> Either.left(key.name) }
        )

        val CODEC: Codec<KeybindEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                KEY.listOf().fieldOf("keys").forGetter(KeybindEntry::keys),
                Codec.STRING.fieldOf("command").forGetter(KeybindEntry::command),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(KeybindEntry::enabled),
                Codec.STRING.optionalFieldOf("category", "").forGetter(KeybindEntry::category),
                KeybindCondition.CODEC.optionalFieldOf("condition", KeybindCondition()).forGetter(KeybindEntry::condition)
            ).apply(instance, ::KeybindEntry)
        }
    }
}
