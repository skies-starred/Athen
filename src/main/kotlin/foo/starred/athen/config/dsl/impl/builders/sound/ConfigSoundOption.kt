package foo.starred.athen.config.dsl.impl.builders.sound

import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.dsl.impl.builders.config.ConfigMainBuilder
import foo.starred.athen.config.dsl.impl.builders.group.ConfigGroupBuilder
import foo.starred.snowbird.api.mainThread
import foo.starred.snowbird.utils.play
import foo.starred.snowbird.utils.sound
import net.minecraft.sounds.SoundEvent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ConfigSoundOption(
    val builder: ConfigMainBuilder,
    val name: String,
    private val sound0: String,
    private val enabled0: Boolean = true,
    private val pitch0: Float = 1f,
    private val volume0: Float = 1f,
    parent: String? = null
) : ReadOnlyProperty<Any?, ConfigSoundOption> {
    private val group = ConfigGroupBuilder(builder, name, parent0 = parent)

    var enabled: Boolean = enabled0
        private set

    var id: String = sound0
        private set

    var event: SoundEvent? = sound0.sound()
        private set

    var pitch: Float = pitch0
        private set

    var volume: Float = volume0
        private set

    val sound: SoundEvent
        get() = event ?: fallback

    fun play(volume0: Float = volume, pitch0: Float = pitch) {
        if (!enabled) return

        mainThread {
            event?.play(volume0, pitch0)
        }
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, ConfigSoundOption> {
        val handle = group.provideDelegate(thisRef, property).getValue(thisRef, property)
        val key = handle.key

        handle.switch("Enabled", enabled0).unique("${property.name}.enabled")
        handle.input("Sound ID", sound0).unique("${property.name}.sound")
        handle.slider("Pitch", pitch0.toDouble(), 0.0, 2.0, double = true).unique("${property.name}.pitch")
        handle.slider("Volume", volume0.toDouble(), 0.0, 1.0, double = true).unique("${property.name}.volume")

        ConfigManager.observe("$key.enabled") {
            enabled = it as? Boolean ?: enabled0
        }

        ConfigManager.observe("$key.sound") {
            val sound = it as? String ?: sound0
            id = sound
            event = sound.sound()
        }

        ConfigManager.observe("$key.pitch") {
            pitch = (it as? Number)?.toFloat() ?: pitch0
        }

        ConfigManager.observe("$key.volume") {
            volume = (it as? Number)?.toFloat() ?: volume0
        }

        return this
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): ConfigSoundOption {
        return this
    }

    companion object {
        private val fallback = SoundEvent.createVariableRangeEvent(ResourceAPI.minecraft("entity.cat.purr"))
    }
}
