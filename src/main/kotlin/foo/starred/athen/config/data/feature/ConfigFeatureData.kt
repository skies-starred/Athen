package foo.starred.athen.config.data.feature

import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.data.base.IConfigElementData
import foo.starred.athen.config.data.impl.*

data class ConfigFeatureData(
    val name: String,
    val description: String,
    val configKey: String,
    val default: Any?
) {
    val options = mutableListOf<IConfigElementData>()

    fun option(data: IConfigElementData) {
        options += data
        when (data) {
            is ConfigSwitchElementData -> default(data.key, data.default)
            is ConfigSliderElementData -> default(data.key, data.default)
            is ConfigSelectorElementData -> default(data.key, data.default)
            is ConfigTextInputElementData -> default(data.key, data.default)
            is ConfigColorPickerElementData -> default(data.key, data.default)
            is ConfigKeybindElementData -> default(data.key, data.default)
            is ConfigMultiSelectorElementData -> default(data.key, data.default)
            is ConfigHudElementData -> default(data.key, data.default)
            is ConfigGroupElementData -> default(data.key, !data.collapsed)
            else -> {}
        }
    }

    fun all(): List<String> = listOf(configKey) + options.flatMap {
        when (it) {
            is ConfigButtonElementData, is ConfigGroupElementData, is ConfigVariablesElementData, is ConfigInformationElementData -> emptyList()
            else -> listOf(it.key)
        }
    }

    fun default(key: String, default: Any?) {
        if (key !in ConfigManager.values && default != null) ConfigManager.values[key] = default
    }
}