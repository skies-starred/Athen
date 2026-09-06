@file:Suppress("unused")

package foo.starred.athen.modules.impl.render.highlight

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import foo.starred.athen.Athen
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.Category
import foo.starred.athen.ducks.entity.EntityDuck.Companion.parent
import foo.starred.athen.events.InputEvent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.render.highlight.ui.MobHighlightGUI
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.command
import foo.starred.athen.utils.name
import foo.starred.athen.utils.render.renderBoundingBox
import foo.starred.snowbird.api.center
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.api.text.parser.impl.parse
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.extentions.serverMaxHealth

//? if >= 26.2 {
/*import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
*///? }

@Load
@OnlyIn(skyblock = true)
object MobHighlight : Module(
    "Mob highlight",
    "Highlights mobs",
    Category.RENDER
) {
    val json = JsonStore("features/mobHighlight")
    val e0 = json.mutableList("e0", EntityNamed.CODEC)
    val e1 = json.mutableList("e1", EntityTyped.CODEC)

    private val key by config.switch("Highlight key", true)
    private val keybind by config.keybind("Key to add entity")
    private val _unused by config.button("Open manager") { MobHighlightGUI.open() }
    private val _unused0 by config.information("You can use the command <red>\"/${Athen.modId} highlight help\"<r> to view all commands!")

    private var wow: Long = -1
    private val int: MutableList<Int> = mutableListOf()
    private val map: Int2IntOpenHashMap = Int2IntOpenHashMap().apply { defaultReturnValue(Int.MIN_VALUE) }

    init {
        command {
            "highlight" / "add" / "named" / string("color") / int("maxHp") / greedyString("name") {
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)
                val max = int("maxHp")
                val name = string("name")

                e0.update { add(EntityNamed(name, color, max)) }
                "Added entity highlight for <red>\"$name\"<r> <gray>[Max HP=$max]<r> with color <$color>$c0<r>!".mod()
            }

            "highlight" / "add" / "named" / string("color") / greedyString("name") {
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)
                val name = string("name")

                e0.update { add(EntityNamed(name, color)) }
                "Added entity highlight for <red>\"$name\"<r> with color <$color>$c0<r>!".mod()
            }

            "highlight" / "add" / "typed" / string("color") / int("maxHp") / string("type") {
                val t0 = string("type")
                //~ if >= 26.2 'EntityType.byString(t0)' -> 'BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(t0))'
                val type = EntityType.byString(t0).orElse(null) ?: return@string
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)
                val max = int("maxHp")

                e1.update { add(EntityTyped(type, color, max)) }
                "Added entity highlight for <red>\"$t0\"<r> <gray>[Max HP=$max]<r> with color <$color>$c0<r>!".mod()
            }

            "highlight" / "add" / "typed" / string("color") / string("type") {
                val t0 = string("type")
                //~ if >= 26.2 'EntityType.byString(t0)' -> 'BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(t0))'
                val type = EntityType.byString(t0).orElse(null) ?: return@string
                val c0 = string("color")
                val color = c0.removePrefix("#").toInt(16)

                e1.update { add(EntityTyped(type, color)) }
                "Added entity highlight for <red>\"$t0\"<r> with color <$color>$c0<r>!".mod()
            }

            "highlight" / "remove" / "named" / greedyString("name") {
                val name = string("name")

                e0.update { removeIf { it.name == name } }
                "Removed highlight for <red>\"$name\"<r>!".mod()
            }

            "highlight" / "remove" / "typed" / string("type") {
                val t0 = string("type")
                //~ if >= 26.2 'EntityType.byString(t0)' -> ' BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(t0))'
                val type = EntityType.byString(t0).orElse(null) ?: return@string

                e1.update { removeIf { it.type == type } }
                "Removed highlight for <red>\"$t0\"<r>!".mod()
            }

            "highlight" / "list" / "named" {
                val a = ("<dark_gray>" + ("-".repeat())).parse()

                a.lie()
                "Highlight list <gray>[Named]<r>:".mod()
                a.lie()

                for (b in e0.value) {
                    " <dark_gray>- <gray>${b.name}${if (b.max != -1) " <dark_gray>[Max HP: ${b.max}]<gray>" else ""} <dark_gray>- <${b.color}>${b.color.toHexString()}".parse().lie()
                }

                a.lie()
            }

            "highlight" / "list" / "typed" {
                val a = ("<dark_gray>" + ("-".repeat())).parse()

                a.lie()
                "Highlight list <gray>[Typed]<r>:".mod()
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

                var b = a.serverMaxHealth.toInt()
                var id = a.id
                if (b0 && a.isInvisible) {
                    val e = l.getEntity(a0) as? LivingEntity ?: continue
                    if (e is ArmorStand) continue
                    if (e.isInvisible) continue

                    b = e.serverMaxHealth.toInt()
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

        on<WorldRenderEvent.Entity> {
            val e = entity ?: return@on
            val color = map.get(e.id).takeIf { it != Int.MIN_VALUE }?.or(0xFF000000.toInt()) ?: return@on
            fn1(e.renderBoundingBox, color)
        }

        on<InputEvent.Keyboard.Press> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen != null) return@on
            if (keyEvent.key() != keybind) return@on
            fn()
        }

        on<InputEvent.Mouse.Press> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
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
        val max = a.serverMaxHealth
        val type = a.type

        MobHighlightGUI.pop(name, type, max.toInt())
    }

    private fun fn1(aabb: AABB, color: Int) {
        val depth = true

        extractFrameBox(aabb, color, depth = depth)
    }

    data class EntityNamed(
        val name: String,
        override val color: Int = -1,
        override val max: Int = -1
    ) : EntityHighlight {
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
        override val color: Int = -1,
        override val max: Int = -1
    ) : EntityHighlight {
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

    interface EntityHighlight {
        val color: Int
        val max: Int
    }
}
