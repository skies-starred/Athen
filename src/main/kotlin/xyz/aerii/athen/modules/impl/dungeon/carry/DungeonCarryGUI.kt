package xyz.aerii.athen.modules.impl.dungeon.carry

import xyz.aerii.athen.modules.common.carry.ICarryGUI

object DungeonCarryGUI : ICarryGUI<DungeonCarryStateTracker.TrackedCarry>("Dungeon Carry Tracker") {
    override fun carries() = DungeonCarryStateTracker.tracked
    override fun persist() = DungeonCarryStateTracker.persist()
    override fun remove(player: String) = DungeonCarryStateTracker.removeCarry(player)
}