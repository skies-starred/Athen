package xyz.aerii.athen.modules.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Scribble

@Load
object Dev {
    @JvmStatic
    val file = Scribble("main/Dev")

    @JvmStatic
    var lastVersion: String by file.string("lastVersion")

    @JvmStatic
    var lastBroadcast: String by file.string("lastBroadcast")

    @JvmStatic
    var debug: Boolean by file.boolean("enabled")

    @JvmStatic
    var clickUiHelperCollapsed: Boolean by file.boolean("clickUiHelperCollapsed")

    @JvmStatic
    var clickUiHelperHidden: Boolean by file.boolean("clickUiHelperHidden")
}