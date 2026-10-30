@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.PlayerEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import foo.starred.kontext.api.item.resolver.impl.LoreKontextItemDataResolver
import foo.starred.kontext.api.storage.impl.generic.extension.mutableSet
import foo.starred.kontext.api.text.parser.impl.parse
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@LoadableAthenModule
object ProtectItems : Module(
    "Protect items",
    "Protects any item!",
    ConfigCategory.GENERAL
) {
    private val _unused by config.information("Command: <red>/athen protect [add|remove|list]")
    private val move by config.switch("Allowing moving items")

    private val render = config.switch("Render protected").unique("render")
    private val renderKey by config.switch("Only when key pressed", true)
    private val renderKeybind by config.keybind("Keybind", InputConstants.KEY_P)

    private val json = JsonStore("features/protectItems")
    private val uuids = json.mutableSet("uuid", Codec.STRING)
    private val types = json.mutableSet("type", Codec.STRING)
    private val types0 = json.mutableSet("type0", Codec.STRING)

    private val trade = Regex("^You\\s+\\w+$")
    private val p = "<bold><${MochaColorScheme.Lavender.argb}>P".parse().visualOrderText

    init {
        on<PlayerEvent.Drop> {
            if (item?.fn() != true) return@on
            if (!gui && DungeonAPI.started) return@on

            "Prevented dropping item! <gray>[ProtectItems]".mod()
            cancel()
        }

        on<GuiEvent.Slots.Input.Click> {
            if (slot?.item?.fn() != true) return@on
            if (move && clickType != ContainerInput.THROW && !fn0()) return@on

            "Prevented clicking item! <gray>[ProtectItems]".mod()
            cancel()
        }

        on<GuiEvent.Slots.Render.Any.Post> {
            if (!slot.item.fn()) return@on
            if (renderKey && !GenericInputState.pressed(renderKeybind)) return@on

            MinecraftFontRenderer.extract(graphics, p, slot.x, slot.y)
        }.runWhen(render.state)

        command {
            "protect" / "add" {
                val item = client.player?.mainHandItem?.takeIf { !it.isEmpty } ?: return@invoke "Not holding anything!".mod()
                val uuid = item.get<GenericKontextItemDataResolver.UUID, String>()
                if (!enabled) "Please turn on the feature \"ProtectItems\"".mod()

                if (uuid != null) {
                    if (uuid in uuids.value) return@invoke "Item uuid already exists in list!".mod()
                    uuids.update { add(uuid) }

                    "Added item uuid to list!".mod()
                    return@invoke
                }

                val sid = item.get<GenericKontextItemDataResolver.ID, String>()
                if (sid != null) {
                    if (sid in types0.value) return@invoke "Item skyblock id already exists in list!".mod()
                    types0.update { add(sid) }

                    "Added item skyblock id to list!".mod()
                    return@invoke
                }

                val id = BuiltInRegistries.ITEM.getKey(item.item).toString()

                if (id in types.value) return@invoke "Item id already exists in list!".mod()
                types.update { add(id) }

                "Added item id to list!".mod()
            }

            "protect" / "remove" {
                val item = client.player?.mainHandItem?.takeIf { !it.isEmpty } ?: return@invoke "Not holding anything!".mod()
                val uuid = item.get<GenericKontextItemDataResolver.UUID, String>()
                if (!enabled) "Please turn on the feature \"ProtectItems\"".mod()

                if (uuid != null) {
                    if (uuid !in uuids.value) return@invoke "Item uuid does not exist in list!".mod()
                    uuids.update { remove(uuid) }

                    "Removed item uuid from list!".mod()
                    return@invoke
                }

                val sid = item.get<GenericKontextItemDataResolver.ID, String>()
                if (sid != null) {
                    if (sid !in types0.value) return@invoke "Item skyblock id does not exist in list!".mod()
                    types0.update { remove(sid) }

                    "Removed item skyblock id from list!".mod()
                    return@invoke
                }

                val id = BuiltInRegistries.ITEM.getKey(item.item).toString()

                if (id !in types.value) return@invoke "Item id does not exist in list!".mod()
                types.update { remove(id) }

                "Removed item id from list!".mod()
            }

            "protect" / "list" {
                val a = ("<gray>" + MinecraftChat.repeat("-")).parse()

                "Protected items list:".mod()
                MinecraftMessages.display(a)

                "Protected UUIDs:".mod()
                for (u in uuids.value) MinecraftMessages.display(" <dark_gray>- <gray>$u")
                MinecraftMessages.display(a)

                "Protected SkyBlock IDs:".mod()
                for (t in types0.value) MinecraftMessages.display(" <dark_gray>- <gray>$t")
                MinecraftMessages.display(a)

                "Protected IDs:".mod()
                for (t in types.value) MinecraftMessages.display(" <dark_gray>- <gray>$t")
                MinecraftMessages.display(a)

                if (!enabled) "Please turn on the feature \"ProtectItems\"".mod()
            }
        }
    }

    private fun ItemStack.fn(): Boolean {
        val uuid = get<GenericKontextItemDataResolver.UUID, String>()
        if (uuid != null) return uuid in uuids.value

        val sid = get<GenericKontextItemDataResolver.ID, String>()
        if (sid != null) return sid in types0.value

        val id = BuiltInRegistries.ITEM.getKey(item).toString()
        return id in types.value
    }

    private fun fn0(): Boolean {
        //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
        val s = client.screen as? AbstractContainerScreen<*> ?: return true
        val t = s.title.string

        if (t == "Salvage Items") return true
        if (t == "Create Auction") return true
        if (t == "Create BIN Auction") return true
        if (trade.matches(t)) return true

        val t0 = s.menu.slots.getOrNull(49)?.item
        if (t0?.item == Items.HOPPER && t0.hoverName.stripped() == "Sell Item") return true
        if (t0?.get<LoreKontextItemDataResolver, List<Component>>()?.lastOrNull()?.stripped() == "Click to buyback!") return true

        return false
    }
}
