package foo.starred.athen.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import foo.starred.athen.annotations.Priority
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.data.feature.ConfigFeatureData
import foo.starred.snowbird.api.data.Observable
import java.awt.Color

@Priority(-4)
object ConfigManager {
    private val json = JsonStore("config/Config")

    val values = mutableMapOf<String, Any>()
    val features = mutableMapOf<Category, MutableList<ConfigFeatureData>>()
    val states = mutableMapOf<String, Observable<Any>>()

    init {
        val data by json.jsonObject("data")

        for ((k, v) in data.entrySet()) {
            values[k] = v.deserialize()
        }
    }

    fun feature(name: String, description: String, category: Category, configKey: String, default: Any?): ConfigFeatureData {
        val feature = ConfigFeatureData(name, description, configKey, default).also { features.getOrPut(category) { mutableListOf() }.add(it) }
        return feature.also { it.default(configKey, default) }
    }

    fun update(key: String, value: Any) {
        values[key] = value
        states[key]?.value = value
        save(false)
    }

    fun observe(key: String, listener: (Any) -> Unit) {
        states.getOrPut(key) { Observable(get(key) ?: return) }.onChange(listener).also { listener(it.value) }
    }

    fun get(key: String): Any? {
        return values[key]
    }

    fun save(force: Boolean) {
        val data = JsonObject()
        for (feature in features.values.flatten()) {
            for (key in feature.all()) {
                val value = values[key] ?: continue
                data.add(key, value.serialize())
            }
        }

        var data0 by json.jsonObject("data")
        data0 = data

        if (force) json.save()
    }

    private fun JsonElement.deserialize(): Any = when {
        isJsonPrimitive -> {
            asJsonPrimitive.run {
                when {
                    isBoolean -> asBoolean
                    isNumber -> asNumber.let { if (it.toDouble() % 1.0 == 0.0) it.toInt() else it.toDouble() }
                    else -> asString
                }
            }
        }

        isJsonArray -> {
            asJsonArray.map { it.deserialize() }
        }

        isJsonObject -> {
            asJsonObject.run {
                if (has("r") && has("g") && has("b") && has("a")) Color(get("r").asInt, get("g").asInt, get("b").asInt, get("a").asInt).rgb
                else entrySet().associate { it.key to it.value.deserialize() }
            }
        }

        else -> toString()
    }

    private fun Any.serialize(): JsonElement = when (this) {
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is List<*> -> JsonArray().also { array -> forEach { it?.let { v -> array.add(v.serialize()) } } }
        is Map<*, *> -> JsonObject().apply { forEach { (k, v) -> if (k is String && v != null) add(k, v.serialize()) } }
        else -> JsonPrimitive(toString())
    }
}
