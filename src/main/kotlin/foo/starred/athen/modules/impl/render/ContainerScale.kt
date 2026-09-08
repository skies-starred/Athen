package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.client
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

@Load
object ContainerScale : Module(
    "Container scale",
    "Scales containers visually",
    Category.RENDER
) {
    private val inventory by config.switch("Inventory only")
    val scale by config.slider("Scale", 1f, 0.5f, 5f, double = true)

    @JvmStatic
    val x0: Float
        //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
        get() = (client.screen?.width ?: 0) / 2f

    @JvmStatic
    val y0: Float
        //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
        get() = (client.screen?.height ?: 0) / 2f

    @JvmStatic
    val bool: Boolean
        get() {
            if (!enabled) return false
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            val screen = client.screen ?: return false
            return if (inventory) screen is InventoryScreen else screen is AbstractContainerScreen<*>
        }

    init {
        on<GuiEvent.Render.Screen.Pre>(Int.MIN_VALUE) {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on
            if (!bool) return@on

            graphics.fillGradient(0, 0, screen.width, screen.height, -1072689136, -804253680)
            graphics.pose().pushMatrix()
            graphics.pose().translate(x0, y0)
            graphics.pose().scale(scale)
            graphics.pose().translate(-x0, -y0)
        }

        on<GuiEvent.Render.Screen.Post>(Int.MIN_VALUE) {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (!bool) return@on

            graphics.pose().popMatrix()
        }
    }
}
