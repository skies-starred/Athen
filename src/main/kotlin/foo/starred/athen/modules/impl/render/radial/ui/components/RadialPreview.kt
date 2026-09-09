package foo.starred.athen.modules.impl.render.radial.ui.components

import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialOverlay
import foo.starred.athen.modules.impl.render.radial.utils.RadialRenderState
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RenderStatePrimitive.Companion.renderState

class RadialPreview(main: ContainerPrimitive) {
    var panel: RectanglePrimitive
        private set

    init {
        panel = rectangle {
            size = FixedSizeConstraint(320, 320)
            position = FixedPositionConstraint(586, 0)
            color = Mocha.Base.argb

            effect(OutlineEffect {
                color = Mocha.Surface0.argb
            })

            on<MouseEvent.Press> {
                if (button != 0) return@on
                cancel()

                val s0 = RadialEditor.main
                val s1 = RadialEditor.sub
                val list0 = RadialEditor.working

                val x0 = x.toFloat()
                val y0 = y.toFloat()

                val x1 = self.x.toInt() + self.width.toInt() / 2
                val y1 = self.y.toInt() + self.height.toInt() / 2

                val b0 = RadialMenu.type == 0 && s1 >= 0 && s0 in list0.indices
                val cur0 = if (b0) list0[s0].sub else list0
                val num0 = maxOf(1, cur0.size)

                val x2 = x0 - x1
                val y2 = y0 - y1
                val d0 = x2 * x2 + y2 * y2

                if (d0 < 144f) {
                    if (b0 || (RadialMenu.type == 2 && s0 in list0.indices && s1 >= 0)) RadialEditor.reload(s0, -1)
                    return@on
                }

                val ex0 = if (b0) emptyList() else RadialEditor.extra()

                var h0 = -1
                var h1 = -1

                if (d0 >= 225f) {
                    if (!b0 && s0 in list0.indices) {
                        h1 = when (RadialMenu.type) {
                            2 -> RadialRenderState.hitRing(x0, y0, x1, y1, num0, RadialMenu.radius2, ex0.map { it.first }, false, RadialMenu.thickness)
                            1 -> RadialRenderState.hitNested(x0, y0, x1, y1, num0, RadialMenu.radius2, s0, list0[s0].sub.size, false, RadialMenu.thickness)
                            else -> -1
                        }

                        if (h1 != -1 && RadialMenu.type == 1) h0 = s0
                    }

                    if (h0 == -1 && h1 == -1) h0 = RadialRenderState.hit(x0, y0, x1, y1, num0, RadialMenu.radius1, RadialMenu.radius2)
                }

                RadialEditor.commit()

                if (b0) {
                    if (h0 != -1) RadialEditor.reload(s0, h0)
                    return@on
                }

                if (h1 != -1) {
                    RadialEditor.reload(s0, h1)
                    return@on
                }

                if (h0 == -1) return@on

                if (RadialMenu.type == 0 && list0.getOrNull(h0)?.sub?.isNotEmpty() == true) RadialEditor.reload(h0, 0)
                else RadialEditor.reload(h0, -1)
            }

            attach(main)
        }

        renderState {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(panel)

            provider = { graphics ->
                val s0 = RadialEditor.main
                val s1 = RadialEditor.sub
                val list0 = RadialEditor.working

                val x1 = panel.x.toInt() + panel.width.toInt() / 2
                val y1 = panel.y.toInt() + panel.height.toInt() / 2

                val b0 = RadialMenu.type == 0 && s1 >= 0 && s0 in list0.indices
                val cur0 = if (b0) list0[s0].sub else list0
                val num0 = maxOf(1, cur0.size)

                val x2 = mouseX - x1
                val y2 = mouseY - y1
                val d0 = x2 * x2 + y2 * y2

                val ex0 = if (b0) emptyList() else RadialEditor.extra()

                var h0 = -1
                var h1 = -1

                if (d0 >= 225f) {
                    if (!b0 && s0 in list0.indices) {
                        h1 = when (RadialMenu.type) {
                            2 -> RadialRenderState.hitRing(mouseX, mouseY, x1, y1, num0, RadialMenu.radius2, ex0.map { it.first }, false, RadialMenu.thickness)
                            1 -> RadialRenderState.hitNested(mouseX, mouseY, x1, y1, num0, RadialMenu.radius2, s0, list0[s0].sub.size, false, RadialMenu.thickness)
                            else -> -1
                        }

                        if (h1 != -1 && RadialMenu.type == 1) h0 = s0
                    }

                    if (h0 == -1 && h1 == -1) h0 = RadialRenderState.hit(mouseX, mouseY, x1, y1, num0, RadialMenu.radius1, RadialMenu.radius2)
                }

                val mini0 = if (RadialMenu.type == 1 && s0 in list0.indices) list0[s0].sub else emptyList()
                val ring0 = if (RadialMenu.type == 2 && h1 != -1) ex0.getOrNull(h1)?.first ?: -1 else -1

                RadialRenderState(graphics, x1, y1, num0, mini0, ex0, if (b0) -1 else s0, if (h1 != -1 && RadialMenu.type == 1) h1 else if (b0) h0 else if (h1 != -1) h1 else h0, s1, ring0)
            }

            ascend = true
        }

        RadialOverlay(panel).apply {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(panel)
        }
    }
}
