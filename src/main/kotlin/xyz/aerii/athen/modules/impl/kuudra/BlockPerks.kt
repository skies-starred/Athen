package xyz.aerii.athen.modules.impl.kuudra

import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.isPressed

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

    private val cancelRender = config.switch("Cancel slot render", true).custom("cancelRender")
    private val key by config.keybind("Override key")

    private val blockedExpandable by config.expandable("Blocked perks")
    private val perks0 = config.multiCheckbox("Cannoneer", cannoneer).childOf { blockedExpandable }.custom("cannoneer")
    private val perks1 = config.multiCheckbox("Crowd control", crowd).childOf { blockedExpandable }.custom("crowd")
    private val perks2 = config.multiCheckbox("Specialist", specialist, listOf(2, 3)).childOf { blockedExpandable }.custom("specialist")
    private val perks3 = config.multiCheckbox("Support", support).childOf { blockedExpandable }.custom("support")
    private val perks4 = config.multiCheckbox("Basic", basic, listOf(0, 2, 3)).childOf { blockedExpandable }.custom("basic")

    private var blocked: Set<String> = fn()
    private var inGui: Boolean = false

    init {
        perks0.state.onChange(::r)
        perks1.state.onChange(::r)
        perks2.state.onChange(::r)
        perks3.state.onChange(::r)
        perks4.state.onChange(::r)

        on<PacketEvent.Receive, ClientboundOpenScreenPacket> {
            inGui = title.stripped() == "Perk Menu"
        }

        on<PacketEvent.Receive, ClientboundContainerClosePacket> {
            inGui = false
        }

        on<PacketEvent.Send, ServerboundContainerClosePacket> {
            inGui = false
        }

        on<GuiEvent.Slots.Render.Pre> {
            if (!inGui) return@on
            if (!key.isPressed()) return@on

            val name = slot.item?.hoverName?.stripped()?.substringBeforeLast(" ") ?: return@on
            if (name in blocked) cancel()
        }.runWhen(cancelRender.state)

        on<GuiEvent.Slots.Click> {
            if (!inGui) return@on
            if (!key.isPressed()) return@on

            val name = slot?.item?.hoverName?.stripped()?.substringBeforeLast(" ") ?: return@on
            if (name in blocked) cancel()
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