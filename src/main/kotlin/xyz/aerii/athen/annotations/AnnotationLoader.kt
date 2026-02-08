package xyz.aerii.athen.annotations

import io.github.classgraph.ClassGraph

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
            }
    }
}
