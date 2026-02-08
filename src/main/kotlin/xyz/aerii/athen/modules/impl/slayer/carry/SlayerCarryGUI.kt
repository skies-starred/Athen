package xyz.aerii.athen.modules.impl.slayer.carry

import xyz.aerii.athen.modules.common.carry.ICarryGUI

object SlayerCarryGUI : ICarryGUI<SlayerCarryStateTracker.TrackedCarry>("Slayer Carry Tracker") {
    override fun carries() = SlayerCarryStateTracker.tracked
    override fun persist() = SlayerCarryStateTracker.persist()
    override fun remove(player: String) = SlayerCarryStateTracker.removeCarry(player)
}