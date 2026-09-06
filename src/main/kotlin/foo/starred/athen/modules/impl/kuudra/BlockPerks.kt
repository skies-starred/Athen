package foo.starred.athen.modules.impl.kuudra

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.bound
import foo.starred.snowbird.api.pressed
import foo.starred.snowbird.utils.stripped

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object BlockPerks : Module(
    "Block perks",
    "Blocks certain perks in the kuudra perk gui so you don't click them!",
    Category.KUUDRA
) {
    private val cannoneer = listOf("Accelerated Shot", "Blast Radius", "Cannon Proficiency", "Multi-Shot", "Rapid Fire", "Steady Aim")
    private val crowd = listOf("Sweeping Edge", "Freezing Touch", "Bonus Damage", "Antibiotic", "Blight Slayer")
    private val specialist = listOf("Steady Hands", "Ballista Mechanic", "Bomberman", "Mining Frenzy")
    private val support = listOf("Healing Aura", "Mana Aura", "Protective Aura", "Faster Respawn")
    private val basic = listOf("Auto Revive", "Human Cannonball", "Elle's Lava Rod", "Elle's Pickaxe")

    private val cancelRender = config.switch("Cancel slot render", true).unique("cancelRender")
    private val key by config.keybind("Override key")

    private val blocks by config.group("Blocked perks")
    private val perks0 = blocks.multiSelector("Cannoneer", cannoneer).unique("cannoneer")
    private val perks1 = blocks.multiSelector("Crowd control", crowd).unique("crowd")
    private val perks2 = blocks.multiSelector("Specialist", specialist, listOf(2, 3)).unique("specialist")
    private val perks3 = blocks.multiSelector("Support", support).unique("support")
    private val perks4 = blocks.multiSelector("Basic", basic, listOf(0, 2, 3)).unique("basic")

    private var blocked: Set<String> = fn()
    private var menu: Boolean = false

    init {
        perks0.state.onChange(::r)
        perks1.state.onChange(::r)
        perks2.state.onChange(::r)
        perks3.state.onChange(::r)
        perks4.state.onChange(::r)

        on<GuiEvent.Open.Container> {
            menu = stripped == "Perk Menu"
        }

        on<GuiEvent.Close.Container> {
            menu = false
        }

        on<GuiEvent.Slots.Render.Any.Pre> {
            if (!menu) return@on
            if (!key.bound) return@on
            if (key.pressed) return@on

            val name = slot.item.hoverName.stripped().substringBeforeLast(" ").takeIf { it.isNotEmpty() } ?: return@on
            if (name !in blocked) return@on

            cancel()
        }.runWhen(cancelRender.state)

        on<GuiEvent.Slots.Input.Click> {
            if (!menu) return@on
            if (!key.bound) return@on
            if (key.pressed) return@on

            val name = slot?.item?.hoverName?.stripped()?.substringBeforeLast(" ")?.takeIf { it.isNotEmpty() } ?: return@on
            if (name !in blocked) return@on

            cancel()
        }
    }

    @Suppress("Unused")
    private fun r(a: Any) {
        blocked = fn()
    }

    private fun fn() = buildSet {
        addAll(perks0.value.map { cannoneer[it] })
        addAll(perks1.value.map { crowd[it] })
        addAll(perks2.value.map { specialist[it] })
        addAll(perks3.value.map { support[it] })
        addAll(perks4.value.map { basic[it] })
    }
}
