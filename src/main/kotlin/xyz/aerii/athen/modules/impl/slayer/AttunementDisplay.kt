package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.util.FormattedCharSequence
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroups
import xyz.aerii.athen.accessors.parent
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.api.skyblock.SlayerAPI
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.fcs
import xyz.aerii.library.utils.literal

@Load
@OnlyIn(islands = [SkyBlockIsland.CRIMSON_ISLE])
object AttunementDisplay : Module(
    "Attunement display",
    "Displays the current attunement for blaze slayer, does not work with demons.",
    Category.SLAYER
) {
    private val regex = Regex("^(?<attunement>[A-Z]+) ♨(?<count>\\d) \\d\\d:\\d\\d$")

    private val count by config.switch("Display count")

    private val ex0 = "§l§eAURIC ♨5".fcs
    private var last: FormattedCharSequence? = null

    init {
        config.hud("Attunement display") {
            if (it) return@hud sizedText(ex0)
            return@hud sizedText(last ?: return@hud null)
        }

        on<EntityEvent.Update.Named> {
            val e = entity.parent ?: return@on
            val s = SlayerAPI.slayerBosses[e] ?: return@on
            if (!s.isOwnedByPlayer) return@on

            val a = regex.findGroups(stripped, "attunement", "count") ?: return@on
            val b = a["attunement"]?.fn() ?: return@on
            val c = a["count"]?.toIntOrNull() ?: return@on

            last = b.fn0(c).literal().visualOrderText
        }

        on<SlayerEvent.Boss.Death> {
            last = null
        }

        on<SlayerEvent.Cleanup> {
            last = null
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