package xyz.aerii.athen.modules.impl.kuudra

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPod
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.PlayerEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render3D
import java.awt.Color

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object StunHelper : Module(
    "Stun helper",
    "Helper features for stunning in kuudra.",
    Category.KUUDRA
) {
    private val highlightPod by config.switch("Highlight pods", true)
    private val boxColor by config.colorPicker("Color", Color(Catppuccin.Mocha.Sapphire.argb, true)).dependsOn { highlightPod }
    private val depthTest by config.switch("Depth test", true).dependsOn { highlightPod }
    private val blockAbility by config.switch("Block pickaxe ability", true)
    private val blockType by config.dropdown("Block when", listOf("Outside belly", "Wrong aim inside belly", "Both"), 2).dependsOn { blockAbility }

    private val stunRegex = Regex("^\\w+ destroyed one of Kuudra's pods!$")

    private var stunning = false
    private var belly = false

    init {
        on<LocationEvent.ServerConnect> {
            reset()
        }

        on<PlayerEvent.Interact> {
            if (!blockAbility) return@on
            if (!KuudraAPI.inRun) return@on
            if (item.getData(DataTypes.COOLDOWN_ABILITY)?.first != "Pickobulus") return@on

            val tier = KuudraAPI.tier?.int ?: return@on
            if (tier < KuudraTier.BURNING.int) return@on

            if ((blockType == 0 || blockType == 2) && !belly) return@on ccl()
            if ((blockType == 1 || blockType == 2) && (!stunning || !belly)) return@on
            val player = client.player ?: return@on

            val eye = player.eyePosition
            val end = eye.add(player.lookAngle.scale(32.0))

            val result = player.level().clip(ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
            if (result.type != HitResult.Type.BLOCK) return@on ccl()

            val pos = (result as BlockHitResult).blockPos
            val pod = KuudraPod.entries.any { pod ->
                val box = pod.aabb
                pos.x >= box.minX && pos.x <= box.maxX &&
                pos.y >= box.minY && pos.y <= box.maxY &&
                pos.z >= box.minZ && pos.z <= box.maxZ
            }

            if (!pod) ccl()
        }

        on<MessageEvent.Chat.Receive> {
            if (!KuudraAPI.inRun) return@on

            val tier = KuudraAPI.tier?.int ?: return@on
            if (tier < KuudraTier.BURNING.int) return@on

            if (stunning && belly) {
                stunRegex.findOrNull(stripped) { reset() }
                return@on
            }

            if (!stunning &&
                KuudraAPI.inRun &&
                stripped == "You purchased Human Cannonball!"
            ) {
                stunning = true
                fn()
            }
        }

        on<PacketEvent.Receive, ClientboundPlayerPositionPacket> {
            if (!stunning) return@on
            if (belly) return@on

            val p = change.position ?: return@on
            if (Mth.floor(p.x) == -161 && Mth.floor(p.y) == 49 && Mth.floor(p.z) == -186) belly = true
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightPod) return@on
            if (!stunning) return@on
            if (!belly) return@on

            for (p in KuudraPod.entries) Render3D.drawBox(p.aabb, boxColor, depthTest = depthTest)
        }
    }

    private fun CancellableEvent.ccl() {
        cancel()
        "Blocked pickaxe ability!".modMessage()
    }

    private fun reset() {
        belly = false
        stunning = false
    }

    private fun fn() {
        // :3
    }
}