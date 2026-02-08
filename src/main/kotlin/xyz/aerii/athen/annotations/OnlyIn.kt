package xyz.aerii.athen.annotations

import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import xyz.aerii.athen.api.location.SkyBlockArea
import xyz.aerii.athen.api.location.SkyBlockIsland

/**
 * Marks a [xyz.aerii.athen.modules.Module] to only be enabled in a certain region.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnlyIn(
    val areas: Array<SkyBlockArea> = [],
    val islands: Array<SkyBlockIsland> = [],
    val floors: Array<DungeonFloor> = [],
    val skyblock: Boolean = false
)
