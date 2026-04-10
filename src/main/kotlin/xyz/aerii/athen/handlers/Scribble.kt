@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import xyz.aerii.athen.Athen
import xyz.aerii.library.handlers.data.AbstractScribble

class Scribble(path: String, tts: Int = 15) : AbstractScribble(Athen.modId, path, tts)