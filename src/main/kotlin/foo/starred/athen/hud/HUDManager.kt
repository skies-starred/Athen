@file:Suppress("AssignedValueIsNeverRead", "VariableNeverRead")

package foo.starred.athen.hud

import foo.starred.athen.annotations.Priority
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.core.on
import foo.starred.athen.modules.impl.ModSettings
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.scheduling.scheduler.extensions.clientTicks

@Priority(-2)
object HUDManager {
    private val storage = JsonStore("config/HUDEditor")
    val elements = mutableMapOf<String, HUDElement>()
    val resolution = CascadeGeometricResolution.FHD.of(2f)

    var scale = 1f
        private set

    var width = 960f
        private set

    var height = 540f
        private set

    init {
        on<GuiEvent.Render.Any.Main> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen is HUDEditor) return@on
            //~ if >= 26.2 'client.options.hideGui' -> 'client.gui.hud.isHidden'
            if (client.options.hideGui && ModSettings.hideGuis) return@on

            graphics.pose().pushMatrix()
            graphics.pose().scale(scale)

            for (element in elements.values) {
                if (!element.render0) continue

                graphics.pose().pushMatrix()
                graphics.pose().translate(element.x, element.y)
                graphics.pose().scale(element.scale, element.scale)

                graphics.pose().pushMatrix()
                element.render(graphics, false)
                graphics.pose().popMatrix()

                graphics.pose().popMatrix()
            }

            graphics.pose().popMatrix()
        }
    }

    fun compute() {
        CascadeGeometricResolution.compute(resolution, client.window.guiScaledWidth, client.window.guiScaledHeight) { scale1, width1, height1 ->
            scale = scale1
            width = width1
            height = height1
        }
    }

    fun register(element: HUDElement) {
        elements[element.id] = element
        Scheduler.schedule(1.clientTicks) { get(element.id) }
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
