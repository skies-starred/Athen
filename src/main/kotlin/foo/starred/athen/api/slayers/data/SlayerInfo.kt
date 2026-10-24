package foo.starred.athen.api.slayers.data

import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.base.ISlayerType
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.lazy.RefreshableLazy
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand

data class SlayerInfo(val entity: Entity) {
    private val _owner by RefreshableLazy(::fn0, true)
    private val _type by RefreshableLazy(::fn1, true)
    private val _tier by RefreshableLazy(::fn2, true)

    val serializable: String
        get() = "${type}_T${tier?.int}"

    val owned: Boolean
        get() = owner == client.user.name

    var owner: String? = null
        get() {
            if (field != null) return field

            field = _owner
            return field
        }

    var type: ISlayerType? = null
        get() {
            if (field != null) return field

            field = _type
            return field
        }

    var tier: SlayerTier? = null
        get() {
            if (field != null) return field

            field = _tier
            return field
        }

    var phase: Int = 1

    override fun toString(): String {
        return "SlayerInfo(owned=$owned, type=$type, tier=$tier, age=${entity.tickCount / 20}s)"
    }

    private fun fn0(): String? {
        val stand = client.level?.getEntity(entity.id + 3) as? ArmorStand ?: return null
        return stand.customName?.stripped()?.takeIf { it.contains("'s SLAYER BOSS") }?.substringBefore("'")?.trim()
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
