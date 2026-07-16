package xyz.aerii.athen.modules.impl.render

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.modules.Module

@Load
object CameraHelper : Module(
    "Camera helper",
    "QoL additions to the vanilla camera.",
    Category.RENDER
) {
    private val _front by config.switch("No front camera", true)

    val front: Boolean
        get() = enabled && _front
}