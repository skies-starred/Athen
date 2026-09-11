@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import foo.starred.athen.Athen
import foo.starred.athen.annotations.Load
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.extractText
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.PlayerEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.command
import foo.starred.athen.utils.id
import foo.starred.athen.utils.lore
import foo.starred.athen.utils.uuid
import foo.starred.snowbird.api.*
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.stripped
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@Load
object ProtectItems : Module(
    "Protect items",
    "Protects any item!",
    Category.GENERAL
) {
    private val _unused by config.information("Use command <red>\"/${Athen.modId} protect [add|remove|list]\"<r> to manage items!")
    private val move by config.switch("Allowing moving items")

    private val render = config.switch("Render protected").unique("render")
    private val renderKey by config.switch("Only when key pressed", true)
    private val renderKeybind by config.keybind("Keybind", InputConstants.KEY_P)

    private val json = JsonStore("features/protectItems")
    private val uuids = json.mutableSet("uuid", Codec.STRING)
    private val types = json.mutableSet("type", Codec.STRING)
    private val types0 = json.mutableSet("type0", Codec.STRING)

    private val trade = Regex("^You\\s+\\w+$")
    private val p = "<bold><${Catppuccin.Mocha.Lavender.argb}>P".parse().visualOrderText

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
            if (renderKey && !renderKeybind.pressed) return@on

            graphics.extractText(p, slot.x, slot.y)
        }.runWhen(render.state)

        command {
            "protect" / "add" {
                val item = held?.takeIf { !it.isEmpty } ?: return@invoke "Not holding anything!".mod()
                val uuid = item.uuid()
                if (!enabled) "Please turn on the feature \"ProtectItems\"".mod()

                if (uuid != null) {
                    if (uuid in uuids.value) return@invoke "Item uuid already exists in list!".mod()
                    uuids.update { add(uuid) }

                    "Added item uuid to list!".mod()
                    return@invoke
                }

                val sid = item.id()
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
                val item = held?.takeIf { !it.isEmpty } ?: return@invoke "Not holding anything!".mod()
                val uuid = item.uuid()
                if (!enabled) "Please turn on the feature \"ProtectItems\"".mod()

                if (uuid != null) {
                    if (uuid !in uuids.value) return@invoke "Item uuid does not exist in list!".mod()
                    uuids.update { remove(uuid) }

                    "Removed item uuid from list!".mod()
                    return@invoke
                }

                val sid = item.id()
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
                val a = ("<gray>" + ("-".repeat())).parse()

                "Protected items list:".mod()
                a.lie()

                "Protected UUIDs:".mod()
                for (u in uuids.value) " <dark_gray>- <gray>$u".parse().lie()
                a.lie()

                "Protected SkyBlock IDs:".mod()
                for (t in types0.value) " <dark_gray>- <gray>$t".parse().lie()
                a.lie()

                "Protected IDs:".mod()
                for (t in types.value) " <dark_gray>- <gray>$t".parse().lie()
                a.lie()

                if (!enabled) "Please turn on the feature \"ProtectItems\"".mod()
            }
        }
    }

    private fun ItemStack.fn(): Boolean {
        val uuid = uuid()
        if (uuid != null) return uuid in uuids.value

        val sid = id()
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
        if (t0?.lore()?.lastOrNull()?.stripped() == "Click to buyback!") return true

        return false
    }
}
