package xyz.aerii.athen.modules.impl.slayer

import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.helpers.getAttachedTo
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.api.skyblock.SlayerAPI
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDuration

@Load
@OnlyIn(islands = [SkyBlockIsland.CRIMSON_ISLE])
object VengeanceTimer : Module(
    "Vengeance timer",
    "Shows the time until your vengeance damage should activate.",
    Category.SLAYER
) {
    private val compact by config.switch("Compact display")
    private val useTicks by config.switch("Use ticks", true)
    private val abilityIds = listOf("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER")
    private var count: React<Boolean> = React(false)
    private var countDown: Int = 120

    init {
        config.hud("Vengeance") {
            if (!count.value && !it) return@hud null

            val value = when {
                it && useTicks -> "120"
                it -> "6s"
                useTicks -> "$countDown"
                else -> (countDown / 20.0).toDuration(secondsDecimals = 1)
            }

            sizedText(if (compact) value else "§cVengeance: §f$value")
        }

        on<TickEvent.Server> {
            if (countDown > 0) countDown-- else reset()
        }.runWhen(count)

        on<EntityEvent.NameChange> {
            if (count.value) return@on
            val entity = infoLineEntity.getAttachedTo() ?: return@on
            val slayerInfo = SlayerAPI.slayerBosses[entity] ?: return@on

            if (!slayerInfo.isOwnedByPlayer) return@on
            if (!component.stripped().contains("ASHEN ♨7")) return@on
            if (Smoothie.heldItem?.getData(DataTypes.SKYBLOCK_ID)?.skyblockId !in abilityIds) return@on

            count.value = true
        }

        on<LocationEvent.ServerConnect> {
            reset()
        }
    }

    private fun reset() {
        count.value = false
        countDown = 120
    }
}