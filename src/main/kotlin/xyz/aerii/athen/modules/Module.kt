package xyz.aerii.athen.modules

import net.minecraft.network.protocol.Packet
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.annotations.Redstone
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.api.location.LocationAPI
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.config.ConfigBuilder
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.core.Event
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Initializer
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.React.Companion.and
import xyz.aerii.athen.utils.ALWAYS_TRUE
import xyz.aerii.athen.utils.toCamelCase
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

open class Module(
    name: String? = null,
    description: String? = null,
    category: Category? = null,
    default: Boolean = false
) {
    private val _config: ConfigBuilder? by lazy {
        ConfigBuilder(
            configKey ?: return@lazy null,
            name ?: return@lazy null,
            description ?: return@lazy null,
            category ?: return@lazy null,
            default
        ).also { it.module = this }
    }

    private val _location: React<Boolean> = run {
        val onlyIn = this::class.findAnnotation<OnlyIn>() ?: return@run ALWAYS_TRUE
        when {
            onlyIn.floors.isNotEmpty() -> DungeonAPI.floor.map { it in onlyIn.floors }
            onlyIn.areas.isNotEmpty() -> LocationAPI.area.map { it in onlyIn.areas }
            onlyIn.islands.isNotEmpty() -> LocationAPI.island.map { it in onlyIn.islands }
            onlyIn.skyblock -> LocationAPI.isOnSkyBlock
            else -> ALWAYS_TRUE
        }
    }

    val react: React<Boolean> by Initializer(::fn0) { if (redstone) ALWAYS_TRUE else config.state and _location }

    val configKey: String? =
        name?.toCamelCase()

    val redstone: Boolean =
        this::class.hasAnnotation<Redstone>()

    val config: ConfigBuilder
        get() = _config ?: error("Config not initialized")

    var enabled: Boolean = false
        private set

    protected inline fun <reified T : Event> on(
        priority: Int = 0,
        noinline handler: T.() -> Unit
    ) = xyz.aerii.athen.events.core.on<T>(priority, handler).runWhen(react)

    protected inline fun <reified E : PacketEvent, reified P : Packet<*>> on(
        priority: Int = 0,
        noinline handler: P.(E) -> Unit
    ) = xyz.aerii.athen.events.core.on<E, P>(priority, handler).runWhen(react)

    private fun fn0() {
        enabled = react.value
        react.onChange { enabled = it }
    }
}