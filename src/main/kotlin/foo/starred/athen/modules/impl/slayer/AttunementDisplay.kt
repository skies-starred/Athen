@file:Suppress("UNUSED", "LocalVariableName")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.api.slayers.SlayerAPI
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.ducks.entity.generic.EntityDuck.Companion.attachedNames
import foo.starred.athen.modules.Module
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.format.extension.component.formattedText
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.format.extension.string.stripped

@LoadableAthenModule
@AthenModuleScope(islands = [PresetSkyBlockIsland.CRIMSON_ISLE])
object AttunementDisplay : Module(
    "Attunement display",
    "Displays the current attunement for blaze slayer, does not work with demons.",
    ConfigCategory.SLAYER
) {
    private val count by config.switch("Display count")

    private val regex = Regex("^(?<attunement>[A-Z]+) ♨(?<count>\\d) \\d\\d:\\d\\d$")

    private val display = Ticking(5) {
        val entity = SlayerAPI.slayer?.entity ?: return@Ticking null
        val lines = entity.attachedNames

        for (line in lines) {
            val (_attunement, _count) = regex.find(line.stripped())?.destructured ?: continue
            val attunement = _attunement.fn() ?: continue
            val count = _count.toIntOrNull() ?: continue

            return@Ticking attunement.fn0(count).literal()
        }

        null
    }

    private val hud by config.hud("Attunement display") {
        val example = "§l§eAURIC ♨5".formattedText()

        constrain {
            MinecraftFontMeasurer.constrain(example)
        }

        preview {
            MinecraftFontRenderer.extract(graphics, example, 0, 0)
        }

        render {
            MinecraftFontRenderer.extract(graphics, display.value ?: return@render, 0, 0)
        }
    }

    private fun String.fn(): String? = when (this) {
        "ASHEN" -> "§l§8$this"
        "AURIC" -> "§l§e$this"
        "CRYSTAL" -> "§l§b$this"
        "SPIRIT" -> "§l§f$this"
        else -> null
    }

    private fun String.fn0(int: Int): String {
        if (!count) return this
        return "$this ♨$int"
    }
}
