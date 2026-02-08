package xyz.aerii.athen.modules.impl.slayer

import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerMob
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDuration

@Load
@OnlyIn(skyblock = true)
object SlayerStats : Module(
    "Slayer stats",
    "Displays slayer session statistics.",
    Category.SLAYER
) {
    private val tierXp = mapOf(1 to 5, 2 to 25, 3 to 100, 4 to 500, 5 to 1500)
    private var displayString: String? = null
    private var lastSlayerType: SlayerMob? = null
    private var lastSlayerTier: Int? = null

    private var bossesKilled = 0
    private var totalXp = 0
    private var sessionStart = 0L
    private var questStartTime = 0L
    private var totalKillTime = 0.0

    @Suppress("UNUSED")
    private val _unused0 by config.textParagraph("Use <red>/${Athen.modId} reset slayerStats<r> to reset.")
    private val displayOptions by config.multiCheckbox(
        "Display options",
        listOf("Bosses killed", "Bosses/hr", "XP/hr", "Avg kill time", "Session time"),
        listOf(0, 1, 2, 3, 4)
    )

    private val showBossesKilled get() = 0 in displayOptions
    private val showBossesPerHour get() = 1 in displayOptions
    private val showXpPerHour get() = 2 in displayOptions
    private val showAvgKillTime get() = 3 in displayOptions
    private val showSessionTime get() = 4 in displayOptions

    init {
        config.hud("Stats display") {
            when {
                it -> sizedText(str1())
                bossesKilled > 0 && displayString != null -> sizedText(displayString!!)
                else -> null
            }
        }

        Chronos.Tick every 2 repeat {
            if (!react.value) return@repeat
            displayString = str()
        }

        on<SlayerEvent.Quest.Start> {
            if (sessionStart == 0L) sessionStart = System.currentTimeMillis()
            questStartTime = System.currentTimeMillis()
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.isOwnedByPlayer) return@on

            bossesKilled++
            totalKillTime += entity.tickCount / 20.0
            totalXp += tierXp[slayerInfo.tier] ?: 0

            val a = lastSlayerType
            val b = lastSlayerTier
            lastSlayerType = slayerInfo.type
            lastSlayerTier = slayerInfo.tier

            if ((a != null && a != lastSlayerType) || (b != null && b != lastSlayerTier)) {
                "Detected a different slayer, click to reset stats.".literal().withColor(Mocha.Lavender.argb)
                    .onHover("This WILL clear all your stats".literal().withColor(Mocha.Red.argb))
                    .onClick {
                        reset()
                        "Slayer stats were reset!".notify()
                    }
                    .modMessage()
            }
        }

        on<SlayerEvent.Cleanup> {
            questStartTime = 0
        }

        on<LocationEvent.ServerConnect> {
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

    private fun str(): String {
        val lines = mutableListOf("§f§lSlayer Stats:")
        val now = System.currentTimeMillis()
        val sessionTimeSeconds = (now - sessionStart) / 1000.0
        val hours = sessionTimeSeconds / 3600.0

        if (showBossesKilled) {
            lines.add("§7Bosses: §b$bossesKilled")
        }

        if (showBossesPerHour && hours > 0) {
            val bossesPerHour = (bossesKilled / hours).toInt()
            lines.add("§7Bosses/hr: §b$bossesPerHour")
        }

        if (showXpPerHour && hours > 0) {
            val xpPerHour = (totalXp / hours).toInt()
            lines.add("§7XP/hr: §b${xpPerHour.formatNumber()}")
        }

        if (showAvgKillTime && bossesKilled > 0) {
            val avgKill = (totalKillTime / bossesKilled).toDuration(secondsDecimals = 1)
            lines.add("§7Avg: §b$avgKill")
        }

        if (showSessionTime) {
            lines.add("§7Session: §b${sessionTimeSeconds.toDuration()}")
        }

        return lines.joinToString("\n")
    }

    private fun str1(): String {
        val lines = mutableListOf("§f§lSlayer Stats:")

        if (showBossesKilled) lines.add("§7Bosses: §b42")
        if (showBossesPerHour) lines.add("§7Bosses/hr: §b120")
        if (showXpPerHour) lines.add("§7XP/hr: §b60,000")
        if (showAvgKillTime) lines.add("§7Avg: §b12.5s")
        if (showSessionTime) lines.add("§7Session: §b21m 30s")

        return lines.joinToString("\n")
    }

    private fun reset() {
        bossesKilled = 0
        totalXp = 0
        sessionStart = 0
        questStartTime = 0
        totalKillTime = 0.0
        lastSlayerType = null
        lastSlayerTier = null
    }

    private fun Int.formatNumber() = "%,d".format(this)
}