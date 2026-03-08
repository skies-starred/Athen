/*
 * Original work by [CyanQT](https://github.com/cyanqt) and contributors (Unknown License).
 *
 * Modifications:
 *   Copyright (c) 2025 skies-starred
 *   Licensed under the BSD 3-Clause License.
 *
 * The original (unknown) license applies to the portions derived from CyanQT.
 * Please reach out to @skies.starred on discord if you have any information about the license.
 */

package xyz.aerii.athen.api.dungeon.terminals

import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver

enum class TerminalType(val slots: Int, val regex: Regex, val actual: String? = null) {
    COLORS(54, Regex("^Select all the ([\\w ]+) items!$")),
    MELODY(54, Regex("^Click the button on time!$"), "Click the button on time!"),
    NUMBERS(36, Regex("^Click in order!$"), "Click in order!"),
    PANES(45, Regex("^Correct all the panes!$"), "Correct all the panes!"),
    RUBIX(45, Regex("^Change all to same color!$"), "Change all to same color!"),
    NAME(45, Regex("^What starts with: '(\\w)'\\?$"))
    ;

    val solver: Boolean
        get() = when (this) {
        COLORS -> 0
        MELODY -> 1
        NAME -> 2
        NUMBERS -> 3
        PANES -> 4
        RUBIX -> 5
    } in TerminalSolver.solve

    companion object {
        fun get(windowTitle: String): TerminalType? = entries.firstOrNull { it.regex.matches(windowTitle) }
    }
}