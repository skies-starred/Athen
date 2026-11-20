@file:Suppress("ObjectPrivatePropertyName", "LocalVariableName")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.skyblock.entity.duck.EntityDuck.Companion.parent
import foo.starred.athen.api.slayers.SlayerAPI
import foo.starred.athen.api.slayers.data.SlayerInfo
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.api.slayers.enums.type.impl.SlayerMini
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand

@AthenModuleScope(skyblock = true)
@LoadableAthenModule
object SlayerNametagHider : Module(
    "Slayer nametag hider",
    "Hides slayer nametags",
    ConfigCategory.SLAYER
) {
    private val boss by config.group("Boss nametags", false)
    private val `boss$nametag` by boss.switch("Hide boss name", true)
    private val `boss$spawner` by boss.switch("Hide spawner", true)
    private val `boss$timer` by boss.switch("Hide timer", true)

    private val demon by config.group("Demon nametags")
    private val `demon$nametag` by demon.switch("Hide demon name")
    private val `demon$spawner` by demon.switch("Hide spawner")
    private val `demon$timer` by demon.switch("Hide timer")

    init {
        on<WorldRenderEvent.Entity> {
            val entity = entity as? ArmorStand ?: return@on
            if (!entity.isCustomNameVisible) return@on
            if (entity.customName == null) return@on

            val parent = entity.parent ?: return@on
            val slayer = SlayerAPI.bosses[parent]?.takeIf { it.type !is SlayerMini } ?: return@on

            hide(parent.id, entity, slayer)
        }
    }

    private fun hide(id: Int, entity: LivingEntity, slayer: SlayerInfo) {
        val boss = slayer.type is SlayerBoss
        val nametag = if (boss) `boss$nametag` else `demon$nametag`
        val timer = if (boss) `boss$timer` else `demon$timer`
        val spawner = if (boss) `boss$spawner` else `demon$spawner`
        if (!boolean(id, entity.id, nametag, timer, spawner)) return

        entity.isCustomNameVisible = false
    }

    private fun boolean(id1: Int, id2: Int, nametag: Boolean, timer: Boolean, spawner: Boolean): Boolean {
        return nametag && id1 == id2 - 1 || timer && id1 == id2 - 2 || spawner && id1 == id2 - 3
    }
}
