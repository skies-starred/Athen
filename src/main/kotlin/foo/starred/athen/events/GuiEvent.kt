package foo.starred.athen.events

import foo.starred.athen.events.core.CancellableEvent
import foo.starred.athen.events.core.Event
import foo.starred.snowbird.utils.stripped
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

sealed class GuiEvent {
    sealed class Render {
        sealed class Any {
            data class Pre(
                val graphics: GuiGraphicsExtractor
            ) : Event()

            data class Main(
                val graphics: GuiGraphicsExtractor
            ) : Event()

            data class Post(
                val graphics: GuiGraphicsExtractor
            ) : Event()
        }

        sealed class Screen {
            data class Pre(
                val graphics: GuiGraphicsExtractor
            ) : CancellableEvent()

            data class Post(
                val graphics: GuiGraphicsExtractor
            ) : Event()
        }
    }

    sealed class Open {
        data class Container(
            val screen: AbstractContainerScreen<*>
        ) : Event() {
            val stripped = screen.title.stripped()
        }

        data class Any(
            val screen: net.minecraft.client.gui.screens.Screen
        ) : Event() {
            val stripped = screen.title.stripped()
        }
    }

    sealed class Close {
        data class Container(
            val screen: AbstractContainerScreen<*>
        ) : Event() {
            val stripped = screen.title.stripped()
        }

        data class Any(
            val screen: net.minecraft.client.gui.screens.Screen
        ) : Event() {
            val stripped = screen.title.stripped()
        }
    }

    sealed class Slots {
        sealed class Render {
            sealed class Any {
                data class Pre(
                    val graphics: GuiGraphicsExtractor,
                    val slot: Slot
                ) : CancellableEvent()

                data class Post(
                    val graphics: GuiGraphicsExtractor,
                    val slot: Slot
                ) : Event()
            }

            sealed class Menu {
                data class Start(
                    val graphics: GuiGraphicsExtractor
                ) : CancellableEvent()

                data class End(
                    val graphics: GuiGraphicsExtractor
                ) : Event()
            }

            sealed class Hotbar {
                data class Pre(
                    val graphics: GuiGraphicsExtractor,
                    val item: ItemStack,
                    val x: Int,
                    val y: Int
                ) : CancellableEvent()

                data class Post(
                    val graphics: GuiGraphicsExtractor,
                    val item: ItemStack,
                    val x: Int,
                    val y: Int
                ) : Event()
            }
        }

        sealed class Input {
            data class Click(
                val slot: Slot?,
                val slotId: Int,
                val mouseButton: Int,
                val clickType: ContainerInput
            ) : CancellableEvent()

            data class Hover(
                val slot: Slot
            ) : Event()

            data class Unhover(
                val slot: Slot
            ) : Event()
        }
    }

    sealed class Items {
        sealed class Render {
            data class Pre(
                val graphics: GuiGraphicsExtractor,
                val item: ItemStack,
                val x: Int,
                val y: Int
            ) : Event()

            data class Post(
                val graphics: GuiGraphicsExtractor,
                val item: ItemStack,
                val x: Int,
                val y: Int
            ) : Event()
        }
    }

    sealed class Tooltip {
        data class Render(
            val item: ItemStack,
            val tooltip: MutableList<Component>
        ) : Event()

        data class Update(
            val item: ItemStack,
            val tooltip: MutableList<Component>
        ) : Event()
    }

    sealed class Input {
        sealed class Key {
            data class Press(
                val keyEvent: KeyEvent
            ) : CancellableEvent()

            data class Release(
                val keyEvent: KeyEvent
            ) : Event()
        }

        sealed class Mouse {
            data class Press(
                val keyEvent: MouseButtonEvent
            ) : CancellableEvent()

            data class Release(
                val keyEvent: MouseButtonEvent
            ) : Event()

            data class Scroll(
                val amount: Double
            ) : CancellableEvent()
        }
    }
}
