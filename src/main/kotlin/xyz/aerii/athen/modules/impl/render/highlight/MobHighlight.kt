@file:Suppress("unused")

package xyz.aerii.athen.modules.impl.render.highlight

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.ducks.entity.parent
import xyz.aerii.athen.events.InputEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.render.highlight.popup.MobHighlightPopup
import xyz.aerii.athen.modules.impl.render.highlight.ui.MobHighlightGUI
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.name
import xyz.aerii.athen.utils.render.renderBoundingBox
import xyz.aerii.library.api.center
import xyz.aerii.library.api.client
import xyz.aerii.library.api.lie
import xyz.aerii.library.api.repeat
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.kommand.ICommand

@Load
@OnlyIn(skyblock = true)
object MobHighlight : Module(
    "Mob highlight",
    "Highlights mobs",
    Category.RENDER
), ICommand {
    val scribble = Scribble("features/mobHighlight")
    val e0 = scribble.mutableList("e0", EntityNamed.CODEC)
    val e1 = scribble.mutableList("e1", EntityTyped.CODEC)

    private val key by config.switch("Highlight key", true)
    private val keybind by config.keybind("Key to add entity")
    private val _unused by config.button("Open manager") { MobHighlightGUI.open() }
    private val _unused0 by config.textParagraph("You can use the command <red>\"/${Athen.modId} highlight help\"<r> to view all commands!")

    private var wow: Long = -1
    private val int: MutableList<Int> = mutableListOf()
    private val map: Int2IntOpenHashMap = Int2IntOpenHashMap().apply { defaultReturnValue(Int.MIN_VALUE) }

    init {
        command(Athen.modId) {
            "highlight" / "add" / "named" / string("color") / int("maxHp") / greedyString("name") {
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)
                val max = int("maxHp")
                val name = string("name")

                e0.update { add(EntityNamed(name, color, max)) }
                "Added entity highlight for <red>\"$name\"<r> <gray>[Max HP=$max]<r> with color <$color>$c0<r>!".parse().modMessage()
            }

            "highlight" / "add" / "named" / string("color") / greedyString("name") {
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)
                val name = string("name")

                e0.update { add(EntityNamed(name, color)) }
                "Added entity highlight for <red>\"$name\"<r> with color <$color>$c0<r>!".parse().modMessage()
            }

            "highlight" / "add" / "typed" / string("color") / int("maxHp") / string("type") {
                val t0 = string("type")
                val type = EntityType.byString(t0).orElse(null) ?: return@string
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)
                val max = int("maxHp")

                e1.update { add(EntityTyped(type, color, max)) }
                "Added entity highlight for <red>\"$t0\"<r> <gray>[Max HP=$max]<r> with color <$color>$c0<r>!".parse().modMessage()
            }

            "highlight" / "add" / "typed" / string("color") / string("type") {
                val t0 = string("type")
                val type = EntityType.byString(t0).orElse(null) ?: return@string
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)

                e1.update { add(EntityTyped(type, color)) }
                "Added entity highlight for <red>\"$t0\"<r> with color <$color>$c0<r>!".parse().modMessage()
            }

            "highlight" / "remove" / "named" / greedyString("name") {
                val name = string("name")

                e0.update { removeIf { it.name == name } }
                "Removed highlight for <red>\"$name\"<r>!".parse().modMessage()
            }

            "highlight" / "remove" / "typed" / string("type") {
                val t0 = string("type")
                val type = EntityType.byString(t0).orElse(null) ?: return@string

                e1.update { removeIf { it.type == type } }
                "Removed highlight for <red>\"$t0\"<r>!".parse().modMessage()
            }

            "highlight" / "list" / "named" {
                val a = ("<dark_gray>" + ("-".repeat())).parse()

                a.lie()
                "Highlight list <gray>[Named]<r>:".parse().modMessage()
                a.lie()

                for (b in e0.value) {
                    " <dark_gray>- <gray>${b.name}${if (b.max != -1) " <dark_gray>[Max HP: ${b.max}]<gray>" else ""} <dark_gray>- <${b.color}>${b.color.toHexString()}".parse().lie()
                }

                a.lie()
            }

            "highlight" / "list" / "typed" {
                val a = ("<dark_gray>" + ("-".repeat())).parse()

                a.lie()
                "Highlight list <gray>[Typed]<r>:".parse().modMessage()
                a.lie()

                for (b in e1.value) {
                    " <dark_gray>- <gray>${b.type}${if (b.max != -1) " <dark_gray>[Max HP: ${b.max}]<gray>" else ""} <dark_gray>- <${b.color}>${b.color.toHexString()}".parse().lie()
                }

                a.lie()
            }

            "highlight" / "help" {
                val a = ("<dark_gray>" + ("-".repeat())).parse()
                val b = Athen.modId
                val c = Catppuccin.Mocha.Green.argb

                a.lie()
                ("<red>" + ("Athen Higlights".center())).parse().lie()
                a.lie()

                " <dark_gray>- <$c>/$b highlight add [named | typed] <color> <maxHp - optional> <name | type>".parse().lie()
                " <dark_gray>- <$c>/$b highlight remove [named | typed] <name | type>".parse().lie()
                " <dark_gray>- <$c>/$b highlight list [named | typed]".parse().lie()
                " <dark_gray>- <$c>/$b highlight [gui - optional]".parse().lie()

                a.lie()
            }

            "highlight" / "gui" {
                MobHighlightGUI.open()
            }

            "highlight" {
                MobHighlightGUI.open()
            }
        }

        on<TickEvent.Client.End> {
            if (ticks % 10 != 0) return@on
            val l = client.level ?: return@on
            val e0 = e0.value
            val e1 = e1.value
            val e2 = (e0.hashCode().toLong() shl 32) xor (e1.hashCode().toLong() and 0xFFFFFFFFL)

            if (wow != e2) {
                int.clear()
                map.clear()
                wow = e2
            }

            for (a in l.entitiesForRendering()) {
                if (!a.isAlive) continue

                val b0 = a is ArmorStand
                val a0 = if (b0) a.parent?.id ?: (a.id - 1) else -1
                val a = (a as? LivingEntity)?.takeIf { it.id !in int && (!b0 || a0 !in int) } ?: continue

                var b = a.maxHealth.toInt()
                var id = a.id
                if (b0 && a.isInvisible) {
                    val e = l.getEntity(a0) as? LivingEntity ?: continue
                    if (e is ArmorStand) continue
                    if (e.isInvisible) continue

                    b = e.maxHealth.toInt()
                    id = a0
                }

                val d = a.customName?.name
                if (d != null) {
                    e0.find { d == it.name && (it.max == -1 || it.max == b) }?.let {
                        int += id
                        map[id] = it.color
                        continue
                    }
                }

                val e = a.type
                e1.find { it.type == e && (it.max == -1 || it.max == b) }?.let {
                    int += id
                    map[id] = it.color
                    continue
                }
            }

            if (ticks % 12000 != 0) return@on
            val s = map.iterator()
            while (s.hasNext()) {
                val e = l.getEntity(s.next().key)
                if (e == null || !e.isAlive) s.remove()
            }
        }

        on<WorldRenderEvent.Entity.Pre> {
            val e = entity ?: return@on
            val color = map.get(e.id).takeIf { it != Int.MIN_VALUE }?.or(0xFF000000.toInt()) ?: return@on
            fn1(e.renderBoundingBox, color)
        }

        on<InputEvent.Keyboard.Press> {
            if (client.screen != null) return@on
            if (keyEvent.key() != keybind) return@on
            fn()
        }

        on<InputEvent.Mouse.Press> {
            if (client.screen != null) return@on
            if (buttonInfo.button() != keybind) return@on
            fn()
        }

        on<LocationEvent.Server.Connect> {
            map.clear()
            int.clear()
        }
    }

    private fun fn() {
        val a = client.crosshairPickEntity as? LivingEntity ?: return
        val name = (client.level?.getEntity(a.id + 1) as? ArmorStand)?.customName?.name
        val max = a.maxHealth
        val type = a.type

        MobHighlightPopup.open(name, type, max.toInt())
    }

    private fun fn1(aabb: AABB, color: Int) {
        val depth = true

        extractFrameBox(aabb, color, depth = depth)
    }

    data class EntityNamed(
        val name: String,
        val color: Int = -1,
        val max: Int = -1
    ) {
        companion object {
            val CODEC: Codec<EntityNamed> = RecordCodecBuilder.create { i ->
                i.group(
                    Codec.STRING.fieldOf("name").forGetter(EntityNamed::name),
                    Codec.INT.optionalFieldOf("color", -1).forGetter(EntityNamed::color),
                    Codec.INT.optionalFieldOf("max", -1).forGetter(EntityNamed::max)
                ).apply(i, ::EntityNamed)
            }
        }
    }

    data class EntityTyped(
        val type: EntityType<*>,
        val color: Int = -1,
        val max: Int = -1
    ) {
        companion object {
            val CODEC: Codec<EntityTyped> = RecordCodecBuilder.create { i ->
                i.group(
                    EntityType.CODEC.fieldOf("type").forGetter(EntityTyped::type),
                    Codec.INT.optionalFieldOf("color", -1).forGetter(EntityTyped::color),
                    Codec.INT.optionalFieldOf("max", -1).forGetter(EntityTyped::max)
                ).apply(i, ::EntityTyped)
            }
        }
    }
}