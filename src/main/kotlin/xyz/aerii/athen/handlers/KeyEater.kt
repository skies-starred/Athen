package xyz.aerii.athen.handlers

import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Smoothie.client

@Priority
object KeyEater {
    @JvmStatic
    val Int.bound: Boolean
        get() = this != -1

    @JvmStatic
    val Int.pressed: Boolean
        get() = when {
            !bound -> false
            this > 0 -> keyed
            else -> moused
        }

    @JvmStatic
    val Int.keyed: Boolean
        get() {
            val a = GLFW.glfwGetKey(client.window.handle(), this)
            return a == GLFW.GLFW_PRESS || a == GLFW.GLFW_REPEAT
        }

    @JvmStatic
    val Int.moused: Boolean
        get() {
            val a = GLFW.glfwGetMouseButton(client.window.handle(), this)
            return a == GLFW.GLFW_PRESS || a == GLFW.GLFW_REPEAT
        }

    @JvmStatic
    val shift: Boolean
        get() = GLFW.GLFW_KEY_LEFT_SHIFT.keyed || GLFW.GLFW_KEY_RIGHT_SHIFT.keyed

    @JvmStatic
    val ctrl: Boolean
        get() = GLFW.GLFW_KEY_LEFT_CONTROL.keyed || GLFW.GLFW_KEY_RIGHT_CONTROL.keyed
}