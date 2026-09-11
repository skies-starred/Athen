package foo.starred.athen.config.dsl.base

import foo.starred.athen.config.data.base.IConfigElementData
import foo.starred.athen.config.data.impl.*
import foo.starred.athen.config.dsl.impl.builders.config.ConfigMainBuilder
import foo.starred.athen.config.dsl.impl.builders.group.ConfigGroupBuilder
import foo.starred.athen.config.dsl.impl.builders.hud.ConfigHudBuilder
import foo.starred.athen.config.dsl.impl.builders.option.ConfigOptionBuilder
import foo.starred.athen.config.dsl.impl.builders.sound.ConfigSoundOption
import foo.starred.athen.hud.HUDElement
import foo.starred.athen.hud.HUDManager
import net.minecraft.client.gui.GuiGraphicsExtractor

interface ConfigScope {
    val builder: ConfigMainBuilder
    val parent: String? get() = null

    fun switch(name: String, default: Boolean = false): ConfigOptionBuilder<Boolean> {
        return option(default, ConfigSwitchElementData(name, "", default))
    }

    fun <T : Number> slider(name: String, default: T, min: T, max: T, unit: String = "", double: Boolean = false): ConfigOptionBuilder<T> {
        return option(default, ConfigSliderElementData(name, "", min.toDouble(), max.toDouble(), default.toDouble(), double, unit))
    }

    fun input(name: String, default: String = "", placeholder: String = ""): ConfigOptionBuilder<String> {
        return option(default, ConfigTextInputElementData(name, "", default, placeholder, Int.MAX_VALUE))
    }

    fun selector(name: String, options: List<String>, default: Int = 0): ConfigOptionBuilder<Int> {
        return option(default, ConfigSelectorElementData(name, "", options, default))
    }

    fun colorPicker(name: String, default: Number = -1): ConfigOptionBuilder<Int> {
        return option(default.toInt(), ConfigColorPickerElementData(name, "", default.toInt()))
    }

    fun keybind(name: String, default: Int = -1): ConfigOptionBuilder<Int> {
        return option(default, ConfigKeybindElementData(name, "", default))
    }

    fun multiSelector(name: String, options: List<String>, default: List<Int> = emptyList()): ConfigOptionBuilder<List<Int>> {
        return option(default, ConfigMultiSelectorElementData(name, "", options, default))
    }

    fun group(name: String, collapsed: Boolean = true): ConfigGroupBuilder {
        return ConfigGroupBuilder(builder, name, collapsed, parent)
    }

    fun sound(name: String, default: String = "block.note_block.pling", enabled: Boolean = true, pitch: Float = 1f, volume: Float = 1f): ConfigSoundOption {
        return ConfigSoundOption(builder, name, default, enabled, pitch, volume, parent)
    }

    fun hud(name: String, default: Boolean = true, outsidePreview: Boolean = true, renderer: GuiGraphicsExtractor.(Boolean) -> Pair<Int, Int>?): ConfigHudBuilder {
        val key = "${builder.configKey}.hud_${name.hashCode()}"
        val element = HUDElement(key, name, builder, renderer, enabled = default, renderOutsidePreview = outsidePreview)
        builder.feature.option(ConfigHudElementData(name, key, default, element, builder, parent = parent))
        HUDManager.register(element)
        return ConfigHudBuilder(builder, element)
    }

    fun button(text: String, onClick: () -> Unit): ConfigOptionBuilder<String> {
        return option(text, ConfigButtonElementData(text, "${builder.configKey}.button_${text.hashCode()}", onClick))
    }

    fun variables(vararg tokens: String): ConfigOptionBuilder<List<String>> {
        return option(tokens.toList(), ConfigVariablesElementData("Variables", "${builder.configKey}.variables_${tokens.joinToString().hashCode()}", tokens.toList()))
    }

    fun variables(name: String, tokens: List<String>): ConfigOptionBuilder<List<String>> {
        return option(tokens, ConfigVariablesElementData(name, "${builder.configKey}.variables_${name.hashCode()}_${tokens.joinToString().hashCode()}", tokens))
    }

    fun information(text: String): ConfigOptionBuilder<String> {
        return option(text, ConfigInformationElementData("Information", "${builder.configKey}.info_${text.hashCode()}", text))
    }

    fun information(name: String, text: String): ConfigOptionBuilder<String> {
        return option(text, ConfigInformationElementData(name, "${builder.configKey}.info_${name.hashCode()}_${text.hashCode()}", text))
    }

    fun <T> option(default: T, data: IConfigElementData): ConfigOptionBuilder<T> {
        return ConfigOptionBuilder(builder, default, data, parent)
    }
}
