@file:Suppress("Unused")

package xyz.aerii.athen.hud.internal

import xyz.aerii.athen.config.ConfigBuilder

/**
 * A wrapper for ConfigBuilder that automatically makes the options depend on the HUD element being enabled in the config screen.
 */
class HUDElementContext(
    val builder: ConfigBuilder,
    val hudElement: HUDElement
) {
    fun switch(name: String, default: Boolean = false) =
        builder.switch(name, default).dependsOn { hudElement.enabled }

    inline fun <reified T : Number> slider(name: String, default: T, min: T, max: T, showDouble: Boolean = false) =
        builder.slider(name, default, min, max, showDouble).dependsOn { hudElement.enabled }

    fun textInput(name: String, default: String = "", placeholder: String = "") =
        builder.textInput(name, default, placeholder).dependsOn { hudElement.enabled }

    fun dropdown(name: String, options: List<String>, default: Int = 0) =
        builder.dropdown(name, options, default).dependsOn { hudElement.enabled }

    fun colorPicker(name: String, default: java.awt.Color = java.awt.Color(0, 255, 255, 127)) =
        builder.colorPicker(name, default).dependsOn { hudElement.enabled }

    fun keybind(name: String, default: Int = 0) =
        builder.keybind(name, default).dependsOn { hudElement.enabled }

    fun multiCheckbox(name: String, options: List<String>, default: List<Int> = emptyList()) =
        builder.multiCheckbox(name, options, default).dependsOn { hudElement.enabled }

    fun button(text: String, onClick: () -> Unit) =
        builder.button(text, onClick).dependsOn { hudElement.enabled }

    fun textParagraph(text: String) =
        builder.textParagraph(text).dependsOn { hudElement.enabled }
}
