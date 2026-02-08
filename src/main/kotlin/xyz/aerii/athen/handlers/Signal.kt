package xyz.aerii.athen.handlers

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.fabric.event.HypixelModAPICallback
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.entity.ComponentAttachEvent
import tech.thatgravyboat.skyblockapi.api.events.entity.NameChangedEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardTitleUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.TabListChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.minecraft.sounds.SoundPlayedEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ItemTooltipEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.PlayerHotbarChangeEvent
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.*
import xyz.aerii.athen.events.core.onReceive
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import kotlin.jvm.optionals.getOrNull

/**
 * Handles event conversion and posts events that are not handled by mixins.
 */
@Priority
object Signal {
    init {
        SkyBlockAPI.eventBus.register(Signal)

        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket::class.java)

        HypixelModAPICallback.EVENT.register { event ->
            if (event is ClientboundLocationPacket) {
                LocationEvent.ServerChange(
                    event.serverName,
                    event.serverType.getOrNull(),
                    event.lobbyName.getOrNull(),
                    event.mode.getOrNull(),
                    event.map.getOrNull(),
                ).post()
            }
        }

        SpecialGuiElementRegistry.register { graphics ->
            NVGSpecialRenderer(graphics.vertexConsumers())
        }

        ClientLifecycleEvents.CLIENT_STARTED.register { _ ->
            GameEvent.Start.post()
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            GameEvent.Stop.post()
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            LocationEvent.ServerConnect.post()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            LocationEvent.ServerDisconnect.post()
        }

        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            EntityEvent.Load(entity).post()
        }

        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            EntityEvent.Unload(entity).post()
        }

        WorldRenderEvents.END_EXTRACTION.register { _ ->
            WorldRenderEvent.Extract.post()
        }

        WorldRenderEvents.END_MAIN.register { context ->
            WorldRenderEvent.Render(context.matrices(), context.consumers() as? MultiBufferSource.BufferSource ?: return@register).post()
        }

        onReceive<ClientboundSystemChatPacket> {
            client.execute { if (ChatEvent(content, overlay).post()) it.cancel() }
        }
    }

    @Subscription(tech.thatgravyboat.skyblockapi.api.events.time.TickEvent::class)
    fun onTick() = TickEvent.Client.post()

    @Subscription
    fun onTabListChange(event: TabListChangeEvent) = TabListEvent.Change(event.old, event.new).post()

    @Subscription
    fun onScoreboardTitleUpdate(event: ScoreboardTitleUpdateEvent) = ScoreboardEvent.UpdateTitle(event.old, event.new).post()

    @Subscription
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) = ScoreboardEvent.Update(event.old, event.new, event.components).post()

    @Subscription
    fun onPlayerHotbarUpdate(event: PlayerHotbarChangeEvent) = PlayerEvent.HotbarChange(event.slot, event.item).post()

    @Subscription
    fun onCommand(event: RegisterCommandsEvent) = CommandRegistration(event).post()

    @Subscription(receiveCancelled = true)
    fun onComponentAttach(event: ComponentAttachEvent) = EntityEvent.ComponentAttach(event.component, event.infoLineEntity).post()

    @Subscription(receiveCancelled = true)
    fun onNameChanged(event: NameChangedEvent) = EntityEvent.NameChange(event.component, event.infoLineEntity).post()

    @Subscription
    fun onTooltipRender(event: ItemTooltipEvent) = GuiEvent.Tooltip.Render(event.item, event.tooltip).post()

    @Subscription
    fun onSoundPlay(event: SoundPlayedEvent) {
        if (SoundPlayEvent(event.sound, event.pos, event.volume, event.pitch).post()) event.cancel()
    }
}
