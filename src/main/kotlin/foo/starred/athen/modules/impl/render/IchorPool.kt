package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.kuudra.KuudraAPI
import foo.starred.athen.api.kuudra.enums.KuudraPhase
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractStyledCircle
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractText
import foo.starred.athen.config.Category
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.command
import foo.starred.snowbird.utils.toDurationFromMillis
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull

@Load
@OnlyIn(skyblock = true)
object IchorPool : Module(
    "Ichor pool",
    "Highlights the area on Ichor Pool",
    Category.RENDER
) {
    private val onlyKuudra by config.switch("Only in Kuudra")
    private val notifyParty by config.switch("Notify party", true)
    private val prio by config.switch("Prioritize own", true)
    private val textColor by config.colorPicker("Text color", Catppuccin.Mocha.Teal.argb)
    private val color by config.colorPicker("Circle color", Catppuccin.Mocha.Sapphire.argb)
    private val style by config.selector("Circle style", listOf("Outline", "Filled", "Both"), 2)

    private val messageRegex = Regex("^Party > (?:\\[[^]]*?] )?\\w{1,16}(?: [ቾ⚒])?: Ichor pool casted at (?<x>-?\\d+) (?<y>-?\\d+) (?<z>-?\\d+)")
    private var pos: Vec3? = null
    private var time: Long = 0

    init {
        on<LocationEvent.Server.Connect> {
            reset()
        }

        on<MessageEvent.Chat.Receive> {
            if (onlyKuudra && KuudraAPI.phase != KuudraPhase.Kill) return@on
            if (pos == null || !prio) {
                messageRegex.findOrNull(stripped, "x", "y", "z") { (x, y, z) ->
                    pos = Vec3(x.toDouble(), y.toDouble() + 0.1, z.toDouble())
                    time = System.currentTimeMillis()
                }
            }

            if (pos != null && !prio) return@on
            if ("Casting Spell: Ichor Pool!" != stripped) return@on

            val n = client.player?.blockPosition() ?: return@on
            //~ if >= 26.2 'n.center' -> 'Vec3.atCenterOf(n)'
            pos = n.center.add(0.0, 0.1, 0.0)
            time = System.currentTimeMillis()

            val str = "${n.x} ${n.y} ${n.z}"
            "Ichor pool casted at <red>$str".mod()
            if (notifyParty && PartyAPI.inParty) "pc Ichor pool casted at $str".command(false)
        }

        on<WorldRenderEvent.Extract> {
            if (onlyKuudra && KuudraAPI.phase != KuudraPhase.Kill) return@on
            val center = pos ?: return@on
            val t = (20100 - (System.currentTimeMillis() - time)).takeIf { it > 0 } ?: return@on reset()

            extractStyledCircle(center, 8.0, color, style)
            extractText(t.toDurationFromMillis(), center, textColor, depth = false, increase = true)
        }
    }

    private fun reset() {
        time = 0
        pos = null
    }
}
