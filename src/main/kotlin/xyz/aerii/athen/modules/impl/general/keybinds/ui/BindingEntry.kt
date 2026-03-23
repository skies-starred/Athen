package xyz.aerii.athen.modules.impl.general.keybinds.ui

import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds

data class BindingEntry(val index: Int, val binding: Keybinds.KeybindEntry) {
    var condition = binding.condition.copy()
    var toggleAnim = if (binding.enabled) 1f else 0f
}
