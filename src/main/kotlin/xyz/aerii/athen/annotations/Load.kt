package xyz.aerii.athen.annotations

/**
 * Marks a class to be automatically loaded on startup.
 *
 * [Load] classes are loaded after all [Priority] classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Load
