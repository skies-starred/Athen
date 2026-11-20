package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.skyblock.entity.duck.EntityDuck.Companion.parent
import foo.starred.athen.api.slayers.SlayerAPI
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.EntityEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.data.Observable
import foo.starred.kontext.api.format.extension.time.parseDuration
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver

@LoadableAthenModule
@AthenModuleScope(islands = [PresetSkyBlockIsland.CRIMSON_ISLE])
object VengeanceTimer : Module(
    "Vengeance timer",
    "Shows the time until your vengeance damage should activate.",
    ConfigCategory.SLAYER
) {
    private val compact = config.switch("Compact display").unique("compact")
    private val useTicks by config.switch("Use ticks", true)
    private val abilityIds = listOf("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER")
    private var count: Observable<Boolean> = Observable(false)
    private var countDown: Int = 120

    private val hud by config.hud("Vengeance") {
        constrain {
            MinecraftFontMeasurer.constrain(if (compact.value) "120" else "§cVengeance: §f120")
        }

        preview {
            MinecraftFontRenderer.extract(graphics, if (compact.value) "120" else "§cVengeance: §f120", 0, 0)
        }

        render {
            if (!count.value) return@render

            val value = if (useTicks) "$countDown" else (countDown / 20.0).parseDuration(decimals = 1)
            MinecraftFontRenderer.extract(graphics, if (compact.value) value else "§cVengeance: §f$value", 0, 0)
        }
    }

    init {
        compact.state.observe {
            hud.constrain()
        }

        on<TickEvent.Server> {
            if (countDown > 0) countDown-- else reset()
        }.runWhen(count)

        on<EntityEvent.Update.Named> {
            if (count.value) return@on
            val entity = entity.parent ?: return@on
            val slayerInfo = SlayerAPI.bosses[entity] ?: return@on

            if (!slayerInfo.owned) return@on
            if (!stripped.contains("ASHEN ♨7")) return@on
            if (client.player?.mainHandItem?.get<GenericKontextItemDataResolver.ID, String>() !in abilityIds) return@on

            count.value = true
        }

        on<SlayerEvent.Boss.Death> {
            reset()
        }

        on<SlayerEvent.Reset.Any> {
            reset()
        }
    }

    private fun reset() {
        count.value = false
        countDown = 120
    }
}
