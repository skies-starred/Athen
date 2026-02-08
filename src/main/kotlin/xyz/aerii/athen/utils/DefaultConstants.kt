@file:Suppress("unused")

package xyz.aerii.athen.utils

import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Texter.literal
import java.util.*

@JvmField
val EMPTY_UUID = UUID(0, 0)

@JvmField
val EMPTY_COMPONENT = "".literal()

@JvmField
val ALWAYS_TRUE = React(true).immutable()

@JvmField
val ALWAYS_FALSE = React(false).immutable()