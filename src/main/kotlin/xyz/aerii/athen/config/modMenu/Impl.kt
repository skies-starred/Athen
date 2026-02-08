package xyz.aerii.athen.config.modMenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import xyz.aerii.athen.config.ui.ClickGUI

internal object Impl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { _: Screen? -> ClickGUI }
}
