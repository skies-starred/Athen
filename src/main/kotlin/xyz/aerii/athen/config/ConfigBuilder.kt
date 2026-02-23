package xyz.aerii.athen.config

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.Athen
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.hud.HUDElement
import xyz.aerii.athen.hud.HUDElementContext
import xyz.aerii.athen.hud.HUDManager
import xyz.aerii.athen.modules.Module
import java.awt.Color
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class ExpandableHandle(val key: String, private val stateProvider: () -> Boolean) {
    operator fun invoke(): Boolean = stateProvider()
}

class ConfigBuilder(
    private val configKey: String,
    name: String,
    description: String,
    category: Category,
    private val default: Boolean = false
) {
    val feature = ConfigManager.addFeature(name, description, category, configKey, default)
    val state = React(default)
    val value get() = state.value
    var module: Module? = null
        internal set

    private val expandableStates = mutableMapOf<String, React<Boolean>>()

    init {
        Athen.LOGGER.debug("Feature added for {}: {}", configKey, feature)
        ConfigManager.observe(configKey) { state.value = it as? Boolean ?: default }
    }

    operator fun invoke(): Boolean = state.value

    fun onChange(call: (Boolean) -> Unit) = state.onChange(call)

    fun switch(name: String, default: Boolean = false) = option(default, ConfigManager.ElementData.Switch(name, "", default))

    inline fun <reified T : Number> slider(name: String, default: T, min: T, max: T, unit: String = "", showDouble: Boolean = false) = option(default, ConfigManager.ElementData.Slider(name, "", min.toDouble(), max.toDouble(), default.toDouble(), showDouble, unit))

    fun textInput(name: String, default: String = "", placeholder: String = "") = option(default, ConfigManager.ElementData.TextInput(name, "", default, placeholder, Int.MAX_VALUE))

    fun dropdown(name: String, options: List<String>, default: Int = 0) = option(default, ConfigManager.ElementData.Dropdown(name, "", options, default))

    fun colorPicker(name: String, default: Color = Color(0, 255, 255, 127)) = option(default, ConfigManager.ElementData.ColorPicker(name, "", default))

    fun keybind(name: String, default: Int = -1) = option(default, ConfigManager.ElementData.Keybind(name, "", default))

    fun multiCheckbox(name: String, options: List<String>, default: List<Int> = emptyList()) = option(default, ConfigManager.ElementData.MultiCheckbox(name, "", options, default))

    fun expandable(name: String) = ExpandableBuilder(name)

    fun hudElement(name: String, default: Boolean = true, outsidePreview: Boolean = true, renderer: HUDElementContext.() -> GuiGraphics.(Boolean) -> Pair<Int, Int>?): HUDElement =
        hudElementImpl(name, default, outsidePreview, renderer)

    fun hud(name: String, default: Boolean = true, outsidePreview: Boolean = true, renderer: GuiGraphics.(Boolean) -> Pair<Int, Int>?): HUDElement =
        hudElementImpl(name, default, outsidePreview) { renderer }

    fun button(text: String, onClick: () -> Unit) = option(text, ConfigManager.ElementData.Button(text, "$configKey.button_${text.hashCode()}", onClick))

    fun textParagraph(text: String) = option(text, ConfigManager.ElementData.TextParagraph(text, "$configKey.paragraph_${text.hashCode()}", text))

    fun <T : Any> option(default: T, data: ConfigManager.ElementData) = OptionBuilder(default, data)

    private fun hudElementImpl(name: String, default: Boolean, outsidePreview: Boolean, renderer: HUDElementContext.() -> GuiGraphics.(Boolean) -> Pair<Int, Int>?): HUDElement {
        val elementKey = "$configKey.hud_${name.hashCode()}"
        val hudElement = HUDElement(elementKey, name, this, { 0 to 0 }, enabled = default, renderOutsidePreview = outsidePreview)

        feature.option(ConfigManager.ElementData.HUDElement(name, elementKey, default, hudElement, this))
        hudElement.renderer = HUDElementContext(this, hudElement).renderer()
        HUDManager.register(hudElement)

        return hudElement
    }

    inner class ExpandableBuilder(private val name: String) {
        private var dependency: (() -> Boolean)? = null
        private var parentKey: String? = null

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, ExpandableHandle> {
            val fullKey = "$configKey.expandable_${property.name}"
            val state = React(false)
            expandableStates[fullKey] = state

            val finalData = ConfigManager.ElementData.Expandable(name, fullKey, dependency, parentKey)
            feature.option(finalData)

            ConfigManager.observe(fullKey) { state.value = it as? Boolean ?: false }

            return ReadOnlyProperty { _, _ -> ExpandableHandle(fullKey) { state.value } }
        }
    }

    inner class OptionBuilder<T : Any>(
        private val default: T,
        private val data: ConfigManager.ElementData
    ) {
        private val dependencies = mutableListOf<() -> Boolean>()
        private val calls = mutableListOf<(OptionHandler<T>) -> Unit>()
        private var parentKey: String? = null
        private var uniqueKey: String? = null

        fun dependsOn(condition: () -> Boolean): OptionBuilder<T> = apply {
            dependencies.add(condition)
        }

        fun childOf(parent: () -> ExpandableHandle): OptionBuilder<T> = apply {
            dependencies.add { parent().invoke() }
            parentKey = parent().key
        }

        fun unique(key: String): OptionBuilder<T> = apply {
            uniqueKey = key
        }

        fun resolve(block: (OptionHandler<T>) -> Unit) = apply {
            calls.add(block)
        }

        fun custom(key: String): OptionHandler<T> {
            val fullKey = "$configKey.$key"
            feature.option(fullKey.data())
            return OptionHandler(fullKey, default)
        }

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): OptionHandler<T> {
            val fullKey = "$configKey.${uniqueKey ?: property.name}"
            feature.option(fullKey.data())
            return OptionHandler(fullKey, default).also { for (c in calls) c.invoke(it) }
        }

        private fun String.data(): ConfigManager.ElementData {
            val visibility = if (dependencies.isEmpty()) null else { { dependencies.all { it() } } }
            return when (data) {
                is ConfigManager.ElementData.Switch -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.Slider -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.Dropdown -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.TextInput -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.ColorPicker -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.Keybind -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.MultiCheckbox -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.TextParagraph -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                is ConfigManager.ElementData.Button -> data.copy(key = this, visibilityDependency = visibility, parentKey = parentKey)
                else -> data
            }
        }
    }

    class OptionHandler<T : Any>(
        key: String,
        private val default: T
    ) : ReadOnlyProperty<Any?, T> {
        val state = React(default)
        val value get() = state.value

        init {
            ConfigManager.observe(key) {
                state.value = it as? T ?: default
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
    }
}