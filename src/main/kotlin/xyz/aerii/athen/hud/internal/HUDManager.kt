@file:Suppress("AssignedValueIsNeverRead", "VariableNeverRead")

package xyz.aerii.athen.hud.internal

import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.platform.translate
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Smoothie.client

@Priority(-2)
object HUDManager {
    private val storage = Scribble("config/HUD")
    val elements = mutableMapOf<String, HUDElement>()

    init {
        on<GuiEvent.Render.Pre> {
            if (client.screen is HUDEditor) return@on

            for (element in elements.values) {
                if (!element.renderNormal || !element.renderOutsidePreview) continue

                graphics.pushPop {
                    graphics.translate(element.scaledX, element.scaledY)
                    graphics.scale(element.scaledScale, element.scaledScale)
                    graphics.pushPop { element.render(graphics, false) }
                }
            }
        }
    }

    fun register(element: HUDElement) {
        elements[element.id] = element
        Chronos.Tick run { loadLayout(element.id) }
    }

    fun saveLayouts() {
        for ((id, element) in elements) {
            var x by storage.float("$id.x", 20f)
            var y by storage.float("$id.y", 20f)
            var scale by storage.float("$id.scale", 1f)

            x = element.x
            y = element.y
            scale = element.scale
        }

        storage.save()
    }

    fun loadLayout(id: String) {
        val element = elements[id] ?: return

        val x by storage.float("$id.x", 20f)
        val y by storage.float("$id.y", 20f)
        val scale by storage.float("$id.scale", 1f)

        element.x = x
        element.y = y
        element.scale = scale
    }
}
