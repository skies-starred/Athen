package foo.starred.athen.modules.impl.dungeon.partyfinder.enums

import foo.starred.athen.modules.impl.dungeon.partyfinder.impl.PartyFinderDisplay

enum class PartyFinderSlotStatus(private val _color: () -> Int) {
    BLOCKED({ PartyFinderDisplay.`color$blocked` }),
    ALLOWED({ PartyFinderDisplay.`color$allowed` }),
    MAYBE({ PartyFinderDisplay.`color$maybe` }),
    CARRY({ PartyFinderDisplay.`color$carry` }),
    VC({ PartyFinderDisplay.`color$vc` }),
    PERM({ PartyFinderDisplay.`color$perm` });

    val color: Int
        get() = _color()
}