package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.abbreviate

@Load
@OnlyIn(islands = [SkyBlockIsland.CRIMSON_ISLE])
object VengeanceDamageTracker : Module(
    "Vengeance damage tracker",
    "Tracks your vengeance damage in chat.",
    Category.SLAYER
) {
    private val abbreviate by config.switch("Abbreviate damage")
    private val vengeanceRegex: Regex = Regex("""^(?<damage>\d+(?:,\d+)*)ﬗ$""")
    private var slayerEntity: Entity? = null

    init {
        on<SlayerEvent.Boss.Spawn> {
            if (!slayerInfo.isOwnedByPlayer) return@on
            slayerEntity = entity
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.isOwnedByPlayer) return@on
            slayerEntity = null
        }

        on<SlayerEvent.Cleanup> {
            slayerEntity = null
        }

        on<EntityEvent.NameChange> {
            val slayer = slayerEntity ?: return@on
            val entity = infoLineEntity as? ArmorStand ?: return@on
            val match = vengeanceRegex.findGroup(component.stripped(), "damage") ?: return@on

            if (entity.distanceTo(slayer) > 5) return@on

            val damage = match.replace(",", "").toLong()
            if (damage < 500_000) return@on

            val displayDamage = if (abbreviate) damage.abbreviate() else match
            "Vengeance -> <${Mocha.Red.argb}>$displayDamage".parse().modMessage()
        }
    }
}