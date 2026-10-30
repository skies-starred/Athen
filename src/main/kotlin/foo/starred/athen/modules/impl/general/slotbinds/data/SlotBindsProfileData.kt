package foo.starred.athen.modules.impl.general.slotbinds.data

data class SlotBindsProfileData(
    val binds: MutableMap<String, Int> = mutableMapOf(),
    val colors: MutableMap<String, Int> = mutableMapOf()
)
