package foo.starred.athen.modules.impl

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.modules.Module

@LoadableAthenModule
object ModSettings : Module(
    "Mod settings",
    "Toggles for a lot of the internal stuff in the mod!",
    ConfigCategory.GENERAL
) {
    @JvmStatic
    val disableTickCulling by config.switch("Disable tick culling", true).description("Disabling this may break slayer features!")

    @JvmStatic
    val commandConfig by config.switch("\'/athen\' opens config")

    @JvmStatic
    val priceFetch = config.slider("Price re-fetch", 10, 5, 60, "minutes").unique("priceFetch")

    @JvmStatic
    val hideGuis by config.switch("Hide GUIs in F1", true)

    @JvmStatic
    val calculator by config.switch("Enable \"/calc\"", true).description("You will need to restart your game after toggling this option!")

    @JvmStatic
    val location = config.switch("Share slayer bosses", true).description("Share spawned slayer bosses with nearby players using Athen via WebSocket.").unique("shareSlayer")
}
