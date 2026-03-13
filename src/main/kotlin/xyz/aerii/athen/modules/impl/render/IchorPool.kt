package xyz.aerii.athen.modules.impl.render

import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.toDurationFromMillis
import java.awt.Color

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
    private val textColor by config.colorPicker("Text color", Color(Catppuccin.Mocha.Teal.argb, true))
    private val color by config.colorPicker("Circle color", Color(Catppuccin.Mocha.Sapphire.argb, true))
    private val style by config.dropdown("Circle style", listOf("Outline", "Filled", "Both"), 2)

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
            if ("Casting Spell: Ichor Pool!" !in stripped) return@on

            val n = client.player?.blockPosition() ?: return@on
            pos = n.center.add(0.0, 0.1, 0.0)
            time = System.currentTimeMillis()

            val str = "${n.x} ${n.y} ${n.z}"
            "Ichor pool casted at <red>$str".parse().modMessage()
            if (notifyParty && PartyAPI.inParty) "pc Ichor pool casted at $str".command()
        }

        on<WorldRenderEvent.Extract> {
            if (onlyKuudra && KuudraAPI.phase != KuudraPhase.Kill) return@on
            val center = pos ?: return@on
            val t = (20100 - (System.currentTimeMillis() - time)).takeIf { it > 0 } ?: return@on reset()

            Render3D.drawStyledCircle(center, 8.0, color, style.get())
            Render3D.drawString(t.toDurationFromMillis(), center, textColor.rgb, depthTest = false, increase = true)
        }
    }

    private fun reset() {
        time = 0
        pos = null
    }

    private fun Int.get() = when (this) {
        0 -> Render3D.CircleStyle.OUTLINED
        1 -> Render3D.CircleStyle.FILLED
        else -> Render3D.CircleStyle.BOTH
    }
}