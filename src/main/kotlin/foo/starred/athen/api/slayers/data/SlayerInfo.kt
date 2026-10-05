package foo.starred.athen.api.slayers.data

import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.base.ISlayerType
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.lazy.RefreshableLazy
import foo.starred.snowbird.utils.stripped
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand

data class SlayerInfo(val entity: Entity) {
    private val _type by RefreshableLazy(::fn1, true)
    private val _tier by RefreshableLazy(::fn2, true)

    val serializable: String
        get() = "${type}_T${tier?.int}"

    val owned: Boolean
        get() = owner == client.user.name

    var type: ISlayerType? = null
        get() = field ?: _type

    var tier: SlayerTier? = null
        get() = field ?: _tier

    var phase: Int = 1

    var owner: String? = null

    override fun toString(): String {
        return "SlayerInfo(owned=$owned, type=$type, tier=$tier, age=${entity.tickCount / 20}s)"
    }

    private fun fn1(): ISlayerType? {
        val name = name() ?: return null
        return ISlayerType.Companion.Names.map.entries.find { (a, _) -> name.contains(a) }?.value
    }

    private fun fn2(): SlayerTier? {
        val name = name() ?: return null
        return SlayerTier.find(name)
    }

    private fun name(): String? {
        return (client.level?.getEntity(entity.id + 1) as? ArmorStand)?.customName?.stripped()
    }
}
