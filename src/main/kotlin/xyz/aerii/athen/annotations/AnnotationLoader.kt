package xyz.aerii.athen.annotations

import io.github.classgraph.ClassGraph
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.safely

object AnnotationLoader {
    fun load() {
        ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages("xyz.aerii")
            .scan()
            .use { scanResult ->
                val sortedPriority = scanResult
                    .getClassesWithAnnotation(Priority::class.java.name)
                    .loadClasses()
                    .sortedBy { it.getAnnotation(Priority::class.java)?.value ?: 0 }

                val toLoad = scanResult
                    .getClassesWithAnnotation(Load::class.java.name)
                    .loadClasses()

                val modules = scanResult.getSubclasses(Module::class.java.name)

                sortedPriority.forEach { klass ->
                    try {
                        Class.forName(klass.name)
                    } catch (_: Exception) {}
                }

                toLoad.forEach { klass ->
                    try {
                        Class.forName(klass.name)
                    } catch (_: Exception) {}
                }

                on<GameEvent.Start> {
                    for (m in modules) safely { (m.loadClass().kotlin.objectInstance as? Module)?.react }
                }.once()
            }
    }
}