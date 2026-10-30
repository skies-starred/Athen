@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.render.radial.ui.editor

import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.actions.IAction
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialForm
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialHeader
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialPreview
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialTree
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.screen.CascadeScreen

object RadialEditor : CascadeScreen("Radial Menu Editor [Athen]", CascadeGeometricResolution.FHD.of(2f)) {
    private var head: RadialHeader
    private var tree: RadialTree
    private var form: RadialForm
    private var view: RadialPreview
    private var last = -1

    val working = mutableListOf<RadialSlot>()
    val collapsed = mutableSetOf<Int>()

    var main = 0
    var sub = -1
    var editing = false
    var type = 0

    val slot: RadialSlot?
        get() {
            if (sub >= 0) return working.getOrNull(main)?.sub?.getOrNull(sub)
            return working.getOrNull(main)
        }

    val names: List<String>
        get() {
            return RadialMenu.store.profiles.keys.toList()
        }

    val max: Int
        get() {
            return when (RadialMenu.type) {
                1 -> 5
                2 -> working.size
                else -> Int.MAX_VALUE
            }
        }

    init {
        val main = container {
            size = FixedSizeConstraint(906, 320)
            position = CenterPositionConstraint()
            attach(scene)
        }

        val side = rectangle {
            size = FixedSizeConstraint(130, 320)
            position = FixedPositionConstraint(0, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        head = RadialHeader(side)

        tree = RadialTree(scrollable {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 298))
            position = FixedPositionConstraint(0, 20)
            attach(side)
        })

        form = RadialForm(container {
            size = FixedSizeConstraint(444, 320)
            position = FixedPositionConstraint(136, 0)
            attach(main)
        })

        view = RadialPreview(main)
    }

    override fun init() {
        super.init()

        working.clear()
        working.addAll(RadialMenu.slots)
        collapsed.clear()
        reload(0, -1)
    }

    override fun removed() {
        save()
        super.removed()
    }

    fun extra(): List<Pair<Int, RadialSlot>> {
        if (RadialMenu.type != 2) return emptyList()
        if (main !in working.indices) return emptyList()

        val list0 = working[main].sub
        val n = list0.size
        val m = maxOf(1, working.size)
        val p = main

        return List(n) { i ->
            ((p - n / 2 + i + m * 2) % m) to list0[i]
        }
    }

    fun reload(m0: Int = main, s0: Int = sub) {
        main = m0.coerceIn(0, maxOf(0, working.size - 1))
        sub = if (s0 >= 0) s0.coerceIn(0, maxOf(0, (working.getOrNull(main)?.sub?.size ?: 1) - 1)) else -1
        unfocus()

        val s = slot ?: run {
            form.name.value = ""
            form.item.value = ""
            form.value.value = ""
            form.texture.value = ""
            head.fn()
            tree.fn()
            form.fn()
            return
        }

        form.name.value = s.name
        form.item.value = s.itemId
        type = s.action.id
        form.value.value = s.action.serializable
        form.texture.value = s.text ?: ""

        head.fn()
        tree.fn()
        form.fn()
    }

    fun commit() {
        val s = slot ?: return
        s.name = form.name.value
        s.itemId = form.item.value
        s.text = form.texture.value.ifBlank { null }
        s.action = IAction.create(type, form.value.value)
    }

    fun sync() {
        commit()
        tree.fn()
    }

    fun unfocus() {
        scene.focused = null
        editing = false
    }

    fun rename() {
        val n = head.title.value.trim()
        if (n.isBlank() || n == RadialMenu.store.active || n in names) {
            editing = false
            head.fn()
            return
        }

        RadialMenu.rename(RadialMenu.store.active, n)
        editing = false
        head.fn()
    }

    fun save() {
        commit()
        RadialMenu.slots.clear()
        RadialMenu.slots.addAll(working)
        RadialMenu.save()
    }

    fun switch(name: String) {
        RadialMenu.load(name)
        working.clear()
        working.addAll(RadialMenu.slots)
        collapsed.clear()
        reload(0, -1)
    }
}
