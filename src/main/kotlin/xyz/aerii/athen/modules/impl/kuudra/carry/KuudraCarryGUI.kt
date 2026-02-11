package xyz.aerii.athen.modules.impl.kuudra.carry

import xyz.aerii.athen.modules.common.carry.ICarryGUI

object KuudraCarryGUI : ICarryGUI<KuudraCarryStateTracker.TrackedCarry>("Kuudra Carry Tracker") {
    override fun carries() = KuudraCarryStateTracker.tracked
    override fun persist() = KuudraCarryStateTracker.persist()
    override fun remove(player: String) = KuudraCarryStateTracker.removeCarry(player)
}