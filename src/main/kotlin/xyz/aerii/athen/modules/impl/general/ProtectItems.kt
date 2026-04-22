@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import com.mojang.serialization.Codec
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.bold
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.PlayerEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.client
import xyz.aerii.library.api.held
import xyz.aerii.library.api.lie
import xyz.aerii.library.api.pressed
import xyz.aerii.library.api.repeat
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.stripped

@Load
object ProtectItems : Module(
    "Protect items",
    "Protects any item!",
    Category.GENERAL
) {
    private val _unused by config.textParagraph("Use command <red>\"/${Athen.modId} protect [add|remove|list]\"<r> to manage items!")
    private val move by config.switch("Allowing moving items")

    private val render = config.switch("Render protected").custom("render")
    private val renderKey by config.switch("Only when key pressed", true).dependsOn { render.value }
    private val renderKeybind by config.keybind("Keybind", GLFW.GLFW_KEY_P).dependsOn { render.value }

    private val scribble = Scribble("features/protectItems")
    private val uuids = scribble.mutableSet("uuid", Codec.STRING)
    private val types = scribble.mutableSet("type", Codec.STRING)
    private val types0 = scribble.mutableSet("type0", Codec.STRING)

    private val trade = Regex("^You\\s+\\w+$")
    private val p = "<${Catppuccin.Mocha.Mauve.argb}>P".parse().apply { bold = true }.visualOrderText

    init {
        on<PlayerEvent.Drop> {
            if (item?.fn() != true) return@on
            if (!gui && DungeonAPI.floorStarted) return@on

            "Prevented dropping item! <gray>[ProtectItems]".parse().modMessage()
            cancel()
        }

        on<GuiEvent.Slots.Click> {
            if (slot?.item?.fn() != true) return@on
            if (move && clickType != ClickType.THROW && !fn0()) return@on

            "Prevented clicking item! <gray>[ProtectItems]".parse().modMessage()
            cancel()
        }

        on<GuiEvent.Slots.Render.Update> {
            if (!slot.item.fn()) return@on

            renders.add { graphics, slot ->
                if (!renderKey || (renderKeybind.bound && renderKeybind.pressed)) graphics.text(p, slot.x, slot.y)
            }
        }.runWhen(render.state)

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("protect") {
                    thenCallback("add") {
                        val item = held?.takeIf { !it.isEmpty } ?: return@thenCallback "Not holding anything!".modMessage()
                        val uuid = item.getData(DataTypes.UUID)?.toString()
                        if (!enabled) "Please turn on the feature \"ProtectItems\"".modMessage()

                        if (uuid != null) {
                            if (uuid in uuids.value) return@thenCallback "Item uuid already exists in list!".modMessage()
                            uuids.update { add(uuid) }

                            "Added item uuid to list!".modMessage()
                            return@thenCallback
                        }

                        val sid = item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId
                        if (sid != null) {
                            if (sid in types0.value) return@thenCallback "Item skyblock id already exists in list!".modMessage()
                            types0.update { add(sid) }

                            "Added item skyblock id to list!".modMessage()
                            return@thenCallback
                        }

                        val id = BuiltInRegistries.ITEM.getKey(item.item).toString()

                        if (id in types.value) return@thenCallback "Item id already exists in list!".modMessage()
                        types.update { add(id) }

                        "Added item id to list!".modMessage()
                    }

                    thenCallback("remove") {
                        val item = held?.takeIf { !it.isEmpty } ?: return@thenCallback "Not holding anything!".modMessage()
                        val uuid = item.getData(DataTypes.UUID)?.toString()
                        if (!enabled) "Please turn on the feature \"ProtectItems\"".modMessage()

                        if (uuid != null) {
                            if (uuid !in uuids.value) return@thenCallback "Item uuid does not exist in list!".modMessage()
                            uuids.update { remove(uuid) }

                            "Removed item uuid from list!".modMessage()
                            return@thenCallback
                        }

                        val sid = item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId
                        if (sid != null) {
                            if (sid !in types0.value) return@thenCallback "Item skyblock id does not exist in list!".modMessage()
                            types0.update { remove(sid) }

                            "Removed item skyblock id from list!".modMessage()
                            return@thenCallback
                        }

                        val id = BuiltInRegistries.ITEM.getKey(item.item).toString()

                        if (id !in types.value) return@thenCallback "Item id does not exist in list!".modMessage()
                        types.update { remove(id) }

                        "Removed item id from list!".modMessage()
                    }

                    thenCallback("list") {
                        val a = ("<gray>" + ("-".repeat())).parse()

                        "Protected items list:".modMessage()
                        a.lie()

                        "Protected UUIDs:".modMessage()
                        for (u in uuids.value) " <dark_gray>- <gray>$u".parse().lie()
                        a.lie()

                        "Protected SkyBlock IDs:".modMessage()
                        for (t in types0.value) " <dark_gray>- <gray>$t".parse().lie()
                        a.lie()

                        "Protected IDs:".modMessage()
                        for (t in types.value) " <dark_gray>- <gray>$t".parse().lie()
                        a.lie()

                        if (!enabled) "Please turn on the feature \"ProtectItems\"".modMessage()
                    }
                }
            }
        }
    }

    private fun ItemStack.fn(): Boolean {
        val uuid = getData(DataTypes.UUID)?.toString()
        if (uuid != null) return uuid in uuids.value

        val sid = getData(DataTypes.SKYBLOCK_ID)?.skyblockId
        if (sid != null) return sid in types0.value

        val id = BuiltInRegistries.ITEM.getKey(item).toString()
        return id in types.value
    }

    private fun fn0(): Boolean {
        val s = client.screen as? AbstractContainerScreen<*> ?: return true
        val t = s.title.string

        if (t == "Salvage Items") return true
        if (t == "Create Auction") return true
        if (t == "Create BIN Auction") return true
        if (trade.matches(t)) return true

        val t0 = s.menu.slots.getOrNull(49)?.item
        if (t0?.item == Items.HOPPER && t0.hoverName?.stripped() == "Sell Item") return true
        if (t0?.getLore()?.lastOrNull()?.stripped() == "Click to buyback!") return true

        return false
    }
}