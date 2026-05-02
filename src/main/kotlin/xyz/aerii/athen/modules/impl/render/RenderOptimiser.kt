package xyz.aerii.athen.modules.impl.render

import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.modules.Module

@Load
object RenderOptimiser :  Module(
    "Render optimiser",
    "Cleans up rendering stuff, and maybe optimizes a bit.",
    Category.RENDER
) {
    private val _arm by config.switch("Hide player arm")
    private val _effects by config.switch("Hide effects in UI", true)

    private val armor0 = config.switch("Hide armor on players").custom("_armor0")
    private val armor0t by config.multiCheckbox("Players to hide for", listOf("Self", "Others"), listOf(0)).dependsOn { armor0.value }
    private val armor1 = config.switch("Hide armor on all mobs").custom("_armor1")

    private val _glow by config.switch("Hide glowing effect")
    private val _fog by config.switch("Hide fog", true)
    private val lavaOverlay by config.switch("Hide lava overlay", true)
    private val fireOverlay by config.switch("Hide fire overlay", true)
    private val entityFire = config.switch("Hide fire on entity", true).dependsOn { fireOverlay }.custom("hideEntityFire")

    @JvmStatic
    val fire: Boolean
        get() = enabled && fireOverlay

    @JvmStatic
    val lava: Boolean
        get() = enabled && lavaOverlay

    @JvmStatic
    val fog: Boolean
        get() = enabled && _fog

    @JvmStatic
    val arm: Boolean
        get() = enabled && _arm

    @JvmStatic
    val glow: Boolean
        get() = enabled && _glow

    @JvmStatic
    val effects: Boolean
        get() = enabled && _effects

    init {
        on<WorldRenderEvent.Entity.Pre> {
            renderState.displayFireAnimation = false
        }.runWhen(entityFire.state)

        on<WorldRenderEvent.Entity.Pre> {
            val r = renderState as? AvatarRenderState ?: return@on
            val e = entity ?: return@on

            val a = armor0t
            val b = 0 in a && e is LocalPlayer
            val c = 1 in a && e is Player && e !is LocalPlayer
            if (!b && !c) return@on

            r.fn0()
        }.runWhen(armor0.state)

        on<WorldRenderEvent.Entity.Pre> {
            val r = renderState as? HumanoidRenderState ?: return@on
            if (r is AvatarRenderState) return@on

            r.fn0()
        }.runWhen(armor1.state)
    }

    fun HumanoidRenderState.fn0() {
        headEquipment = ItemStack.EMPTY
        chestEquipment = ItemStack.EMPTY
        legsEquipment = ItemStack.EMPTY
        feetEquipment = ItemStack.EMPTY
    }
}