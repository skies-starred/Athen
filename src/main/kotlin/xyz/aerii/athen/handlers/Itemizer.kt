@file:Suppress("FunctionName", "ObjectPrivatePropertyName", "Unchecked_Cast", "Unused")

package xyz.aerii.athen.handlers

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.extentions.getHoveredSlot
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.ConfigBuilder
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Smoothie.client
import java.util.WeakHashMap

@Load
object Itemizer {
    private val pressed = mutableSetOf<Int>()

    private val `watched$tooltip` = mutableListOf<() -> Int>()
    private val `cache$tooltip` = WeakHashMap<ItemStack, Pair<List<*>, List<*>>>()

    private val `watched$slot` = mutableListOf<() -> Int>()
    private val `cache$slot` = WeakHashMap<Slot, Pair<ItemStack, List<(GuiGraphics, Slot) -> Unit>?>>()

    fun Slot.`invalidate$render`() {
        `cache$slot`.remove(this)
    }

    fun ItemStack.`invalidate$tooltip`() {
        `cache$tooltip`.remove(this)
    }

    init {
        on<GuiEvent.Container.Close> {
            `cache$slot`.clear()
            `cache$tooltip`.clear()
        }

        on<GuiEvent.Slots.Hover> {
            slot.item?.takeIf { !it.isEmpty }?.`invalidate$tooltip`()
        }

        on<GuiEvent.Input.Key.Press> {
            if (!pressed.add(keyEvent.key)) return@on
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on

            if (`watched$tooltip`.any { it() == keyEvent.key }) screen.getHoveredSlot()?.item?.`invalidate$tooltip`()
            if (`watched$slot`.any { it() == keyEvent.key }) screen.menu?.slots?.forEach { it.`invalidate$render`() }
        }

        on<GuiEvent.Input.Key.Release> {
            if (!pressed.remove(keyEvent.key)) return@on
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on

            if (`watched$tooltip`.any { it() == keyEvent.key }) screen.getHoveredSlot()?.item?.`invalidate$tooltip`()
            if (`watched$slot`.any { it() == keyEvent.key }) screen.menu?.slots?.forEach { it.`invalidate$render`() }
        }

        on<GuiEvent.Tooltip.Render> {
            val current = tooltip.toList()
            val cached = `cache$tooltip`[item]

            if (cached != null && cached.first == current) {
                tooltip.clear()
                tooltip.addAll(cached.second as List<Nothing>)
                return@on
            }

            GuiEvent.Tooltip.Update(item, tooltip).post()
            `cache$tooltip`[item] = current to tooltip.toList()
        }

        on<GuiEvent.Slots.Render.Pre> {
            val cached = `cache$slot`[slot] ?: return@on
            if (cached.first != slot.item) return@on
            if (cached.second != null) return@on
            cancel()
        }

        on<GuiEvent.Slots.Render.Post> {
            val cached = `cache$slot`[slot]

            if (cached != null && cached.first == slot.item) {
                cached.second?.forEach { it(graphics, slot) }
                return@on
            }

            val renders = mutableListOf<(GuiGraphics, Slot) -> Unit>()
            val event = GuiEvent.Slots.Render.Update(graphics, slot, renders).post()

            if (event) {
                `cache$slot`[slot] = slot.item to null
                return@on
            }

            `cache$slot`[slot] = slot.item to renders.toList()
            for (r in renders) r(graphics, slot)
        }
    }

    fun ConfigBuilder.OptionBuilder<Int>.`watch$tooltip`(): ConfigBuilder.OptionBuilder<Int> = apply {
        resolve { `watched$tooltip`.add { it.value } }
    }

    fun ConfigBuilder.OptionBuilder<Int>.`watch$slot`(): ConfigBuilder.OptionBuilder<Int> = apply {
        resolve { `watched$slot`.add { it.value } }
    }
}