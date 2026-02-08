package xyz.aerii.athen.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Scribble
import java.awt.Color

@Priority(-4)
object ConfigManager {
    private val configFile = Scribble("config/Config")
    val configValues = mutableMapOf<String, Any>()
    val features = mutableMapOf<Category, MutableList<Feature>>()
    val states = mutableMapOf<String, React<Any>>()

    init {
        val data by configFile.jsonObject("data")
        data.entrySet().forEach { (key, value) ->
            configValues[key] = parseValue(value)
        }
    }

    private fun parseValue(element: JsonElement): Any = when {
        element.isJsonPrimitive -> {
            element.asJsonPrimitive.let { prim ->
                when {
                    prim.isBoolean -> prim.asBoolean
                    prim.isNumber -> prim.asNumber.let { if (it.toDouble() % 1.0 == 0.0) it.toInt() else it.toDouble() }
                    else -> prim.asString
                }
            }
        }

        element.isJsonArray -> {
            element.asJsonArray.map { parseValue(it) }
        }

        element.isJsonObject -> {
            element.asJsonObject.let { obj ->
                if (
                    obj.has("r") &&
                    obj.has("g") &&
                    obj.has("b") &&
                    obj.has("a")
                ) {
                    Color(obj["r"].asInt, obj["g"].asInt, obj["b"].asInt, obj["a"].asInt)
                } else {
                    obj.entrySet().associate { it.key to parseValue(it.value) }
                }
            }
        }

        else -> element.toString()
    }

    private fun toJson(value: Any): JsonElement = when (value) {
        is Boolean -> {
            JsonPrimitive(value)
        }

        is Number -> {
            JsonPrimitive(value)
        }

        is String -> {
            JsonPrimitive(value)
        }

        is List<*> -> {
            JsonArray().apply {
                value.forEach { add(toJson(it ?: return@forEach)) }
            }
        }

        is Map<*, *> -> {
            JsonObject().apply {
                value.forEach { (k, v) ->
                    if (k is String && v != null) add(k, toJson(v))
                }
            }
        }

        is Color -> {
            JsonObject().apply {
                addProperty("r", value.red)
                addProperty("g", value.green)
                addProperty("b", value.blue)
                addProperty("a", value.alpha)
            }
        }

        else -> JsonPrimitive(value.toString())
    }

    fun addFeature(name: String, description: String, category: Category, configKey: String, default: Any?): Feature {
        val feature = Feature(name, description, configKey, default)
        features.getOrPut(category) { mutableListOf() }.add(feature)
        ensureDefault(configKey, default)
        return feature
    }

    fun updateConfig(key: String, value: Any) {
        configValues[key] = value
        states[key]?.value = value
        save(false)
    }

    fun observe(key: String, listener: (Any) -> Unit) {
        states.getOrPut(key) { React(getValue(key) ?: return) }.onChange(listener).also { listener(it.value) }
    }

    fun getValue(key: String): Any? = configValues[key]

    fun ensureDefault(key: String, default: Any?) {
        if (key !in configValues && default != null) configValues[key] = default
    }

    fun save(force: Boolean) {
        val data = JsonObject()
        features.values.flatten().forEach { feature ->
            feature.all().forEach { key ->
                configValues[key]?.let { value ->
                    data.add(key, toJson(value))
                }
            }
        }

        @Suppress("VariableNeverRead")
        var dataProp by configFile.jsonObject("data")
        @Suppress("AssignedValueIsNeverRead")
        dataProp = data

        if (force) configFile.save()
    }

    data class Feature(
        val name: String,
        val description: String,
        val configKey: String,
        val default: Any?
    ) {
        val options = mutableListOf<ElementData>()

        fun option(data: ElementData) {
            options.add(data)
            when (data) {
                is ElementData.Switch -> ensureDefault(data.key, data.default)
                is ElementData.Slider -> ensureDefault(data.key, data.default)
                is ElementData.Dropdown -> ensureDefault(data.key, data.default)
                is ElementData.TextInput -> ensureDefault(data.key, data.default)
                is ElementData.ColorPicker -> ensureDefault(data.key, data.default)
                is ElementData.Keybind -> ensureDefault(data.key, data.default)
                is ElementData.MultiCheckbox -> ensureDefault(data.key, data.default.toList())
                is ElementData.HUDElement -> ensureDefault(data.key, data.default)
                is ElementData.Expandable -> ensureDefault(data.key, false)
                else -> {}
            }
        }

        fun all(): List<String> = listOf(configKey) + options.mapNotNull {
            when (it) {
                is ElementData.Button, is ElementData.TextParagraph, is ElementData.Expandable -> null
                else -> it.key
            }
        }
    }

    sealed class ElementData {
        abstract val name: String
        abstract val key: String
        abstract val visibilityDependency: (() -> Boolean)?
        abstract val parentKey: String?

        data class HUDElement(override val name: String, override val key: String, val default: Boolean, val hudElement: xyz.aerii.athen.hud.internal.HUDElement, val config: ConfigBuilder, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class Switch(override val name: String, override val key: String, val default: Boolean, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class Slider(override val name: String, override val key: String, val min: Double, val max: Double, val default: Double, val showDouble: Boolean, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class Dropdown(override val name: String, override val key: String, val options: List<String>, val default: Int, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class TextInput(override val name: String, override val key: String, val default: String, val placeholder: String, val maxLength: Int, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class ColorPicker(override val name: String, override val key: String, val default: Color, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class Keybind(override val name: String, override val key: String, val default: Int, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class MultiCheckbox(override val name: String, override val key: String, val options: List<String>, val default: List<Int>, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class Button(override val name: String, override val key: String, val onClick: () -> Unit, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class TextParagraph(override val name: String, override val key: String, val text: String, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
        data class Expandable(override val name: String, override val key: String, override val visibilityDependency: (() -> Boolean)? = null, override val parentKey: String? = null) : ElementData()
    }
}