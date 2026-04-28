package xyz.aerii.athen.annotations

import io.github.classgraph.ClassGraph
import xyz.aerii.athen.api.websocket.WebSocket
import xyz.aerii.athen.api.websocket.base.IWebSocket
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.modules.Module
import xyz.aerii.library.utils.safely

object AnnotationLoader {
    fun load() {
        ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages("xyz.aerii.athen", "xyz.aerii.nebulune")
            .scan()
            .use { s ->
                val a = s.getClassesWithAnnotation(Priority::class.java.name).loadClasses().sortedBy { it.getAnnotation(Priority::class.java)?.value ?: 0 }
                val b = s.getClassesWithAnnotation(Load::class.java.name).loadClasses()
                val c = s.getClassesWithAnnotation(Websocket::class.java).loadClasses()

                loop@ for (k in a) {
                    safely {
                        Class.forName(k.name)
                    }
                }

                loop@ for (k in b) {
                    safely {
                        Class.forName(k.name)
                    }
                }

                loop@ for (k in c) {
                    safely {
                        Class.forName(k.name)
                        WebSocket.all.add(k.kotlin.objectInstance as? IWebSocket ?: continue@loop)
                    }
                }

                val d = s.getSubclasses(Module::class.java.name)

                on<GameEvent.Start> {
                    for (m in d) {
                        safely {
                            (m.loadClass().kotlin.objectInstance as? Module)?.observable
                        }
                    }
                }.once()
            }
    }
}