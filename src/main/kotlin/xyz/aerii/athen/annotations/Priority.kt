package xyz.aerii.athen.annotations

/**
 * Controls class loading order by loading these classes in before any classes with the [Load] annotation.
 * Lower numbers load first.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority(val value: Int = 0)
