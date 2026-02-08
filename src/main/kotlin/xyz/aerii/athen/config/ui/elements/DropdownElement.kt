@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ui.elements.base.IExpandable

class DropdownElement(
    name: String,
    options: List<String>,
    private var selectedIndex: Int,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IExpandable<Int>(name, options, configKey, onUpdate) {

    override fun text() = options[selectedIndex]

    override fun content(x: Float, y: Float, index: Int, option: String) {
        val textW = textWidth(option)
        drawText(option, x + width / 2f - textW / 2f, y + 5f)
    }

    override fun click(index: Int) {
        selectedIndex = index
        onUpdate(configKey, selectedIndex)
        collapse()
    }
}