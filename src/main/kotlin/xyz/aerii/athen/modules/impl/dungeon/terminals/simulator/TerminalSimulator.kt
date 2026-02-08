@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.dungeon.terminals.simulator

import com.mojang.brigadier.arguments.IntegerArgumentType
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.core.override
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.base.SimulatorMenu
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.impl.*

@Load
object TerminalSimulator : Module(
    "Terminal simulator",
    "Simulator terminal, terminal simulators?",
    Category.DUNGEONS
) {
    private val ipInput by config.textInput("Simulator server IP", "hypixelp3sim.zapto.org")
    private val _unused0 by config.textParagraph("The simulator server IP is optional. You can still do <red>\"/${Athen.modId} simulate terminals\"<r> to simulate.")
    private val pingInput = config.textInput("Ping", "0", "0").custom("ping")

    var ping = 0
    val s = React(false)
    val s0 = React(false)

    init {
        run {
            ping = pingInput.value.toIntOrNull() ?: return@run
        }

        pingInput.state.onChange {
            ping = it.toIntOrNull() ?: return@onChange
        }

        react.onChange {
            SimulatorMenu.a()
            if (it) {
                "Run \"/${Athen.modId} simulate terminals ping <ping>\" to change ping!".modMessage()
                if (configKey != null) ConfigManager.updateConfig(configKey, false)
            }
        }

        on<LocationEvent.ServerConnect> {
            s0.value = client.currentServer?.ip == ipInput
        }.override()

        on<LocationEvent.ServerDisconnect> {
            s0.value = false
        }.override()

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("simulate") {
                    then("terminals") {
                        callback {
                            SimulatorMenu.a()
                        }

                        then("ping") {
                            thenCallback("int", IntegerArgumentType.integer()) {
                                ConfigManager.updateConfig("$configKey.ping", IntegerArgumentType.getInteger(this, "int").toString())
                                "Ping set to ${ping}ms".modMessage()
                            }
                        }

                        thenCallback("rubix") {
                            RubixSimulator().a()
                        }

                        thenCallback("color") {
                            ColorSimulator().a()
                        }

                        thenCallback("melody") {
                            MelodySimulator().a()
                        }

                        thenCallback("name") {
                            NameSimulator().a()
                        }

                        thenCallback("panes") {
                            PanesSimulator().a()
                        }

                        thenCallback("numbers") {
                            NumbersSimulator().a()
                        }
                    }
                }
            }
        }
    }
}