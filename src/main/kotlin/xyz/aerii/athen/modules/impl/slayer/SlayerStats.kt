@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.util.FormattedCharSequence
import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerMob
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Ticking
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.fcs
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.formatted
import xyz.aerii.library.utils.literal
import xyz.aerii.library.utils.toDuration

@Load
@OnlyIn(skyblock = true)
object SlayerStats : Module(
    "Slayer stats",
    "Displays slayer session statistics.",
    Category.SLAYER
) {
    private val tierXp = mapOf(1 to 5, 2 to 25, 3 to 100, 4 to 500, 5 to 1500)
    private var `last$type`: SlayerMob? = null
    private var `last$tier`: Int? = null

    private var kills = 0
    private var xp = 0
    private var start = 0L
    private var `start$quest` = 0L
    private var total = 0.0

    private val ex0 = listOf("§cSlayer Stats:", "Bosses: §c67", "Bosses/hr: §c104", "XP/hr: §c60,000", "Kill: §c23.4s", "Session: §c21m 24s").fcs

    @Suppress("UNUSED")
    private val _unused0 by config.textParagraph("Use <red>/${Athen.modId} reset slayerStats<r> to reset.")
    private val displayOptions by config.multiCheckbox("Display options", listOf("Bosses killed", "Bosses/hr", "XP/hr", "Avg kill time", "Session time"), listOf(0, 1, 2, 3, 4))

    private val styleExpandable by config.expandable("Text style")
    private val `style$advanced` by config.switch("Advanced styling").childOf { styleExpandable }
    private val `style$title` by config.textInput("Title style", "<red>Slayer Stats:").childOf { styleExpandable }
    private val `style$general` by config.textInput("General style", "#name: <red>#number").dependsOn { !`style$advanced` }.childOf { styleExpandable }

    private val `style$killed` by config.textInput("Bosses killed", "Bosses: <red>#number").dependsOn { `style$advanced` }.childOf { styleExpandable }
    private val `style$bosses` by config.textInput("Bosses per hour", "Bosses/hr: <red>#number").dependsOn { `style$advanced` }.childOf { styleExpandable }
    private val `style$xp` by config.textInput("XP per hour", "XP/hr: <red>#number").dependsOn { `style$advanced` }.childOf { styleExpandable }
    private val `style$kill` by config.textInput("Kill times", "Kill: <red>#number").dependsOn { `style$advanced` }.childOf { styleExpandable }
    private val `style$session` by config.textInput("Session time", "Session: <red>#number").dependsOn { `style$advanced` }.childOf { styleExpandable }

    private val display = Ticking(2) {
        val t = (System.currentTimeMillis() - start) / 1000.0
        val d = t / 3600.0

        buildList {
            add(`style$title`.prs())

            if (0 in displayOptions)
                add((if (`style$advanced`) `style$killed` else `style$general`.replace("#name", "Bosses"))
                    .replace("#number", "$kills").prs())

            if (1 in displayOptions)
                add((if (`style$advanced`) `style$bosses` else `style$general`.replace("#name", "Bosses/hr"))
                    .replace("#number", (kills / d).formatted(false)).prs())

            if (2 in displayOptions)
                add((if (`style$advanced`) `style$xp` else `style$general`.replace("#name", "XP/hr"))
                    .replace("#number", (xp / d).formatted(false)).prs())

            if (3 in displayOptions)
                add((if (`style$advanced`) `style$kill` else `style$general`.replace("#name", "Kill"))
                    .replace("#number", (total / kills).toDuration(secondsDecimals = 1)).prs())

            if (4 in displayOptions)
                add((if (`style$advanced`) `style$session` else `style$general`.replace("#name", "Session"))
                    .replace("#number", t.toDuration()).prs())
        }
    }

    init {
        config.hud("Stats display") {
            if (it) return@hud sizedText(ex0)
            if (kills <= 0) return@hud null
            sizedText(display.value ?: return@hud null)
        }

        on<SlayerEvent.Quest.Start> {
            if (start == 0L) start = System.currentTimeMillis()
            `start$quest` = System.currentTimeMillis()
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.isOwnedByPlayer) return@on

            kills++
            total += entity.tickCount / 20.0
            xp += tierXp[slayerInfo.tier] ?: 0

            val a = `last$type`
            val b = `last$tier`
            `last$type` = slayerInfo.type
            `last$tier` = slayerInfo.tier

            if ((a != null && a != `last$type`) || (b != null && b != `last$tier`)) {
                "Detected a different slayer, click to reset stats.".literal().withColor(Mocha.Lavender.argb)
                    .onHover("This WILL clear all your stats".literal().withColor(Mocha.Red.argb))
                    .onClick {
                        reset()
                        "Slayer stats were reset!".notify()
                    }
                    .modMessage()
            }
        }

        on<SlayerEvent.Reset.QuestFail> {
            `start$quest` = 0
        }

        on<LocationEvent.Server.Connect> {
            reset()
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("reset") {
                    thenCallback("slayerStats") {
                        reset()
                        "Slayer stats were reset!".notify()
                    }
                }
            }
        }
    }

    private fun String.prs(): FormattedCharSequence =
        parse(true).visualOrderText

    private fun reset() {
        kills = 0
        xp = 0
        start = 0
        `start$quest` = 0

        total = 0.0
        `last$type` = null
        `last$tier` = null
    }
}