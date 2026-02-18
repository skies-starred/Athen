@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Commander
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Smoothie.showTitle
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.fromLongDuration
import xyz.aerii.athen.utils.toDurationFromMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@Load
@OnlyIn(skyblock = true)
object KatReminder : Module(
    "Kat reminder",
    "Reminds you about your pet that you gave to upgrade to Kat!",
    Category.GENERAL
) {
    private val showTitle by config.switch("Show title", true)
    private val message by config.textInput("Alert message", "<red>#pet<white> is waiting for you at Kat!")
    private val _unused0 by config.textParagraph("Variable: <red>#pet")
    private val _unused1 by config.textParagraph("Commands: <red>/athen times kat<r>, <red>/athen clear kat")

    private val scribble = Scribble("features/katReminder")
    private var pet by scribble.string("pet")
    private var time by scribble.long("time")

    private val giveRegex = Regex("^\\[NPC] Kat: I'll get your (?<pet>\\w+) upgraded to \\w+ in no time!$")
    private val durationRegex = Regex("^\\[NPC] Kat: Come back in (?<duration>.+) to pick it up!$")

    private val remindRegex = Regex("^\\[NPC] Kat: I'm currently taking care of your (?<pet>\\w+)!$")
    private val durationRemindRegex = Regex("^\\[NPC] Kat: You can pick it up in (?<duration>.+)\\.$")

    private var task: Chronos.Task? = null

    init {
        fn()

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("clear") {
                    thenCallback("kat") {
                        if (!Commander.StateTracker.`warning$kat$sentOnce`) {
                            "<red>This WILL clear your Kat time info! Only use this if you know what you're doing.".parse().modMessage(Typo.PrefixType.ERROR)
                            Commander.StateTracker.`warning$kat$sentOnce` = true
                            return@thenCallback
                        }

                        reset()
                        "Kat time info was successfully reset.".modMessage(Typo.PrefixType.SUCCESS)
                        Commander.StateTracker.`warning$kat$sentOnce` = false
                    }
                }

                then("times") {
                    thenCallback("kat") {
                        if (time == 0L) return@thenCallback "No pet being upgraded!".modMessage(Typo.PrefixType.ERROR)

                        val d = (time - System.currentTimeMillis()).toDurationFromMillis()
                        "Time until upgrade for <red>$pet<r>: <red>$d".parse().modMessage()
                    }
                }
            }
        }

        on<LocationEvent.SkyBlockJoin> {
            if (time <= 0) return@on
            if (time > System.currentTimeMillis()) return@on

            Chronos.Tick after 10 then {
                fn0()
            }
        }

        on<MessageEvent.Chat> {
            if ("[NPC] Kat: " !in stripped) return@on

            when (stripped) {
                "[NPC] Kat: A flower? For me? How sweet!" -> {
                    fn1(1.days)
                    return@on
                }

                "[NPC] Kat: A bouquet? For me? How sweet!" -> {
                    fn1(5.days)
                    return@on
                }

                "[NPC] Kat: If you have any other pets you'd like to upgrade, you know where to find me!" -> {
                    reset()
                    return@on
                }
            }

            giveRegex.findThenNull(stripped, "pet") { (p) ->
                pet = p
            } ?: return@on

            remindRegex.findThenNull(stripped, "pet") { (p) ->
                pet = p
            } ?: return@on

            durationRegex.findThenNull(stripped, "duration") { (d) ->
                val s = d.fromLongDuration().toLong()
                time = System.currentTimeMillis() + (s * 1000L)
                fn()
            } ?: return@on

            durationRemindRegex.findThenNull(stripped, "duration") { (d) ->
                val s = d.fromLongDuration().toLong()
                time = System.currentTimeMillis() + (s * 1000L)
                fn()
            } ?: return@on
        }.runWhen(SkyBlockIsland.HUB.inIsland)
    }

    private fun fn() {
        task?.cancel()

        val delay = (time - System.currentTimeMillis()).takeIf { it > 0 } ?: return
        task = Chronos.Time after delay.milliseconds then { fn0() }
    }

    private fun fn1(days: Duration) {
        if (time <= 0L) return

        time -= days.inWholeMilliseconds
        if (time > System.currentTimeMillis()) return fn()

        task?.cancel()
        fn0()
    }

    private fun fn0() {
        if (pet.isEmpty()) return

        val str = message.replace("#pet", pet).parse()

        if (showTitle) str.showTitle(fadeIn = 10, stay = 40, fadeOut = 10)
        str.modMessage()
    }

    private fun reset() {
        pet = ""
        time = 0
    }
}