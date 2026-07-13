package xyz.aerii.athen.api.rendering.level.impl.queue.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.rendering.level.impl.data.impl.ExtractedBeam
import xyz.aerii.athen.api.rendering.level.impl.data.impl.ExtractedBox
import xyz.aerii.athen.api.rendering.level.impl.data.impl.ExtractedCircle
import xyz.aerii.athen.api.rendering.level.impl.data.impl.ExtractedLine
import xyz.aerii.athen.api.rendering.level.impl.data.impl.ExtractedText
import xyz.aerii.athen.api.rendering.level.impl.queue.base.ILevelQueue
import xyz.aerii.athen.api.rendering.level.impl.queue.data.ExtractedQueue
import xyz.aerii.athen.api.rendering.level.impl.renderers.base.ILevelRenderer
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.library.api.client

@Load
object LevelQueueImpl : ILevelQueue {
    val renderers: MutableList<ILevelRenderer> = mutableListOf()

    override val beams: MutableList<ExtractedBeam> = mutableListOf()
    override val texts: MutableList<ExtractedText> = mutableListOf()
    override val lines: ExtractedQueue<ExtractedLine> = ExtractedQueue()
    override val boxes0: ExtractedQueue<ExtractedBox> = ExtractedQueue()
    override val boxes1: ExtractedQueue<ExtractedBox> = ExtractedQueue()
    override val circles0: ExtractedQueue<ExtractedCircle> = ExtractedQueue()
    override val circles1: ExtractedQueue<ExtractedCircle> = ExtractedQueue()

    init {
        on<WorldRenderEvent.Render> {
            val camera = client.gameRenderer.mainCamera.position()

            pose.pushPose()
            pose.translate(-camera.x, -camera.y, -camera.z)

            for (r in renderers) r.render(pose, pose.last(), consumers)

            pose.popPose()
            clear()
        }
    }

    override fun clear() {
        beams.clear()
        texts.clear()
        lines.clear()
        boxes0.clear()
        boxes1.clear()
        circles0.clear()
        circles1.clear()
    }
}