package xyz.aerii.athen.utils

import xyz.aerii.athen.modules.impl.ModSettings

const val apiUrl: String = "https://athen.aerii.xyz"
const val oau: String = "https://api.aerii.xyz"
const val dataUrl: String = "https://data.aerii.xyz"

inline val String.api: String
    get() = if (ModSettings.oldApi) "$oau/$this" else "$apiUrl/$this"

inline val String.data: String
    get() = "$dataUrl/$this"