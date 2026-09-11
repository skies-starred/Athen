@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.kuudra.carry

import foo.starred.athen.Athen
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.kuudra.KuudraAPI
import foo.starred.athen.api.kuudra.enums.KuudraTier
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.sizedText
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.config.Category
import foo.starred.athen.config.dsl.impl.builders.hud.ConfigHudBuilder
import foo.starred.athen.events.KuudraEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.kuudra.carry.KuudraCarryStateTracker.tracked
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.athen.utils.command
import foo.starred.athen.utils.render.fcs
import foo.starred.athen.utils.render.renderBoundingBox
import foo.starred.snowbird.api.center
import foo.starred.snowbird.api.command
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.network.data.HttpRequest
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.toDuration
import tech.thatgravyboat.skyblockapi.helpers.McClient

@Load
@OnlyIn(skyblock = true)
object KuudraCarryTracker : Module(
    "Kuudra carry tracker",
    "Track kuudra carries and display progress.",
    Category.KUUDRA
) {
    private val announceInParty by config.switch("Announce in party", true)
    private val showStartMessage by config.switch("Show start message", true)

    private val _webhook by config.group("Discord webhook")
    private val webhook by _webhook.switch("Send to webhook")
    private val webhookEach by _webhook.switch("Send on each kill", true)
    private val webhookUrl by _webhook.input("Webhook URL")
    private val _webhookUrl by _webhook.information("Requires you to add your own webhook URL!")

    private val highlights by config.group("Highlights")
    private val highlightPlayer by highlights.switch("Highlight player", true)
    private val playerColor by highlights.colorPicker("Player color", Mocha.Blue.argb)
    private val playerLineWidth by highlights.slider("Player line width", 2f, 0f, 10f)

    private val ex0 = listOf("§f§lKuudra Carries:", "§7> §bExample §8[§7Infernal§8]§f: §b3§f/§b10 §7(5m 30s | 12/hr)").fcs
    private val hud: ConfigHudBuilder = config.hud("Kuudra carry display") {
        if (it) return@hud sizedText(ex0)
        sizedText(display.value ?: return@hud null)
    }

    private val `hud$kuudra` by config.switch("Only in Kuudra", true)

    private val tierMap = mapOf(
        "basic" to KuudraTier.BASIC,
        "hot" to KuudraTier.HOT,
        "burning" to KuudraTier.BURNING,
        "fiery" to KuudraTier.FIERY,
        "infernal" to KuudraTier.INFERNAL,
        "t1" to KuudraTier.BASIC,
        "t2" to KuudraTier.HOT,
        "t3" to KuudraTier.BURNING,
        "t4" to KuudraTier.FIERY,
        "t5" to KuudraTier.INFERNAL
    )

    private val display = Ticking {
        if (tracked.isEmpty()) return@Ticking null
        if (`hud$kuudra` && !SkyBlockIsland.KUUDRA.inIsland.value) return@Ticking null

        buildString {
            append("§f§lKuudra Carries:")
            for (i in tracked.values) append("\n${i.str()}")
        }.split("\n").fcs
    }

    init {
        command {
            "kcarry" / "add" / word("player").suggests { McClient.players.map { it.profile.name } } / int("amount", 1).suggests { listOf("1", "5", "10", "20") } / word("tier") {
                val player = string("player")
                val amount = int("amount")
                val tierInput = string("tier")

                val tier = tierMap[tierInput.lowercase()] ?: return@word "Invalid tier. Use: basic, hot, burning, fiery, infernal, or t1-t5.".mod()

                KuudraCarryStateTracker.addCarry(player, amount, tier)
            }.suggests { tierMap.keys.toList() }

            "kcarry" / "remove" / word("player") {
                KuudraCarryStateTracker.removeCarry(string("player"))
            }.suggests { tracked.keys }

            "kcarry" / "list" {
                KuudraCarryStateTracker.listCarries()
            }

            "kcarry" / "list" / "clear" {
                KuudraCarryStateTracker.clearCarries()
            }

            "kcarry" / "history" {
                KuudraCarryStateTracker.displayHistory(1)
            }

            "kcarry" / "history" / int("page", 1) {
                KuudraCarryStateTracker.displayHistory(int("page"))
            }.suggests { listOf("1", "2", "3", "4", "5") }

            "kcarry" / "help" {
                showHelp()
            }

            "kcarry" / "gui" {
                KuudraCarryGUI.open()
            }

            "kcarry" {
                KuudraCarryGUI.open()
            }
        }

        on<KuudraEvent.Start> {
            val tier = KuudraAPI.tier ?: return@on

            for (teammate in KuudraAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.tier != tier) continue

                if (showStartMessage) "Kuudra started for <aqua>${teammate.name}<gray> [${tier.str}]".mod()
            }
        }

        on<KuudraEvent.End.Success> {
            val tier = KuudraAPI.tier ?: return@on

            for (teammate in KuudraAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.tier != tier) continue

                val result = carry.onCompletion()

                "Completed run for <aqua>${teammate.name}".mod()
                if (announceInParty) "pc ${teammate.name}: ${result.current}/${result.total}".command(false)
                if (webhookEach && webhook) {
                    webhookUrl.request(HttpRequest.POST) {
                        body(mapOf("content" to "Completed ${result.amount}/${result.total} ${tier.str} carries for ${teammate.name}"))
                    }
                }

                if (result.completed) {
                    val time = result.totalTime.toDuration()
                    "<${Mocha.Green.argb}>Completed carries for <aqua>${teammate.name} <gray>[${tier.str}] <r>in <yellow>$time".mod()

                    if (webhook) {
                        webhookUrl.request(HttpRequest.POST) {
                            body(mapOf("content" to "Completed ${result.amount}x ${tier.str} carries for ${teammate.name} ($time)"))
                        }
                    }

                    KuudraCarryStateTracker.add(teammate.name, result.amount, carry.type)
                    tracked.remove(teammate.name)
                }

                KuudraCarryStateTracker.persist()
            }
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightPlayer) return@on
            if (tracked.isEmpty()) return@on
            if (!KuudraAPI.inRun) return@on

            for (teammate in KuudraAPI.teammates) {
                if (teammate.name !in tracked) continue
                val e = teammate.entity ?: continue
                extractFrameBox(e.renderBoundingBox, playerColor, playerLineWidth, false)
            }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)
    }

    private fun showHelp() {
        val commands = listOf(
            "/${Athen.modId} kcarry" to "Open the kuudra carry tracker GUI",
            "/${Athen.modId} kcarry add <player> <amount> <tier>" to "Add kuudra carries to track",
            "/${Athen.modId} kcarry remove <player>" to "Remove a tracked player",
            "/${Athen.modId} kcarry list" to "List players being tracked",
            "/${Athen.modId} kcarry list clear" to "Clear the active list",
            "/${Athen.modId} kcarry history [page=1]" to "Show tracked history"
        )

        val divider = ("§8§m" + ("-".repeat())).literal()

        divider.lie()
        "§bAthen Kuudra Carry Commands".center().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }
}
