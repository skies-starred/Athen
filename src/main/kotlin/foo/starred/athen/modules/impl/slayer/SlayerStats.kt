@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.minecraft.text.renderer.VanillaFontRenderer
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.athen.utils.render.fcs
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.formatted
import foo.starred.snowbird.utils.toDuration
import net.minecraft.util.FormattedCharSequence
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick

@Load
@OnlyIn(skyblock = true)
object SlayerStats : Module(
    "Slayer stats",
    "Displays slayer session statistics.",
    ConfigCategory.SLAYER
) {
    private var `last$type`: SlayerBoss? = null
    private var `last$tier`: SlayerTier? = null

    private var kills = 0
    private var xp = 0
    private var start = 0L
    private var `start$quest` = 0L
    private var total = 0.0

    private val _unused by config.information("Reset command: <red>/athen reset slayerStats")
    private val displayOptions by config.multiSelector("Display options", listOf("Bosses killed", "Bosses/hr", "XP/hr", "Avg kill time", "Session time"), listOf(0, 1, 2, 3, 4))
    private val styleType by config.selector("Styling type", listOf("General", "Advanced"))

    private val general by config.group("General text style")
    private val `style$title` by general.input("Title style", "<red>Slayer Stats:")
    private val `style$general` by general.input("General style", "#name: <red>#number")
    private val _unused0 by general.variables("#name", "#number")

    private val advanced by config.group("Advanced text style")
    private val `style$killed` by advanced.input("Bosses killed", "Bosses: <red>#number")
    private val `style$bosses` by advanced.input("Bosses per hour", "Bosses/hr: <red>#number")
    private val `style$xp` by advanced.input("XP per hour", "XP/hr: <red>#number")
    private val `style$kill` by advanced.input("Kill times", "Kill: <red>#number")
    private val `style$session` by advanced.input("Session time", "Session: <red>#number")
    private val _unused1 by advanced.variables("#number")

    private val display = Ticking(2) {
        val t = (System.currentTimeMillis() - start) / 1000.0
        val d = t / 3600.0

        buildList {
            add(`style$title`.prs())

            if (0 in displayOptions)
                add((if (styleType == 1) `style$killed` else `style$general`.replace("#name", "Bosses"))
                    .replace("#number", "$kills").prs())

            if (1 in displayOptions)
                add((if (styleType == 1) `style$bosses` else `style$general`.replace("#name", "Bosses/hr"))
                    .replace("#number", (kills / d).formatted(false)).prs())

            if (2 in displayOptions)
                add((if (styleType == 1) `style$xp` else `style$general`.replace("#name", "XP/hr"))
                    .replace("#number", (xp / d).formatted(false)).prs())

            if (3 in displayOptions)
                add((if (styleType == 1) `style$kill` else `style$general`.replace("#name", "Kill"))
                    .replace("#number", (total / kills).toDuration(secondsDecimals = 1)).prs())

            if (4 in displayOptions)
                add((if (styleType == 1) `style$session` else `style$general`.replace("#name", "Session"))
                    .replace("#number", t.toDuration()).prs())
        }
    }

    init {
        config.hud("Stats display") {
            val example = listOf("§cSlayer Stats:", "Bosses: §c67", "Bosses/hr: §c104", "XP/hr: §c60,000", "Kill: §c23.4s", "Session: §c21m 24s").fcs

            constrain {
                VanillaFontMeasurer.constrain(example)
            }

            preview {
                VanillaFontRenderer.extract(graphics, example, 0, 0)
            }

            render {
                if (kills <= 0) return@render
                VanillaFontRenderer.extract(graphics, display.value ?: return@render, 0, 0)
            }
        }

        on<SlayerEvent.Quest.Start> {
            if (start == 0L) start = System.currentTimeMillis()
            `start$quest` = System.currentTimeMillis()
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.owned) return@on
            if (slayerInfo.type == SlayerBoss.Tarantula && slayerInfo.tier == SlayerTier.Five && slayerInfo.phase != 2) return@on

            kills++
            total += entity.tickCount / 20.0
            xp += slayerInfo.tier?.xp ?: 0

            val a = `last$type`
            val b = `last$tier`
            `last$type` = slayerInfo.type as SlayerBoss
            `last$tier` = slayerInfo.tier

            if ((a != null && a != `last$type`) || (b != null && b != `last$tier`)) {
                "<hover:<${MochaColorScheme.Red.argb}>This WILL clear all your stats!><${MochaColorScheme.Lavender.argb}>Detected a different slayer, click to reset stats.".parse()
                    .onClick {
                        reset()
                        "Slayer stats were reset!".mod()
                    }
                    .mod()
            }
        }

        on<SlayerEvent.Reset.QuestFail> {
            `start$quest` = 0
        }

        on<LocationEvent.Server.Connect> {
            reset()
        }

        command {
            "reset" / "slayerStats" {
                reset()
                "Slayer stats were reset!".mod()
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
