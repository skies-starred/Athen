package xyz.aerii.athen.utils

const val apiUrl: String = "https://api.aerii.xyz"
const val dataUrl: String = "https://data.aerii.xyz"

inline val String.api: String
    get() = "$apiUrl/$this"

inline val String.data: String
    get() = "$dataUrl/$this"