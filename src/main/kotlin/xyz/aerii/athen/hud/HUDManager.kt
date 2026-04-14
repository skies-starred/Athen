@file:Suppress("AssignedValueIsNeverRead", "VariableNeverRead")

package xyz.aerii.athen.hud

import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.platform.translate
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.modules.impl.ModSettings
import xyz.aerii.library.api.client
import xyz.aerii.library.handlers.time.client

@Priority(-2)
object HUDManager {
    private val storage = Scribble("config/HUDEditor")
    val elements = mutableMapOf<String, HUDElement>()

    init {
        on<GuiEvent.Render.Main> {
            if (client.screen is HUDEditor) return@on
            if (client.options.hideGui && ModSettings.hideGuis) return@on

            Resolute.push(graphics)

            for (element in elements.values) {
                if (!element.render0) continue

                graphics.pushPop {
                    graphics.translate(element.x, element.y)
                    graphics.scale(element.scale, element.scale)
                    graphics.pushPop { element.render(graphics, false) }
                }
            }

            Resolute.pop(graphics)
        }
    }

    fun register(element: HUDElement) {
        elements[element.id] = element
        Chronos.schedule(1.client){ get(element.id) }
    }

    fun set() {
        for ((id, element) in elements) {
            var x by storage.float("$id.x", element.defaultX)
            var y by storage.float("$id.y", element.defaultY)
            var scale by storage.float("$id.scale", element.defaultScale)

            x = element.x
            y = element.y
            scale = element.scale
        }

        storage.save()
    }

    fun get(id: String) {
        val element = elements[id] ?: return

        val x by storage.float("$id.x", element.defaultX)
        val y by storage.float("$id.y", element.defaultY)
        val scale by storage.float("$id.scale", element.defaultScale)

        element.x = x
        element.y = y
        element.scale = scale
    }
}
