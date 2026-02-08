@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import kotlinx.coroutines.*
import net.fabricmc.loader.api.FabricLoader
import tech.thatgravyboat.skyblockapi.utils.json.Json
import tech.thatgravyboat.skyblockapi.utils.json.Json.toData
import tech.thatgravyboat.skyblockapi.utils.json.Json.toJson
import xyz.aerii.athen.Athen
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.utils.asJsonObjectOrNull
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Manages persistent storage of data in JSON format.
 *
 * @property path The relative path to the storage file (without extension)
 */
class Scribble(private val path: String, private val tts: Int = 15) {
    private val file: File = File(FabricLoader.getInstance().configDir.toFile(), "${Athen.modId}/$path.json").apply { parentFile.mkdirs() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var root: JsonObject? = null
    private var dirty = false
    private var job: Job? = null

    init {
        on<GameEvent.Stop> {
            runBlocking {
                job?.cancelAndJoin()
                save()
            }
        }
    }

    private fun load(): JsonObject {
        if (root != null) return root!!

        root = try {
            if (!file.exists() || file.length() <= 0) JsonObject()
            else JsonParser.parseString(file.readText()).asJsonObjectOrNull?.get("@athen:data")?.asJsonObjectOrNull ?: JsonObject()
        } catch (e: Exception) {
            Athen.LOGGER.error("Failed to load scribbled file ($path): $e")
            JsonObject()
        }

        return root!!
    }

    fun save() {
        if (!dirty) return

        try {
            val data = root ?: return

            val wrapped = JsonObject().apply {
                add("@athen:data", data)
            }

            val tempFile = File(file.parent, "${file.name}.tmp")
            tempFile.writeText(Json.gson.toJson(wrapped))

            if (file.exists()) file.delete()
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }

            dirty = false
        } catch (e: Exception) {
            Athen.LOGGER.error("Failed to save scribbled file ($path): $e")
        }
    }

    fun dirty() {
        dirty = true
        job?.cancel()
        job = scope.launch {
            delay(tts * 1000L)
            save()
        }
    }

    fun reload() {
        root = null
        dirty = false
    }

    fun jsonObject(key: String, default: JsonObject = JsonObject()) = object : ReadWriteProperty<Any?, JsonObject> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): JsonObject {
            val obj = load()

            if (!obj.has(key)) {
                obj.add(key, default)
                dirty()
            }

            return obj.getAsJsonObject(key)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: JsonObject) {
            load().add(key, value)
            dirty()
        }
    }

    inner class Value<T : Any>(
        private val key: String,
        private val default: T,
        private val codec: Codec<T>
    ) : ReadWriteProperty<Any?, T> {
        var value: T
            get() = load().get(key)?.toData(codec) ?: default
            set(value) {
                value.toJson(codec)?.let {
                    load().add(key, it)
                    dirty()
                }
            }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.value = value
        }

        fun update(block: T.() -> Unit) {
            value = value.apply(block)
        }
    }

    fun int(key: String, default: Int = 0) = Value(key, default, Codec.INT)
    fun long(key: String, default: Long = 0L) = Value(key, default, Codec.LONG)
    fun string(key: String, default: String = "") = Value(key, default, Codec.STRING)
    fun boolean(key: String, default: Boolean = false) = Value(key, default, Codec.BOOL)
    fun double(key: String, default: Double = 0.0) = Value(key, default, Codec.DOUBLE)
    fun float(key: String, default: Float = 0f) = Value(key, default, Codec.FLOAT)

    fun <T : Any> list(key: String, codec: Codec<T>, default: List<T> = emptyList()) = Value(key, default, codec.listOf())
    fun <T : Any> set(key: String, codec: Codec<T>, default: Set<T> = emptySet()) = Value(key, default, codec.listOf().xmap({ it.toSet() }, { it.toList() }))
    fun <K : Any, V : Any> map(key: String, keyCodec: Codec<K>, valueCodec: Codec<V>, default: Map<K, V> = emptyMap()) = Value(key, default, Codec.unboundedMap(keyCodec, valueCodec))

    fun <T : Any> mutableList(key: String, codec: Codec<T>, default: MutableList<T> = mutableListOf()) = Value(key, default, codec.listOf().xmap({ it.toMutableList() }, { it.toList() }))
    fun <T : Any> mutableSet(key: String, codec: Codec<T>, default: MutableSet<T> = mutableSetOf()) = Value(key, default, codec.listOf().xmap({ it.toMutableSet() }, { it.toList() }))
    fun <K : Any, V : Any> mutableMap(key: String, keyCodec: Codec<K>, valueCodec: Codec<V>, default: MutableMap<K, V> = mutableMapOf()) = Value(key, default, Codec.unboundedMap(keyCodec, valueCodec).xmap({ it.toMutableMap() }, { it.toMap() }))
}