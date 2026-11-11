package foo.starred.athen.modules.impl.kuudra

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.kuudra.KuudraAPI
import foo.starred.athen.api.kuudra.enums.KuudraPod
import foo.starred.athen.api.kuudra.enums.KuudraTier
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.*
import foo.starred.athen.modules.Module
import foo.starred.kbus.data.event.traits.KBusCancellableTrait
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.kontext.api.entity.extension.renderVec3
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import foo.starred.parallax.api.primitives.ParallaxBox
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

@LoadableAthenModule
@AthenModuleScope(islands = [PresetSkyBlockIsland.KUUDRA])
object StunHelper : Module(
    "Stun helper",
    "Helper features for stunning in kuudra.",
    ConfigCategory.KUUDRA
) {
    private val highlightPod by config.switch("Highlight pods", true)
    private val highlightSpecific by config.switch("Highlight exact block")
    private val pod by config.selector("Exact pod", listOf("Left", "Middle", "Right"))
    private val boxColor by config.colorPicker("Color", MochaColorScheme.Sapphire.argb)
    private val depthTest by config.switch("Depth test", true)
    private val blockAbility by config.switch("Block pickaxe ability", true)
    private val blockOverride by config.keybind("Block override key")
    private val blockType by config.selector("Block when", listOf("Outside belly", "Wrong aim inside belly", "Both"), 2)

    private val stunRegex = Regex("^\\w+ destroyed one of Kuudra's pods!$")

    private var stunning = false
    private var belly = false
    private var last = 0L

    init {
        on<LocationEvent.Server.Connect> {
            reset()
        }

        on<PlayerEvent.Interact.Any> {
            if (!blockAbility) return@on
            if (!KuudraAPI.inRun) return@on

            val item = client.player?.getItemInHand(InteractionHand.MAIN_HAND) ?: return@on
            if (item.get<GenericKontextItemDataResolver.LORE, List<Component>>()?.any { "Ability: Pickobulus" in it.string } != true) return@on
            if (GenericInputState.pressed(blockOverride)) return@on

            val tier = KuudraAPI.tier?.int ?: return@on
            if (tier < KuudraTier.BURNING.int) return@on

            if ((blockType == 0 || blockType == 2) && !belly) return@on ccl()
            if ((blockType == 1 || blockType == 2) && (!stunning || !belly)) return@on
            val player = client.player ?: return@on

            val eye = player.eyePosition
            val end = eye.add(player.lookAngle.scale(32.0))

            val result = player.level().clip(ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
            if (result.type != HitResult.Type.BLOCK) return@on ccl()

            val pos = result.blockPos
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

            if (stunning) {
                if (stunRegex.containsMatchIn(stripped)) reset()
                return@on
            }

            if (KuudraAPI.inRun && stripped == "You purchased Human Cannonball!") {
                stunning = true
                fn()
            }
        }

        on<PacketEvent.Receive, ClientboundPlayerPositionPacket> {
            if (!stunning) return@on
            if (belly) return@on

            val p = change.position
            if (Mth.floor(p.x) == -161 && Mth.floor(p.y) == 49 && Mth.floor(p.z) == -186) belly = true
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightPod && !highlightSpecific) return@on
            if (!stunning) return@on
            val player = client.player ?: return@on
            val selected = fn0()
            val offset =
                if (!belly) player.renderVec3().subtract(-161.0, 49.0, -186.0)
                else null

            for (p in KuudraPod.entries) {
                if (highlightPod && belly) ParallaxBox.frame(p.aabb, boxColor, depth = depthTest)

                if (!highlightSpecific) continue
                if (p != selected) continue

                val aabb =
                    if (offset != null) p.aabb0.move(offset.x, offset.y, offset.z)
                    else p.aabb0

                ParallaxBox.frame(aabb, boxColor, depth = false)
            }
        }
    }

    private fun fn0(): KuudraPod? = when (pod) {
        0 -> KuudraPod.Left
        1 -> KuudraPod.Middle
        2 -> KuudraPod.Right
        else -> null
    }

    private fun KBusCancellableTrait.ccl() {
        cancel()

        val now = System.currentTimeMillis()
        if (now - last < 500) return
        last = now

        "Blocked pickaxe ability!".mod()
    }

    private fun reset() {
        belly = false
        stunning = false
        last = 0
    }

    private fun fn() {
        // :3
    }
}
