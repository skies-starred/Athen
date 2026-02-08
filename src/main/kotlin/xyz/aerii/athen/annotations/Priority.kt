package xyz.aerii.athen.annotations

/**
 * Controls class loading order by loading these classes in before any classes with the [Load] annotation.
 * Lower numbers load first.
 *
 * Priority levels:
 *  - `-6`: [xyz.aerii.athen.updater.ModUpdater]
 *  - `-5`: [xyz.aerii.athen.handlers.Chronos]
 *  - `-4`: [xyz.aerii.athen.config.ConfigManager]
 *  - `-3`: [xyz.aerii.athen.config.ui.ClickGUI]
 *  - `-2`: [xyz.aerii.athen.hud.internal.HUDManager]
 *  - `-1`: [xyz.aerii.athen.hud.internal.HUDEditor]
 *  - `0`: Default (classes using ``@Priority`` without any value specified.)
 *  - Positive values load after defaults
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority(val value: Int = 0)
