package xyz.aerii.athen.api.kuudra.enums

import net.minecraft.world.entity.Entity
import xyz.aerii.athen.Athen
import xyz.aerii.library.api.level
import xyz.aerii.library.handlers.delegate.Expirable
import xyz.aerii.library.utils.stripped

class KuudraPlayer(
    val name: String
) {
    var deaths = 0
        internal set

    val entity by Expirable(::d) { !it.isAlive }

    init {
        Athen.LOGGER.debug("Created KuudraPlayer with entity: {}", entity)
    }

    private fun d(): Entity? =
        level?.players()?.find { it.uuid.version() == 4 && it.name.stripped() == name }

    override fun toString(): String =
        "KuudraPlayer(n=$name, d=$deaths, entity: ${entity != null})"
}