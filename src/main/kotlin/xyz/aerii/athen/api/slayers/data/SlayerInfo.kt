package xyz.aerii.athen.api.slayers.data

import net.minecraft.world.entity.Entity
import xyz.aerii.athen.api.slayers.enums.tier.SlayerTier
import xyz.aerii.athen.api.slayers.enums.type.base.ISlayerType
import xyz.aerii.library.api.level
import xyz.aerii.library.api.name
import xyz.aerii.library.handlers.delegate.Expirable
import xyz.aerii.library.utils.stripped

data class SlayerInfo(val entity: Entity) {
    val owner by Expirable(::fn0, true)
    val type by Expirable(::fn1, true)
    val tier by Expirable(::fn2, true)

    val string: String
        get() = "${type}_T${tier?.int}"

    val owned: Boolean
        get() = owner == name

    private fun fn0(): String? {
        return level?.getEntity(entity.id + 3)?.customName?.stripped()?.substringAfterLast(":")?.trim()
    }

    private fun fn1(): ISlayerType? {
        val name = level?.getEntity(entity.id + 1)?.customName?.stripped() ?: return null
        return ISlayerType.Companion.Names.map.entries.find { (a, _) -> name.contains(a) }?.value
    }

    private fun fn2(): SlayerTier? {
        return SlayerTier.find(level?.getEntity(entity.id + 1)?.customName?.stripped() ?: return null)
    }

    override fun toString(): String {
        return "SlayerInfo(owner=$owner, isOwnedByPlayer=$owned, type=$type, tier=$tier, age=${entity.tickCount / 20}s)"
    }
}