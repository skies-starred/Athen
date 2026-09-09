@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general.messageactions

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import foo.starred.athen.Athen
import foo.starred.athen.Athen.GSON
import foo.starred.athen.annotations.Load
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.Category
import foo.starred.athen.events.GameEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionEntry
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageMatchType
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageResolvedEntry
import foo.starred.athen.modules.impl.general.messageactions.actions.impl.NoAction
import foo.starred.athen.modules.impl.general.messageactions.ui.data.MessageActionsCategoryEntry
import foo.starred.athen.modules.impl.general.messageactions.ui.impl.MessageActionsGUI
import foo.starred.athen.utils.command
import foo.starred.snowbird.utils.colorCoded
import foo.starred.snowbird.utils.compress
import foo.starred.snowbird.utils.decompress
import foo.starred.snowbird.utils.safely
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import tech.thatgravyboat.skyblockapi.helpers.McClient
import java.lang.reflect.Type

@Load
object MessageActions : Module(
    "Message actions",
    "Allows you to run actions when you receive a message.",
    Category.GENERAL
) {
    private val _unused by config.button("Open manager") { MessageActionsGUI.open() }
    private val _unused0 by config.information("You can use the commands <red>\"/${Athen.modId} [import|export] messageactions\"<r> to share configs!")

    private val json = JsonStore("features/MessageActions")
    private var _actions: String by json.string("actions")
    private var _categories: String by json.string("categories")

    private val gson: Gson = GSON.newBuilder()
        .registerTypeAdapter(MessageActionEntry::class.java, object : JsonSerializer<MessageActionEntry>, JsonDeserializer<MessageActionEntry> {
            override fun serialize(src: MessageActionEntry, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                val action = JsonObject().apply {
                    addProperty("id", src.action.id)
                    add("data", JsonObject().apply { for ((k, v) in src.action.serializable) addProperty(k, v) })
                }

                return JsonObject().apply {
                    addProperty("pattern", src.pattern)
                    addProperty("match", src.match.name)
                    add("action", action)
                    addProperty("enabled", src.enabled)
                    addProperty("category", src.category)
                    addProperty("cancel", src.cancel)
                    addProperty("delay", src.delay)
                }
            }

            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessageActionEntry {
                val obj = json.asJsonObject
                val pattern = obj.get("pattern")?.asString ?: ""
                val match = runCatching { MessageMatchType.valueOf(obj.get("match").asString) }.getOrDefault(MessageMatchType.CONTAINS)
                val enabled = obj.get("enabled")?.asBoolean ?: true
                val category = obj.get("category")?.asString ?: ""
                val cancel = obj.get("cancel")?.asBoolean ?: false
                val delay = obj.get("delay")?.asDouble ?: 0.0
                val action0 = obj.getAsJsonObject("action")
                val id = action0?.get("id")?.asInt ?: obj.get("id")?.asInt ?: 0

                val data =
                    if (action0 != null) action0.getAsJsonObject("data")?.entrySet()?.associate { it.key to it.value.asString } ?: emptyMap()
                    else IMessageAction.get(id)?.fields?.mapIndexed { i, v -> v.key to (obj.get(v.key)?.asString ?: if (i == 0) obj.get("value")?.asString else v.defaultValue) }?.toMap() ?: emptyMap()

                val action = IMessageAction.create(id, data)
                return MessageActionEntry(pattern, match, action, enabled, category, cancel, delay)
            }
        })
        .create()

    private var set = emptySet<String>()
    private var a0 = emptyArray<MessageResolvedEntry>()
    private var a1 = emptyArray<MessageResolvedEntry>()

    val actions = ObjectArrayList<MessageActionEntry>()
    val categories = ObjectArrayList<MessageActionsCategoryEntry>()

    init {
        on<GameEvent.Start> {
            safely {
                val ra = _actions.takeIf { it.isNotBlank() }
                if (ra != null) {
                    actions.clear()
                    actions.addAll(gson.fromJson<List<MessageActionEntry>>(ra, object : TypeToken<List<MessageActionEntry>>() {}.type))
                }

                val rc = _categories.takeIf { it.isNotBlank() }
                if (rc != null) {
                    categories.clear()
                    categories.addAll(GSON.fromJson<List<MessageActionsCategoryEntry>>(rc, object : TypeToken<List<MessageActionsCategoryEntry>>() {}.type))
                }

                fn()
            }
        }

        on<GameEvent.Stop> {
            disk()
        }

        on<MessageEvent.Chat.Intercept> {
            var text: String? = null

            for (entry in a0) {
                if (!entry.matches(stripped, set)) continue
                cancel()

                if (entry.action == NoAction) return@on
                val action = entry.action(text ?: message.colorCoded().also { text = it })
                if (entry.source.delay > 0.0) Scheduler.schedule(entry.delay) { action.run() } else action.run()
            }
        }

        on<MessageEvent.Chat.Receive> {
            var text: String? = null

            for (entry in a1) {
                if (!entry.matches(stripped, set)) continue

                val action = entry.action(text ?: message.colorCoded().also { text = it })
                if (entry.source.delay > 0.0) Scheduler.schedule(entry.delay) { action.run() } else action.run()
            }
        }

        command {
            "messageactions" {
                MessageActionsGUI.open()
            }

            "messageactions" / "gui" {
                MessageActionsGUI.open()
            }

            "export" / "messageactions" {
                disk()
                McClient.clipboard = gson.toJson(mapOf("actions" to actions, "categories" to categories)).compress()
                "Exported ${actions.size} actions to clipboard!".mod()
            }

            "import" / "messageactions" {
                val a = McClient.clipboard
                if (a.isEmpty()) return@invoke "No data found in clipboard!".mod()

                safely {
                    val map = gson.fromJson(a.decompress(), object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                    val b = gson.fromJson<List<MessageActionEntry>>(gson.toJson(map["actions"]), object : TypeToken<List<MessageActionEntry>>() {}.type)
                    val c = GSON.fromJson<List<MessageActionsCategoryEntry>>(GSON.toJson(map["categories"]), object : TypeToken<List<MessageActionsCategoryEntry>>() {}.type)

                    actions.clear()
                    actions.addAll(b)
                    categories.clear()
                    categories.addAll(c)

                    fn()
                    disk()
                    "Imported ${b.size} actions and ${c.size} categories!".mod()
                }
            }
        }
    }

    fun fn() {
        set = HashSet<String>(categories.size).also { set ->
            for ((name, enabled1) in categories) if (!enabled1) set.add(name)
        }

        val a = ObjectArrayList<MessageResolvedEntry>()
        val b = ObjectArrayList<MessageResolvedEntry>()
        for (a0 in actions) {
            val r = MessageResolvedEntry(a0)
            if (a0.cancel) a.add(r) else if (a0.action != NoAction) b.add(r)
        }

        a0 = a.toArray(emptyArray())
        a1 = b.toArray(emptyArray())
    }

    fun disk() {
        _actions = gson.toJson(actions)
        _categories = GSON.toJson(categories)
    }

    fun add(pattern: String, match: MessageMatchType, action: IMessageAction, category: String, cancel: Boolean, delay: Double): Boolean {
        if (pattern.isBlank()) return false
        actions.add(MessageActionEntry(pattern, match, action, true, category, cancel, delay))
        fn()
        return true
    }

    fun remove(index: Int): Boolean {
        if (index !in actions.indices) return false
        actions.removeAt(index)
        fn()
        return true
    }

    fun update(index: Int, entry: MessageActionEntry): Boolean {
        if (index !in actions.indices || entry.pattern.isBlank()) return false
        actions[index] = entry
        fn()
        return true
    }

    fun add(name: String): Boolean {
        if (name.isBlank() || categories.any { it.name == name }) return false
        categories.add(MessageActionsCategoryEntry(name))
        fn()
        return true
    }

    fun remove(name: String) {
        categories.removeIf { it.name == name }

        for (i in actions.indices) {
            val a = actions[i].takeIf { it.category == name } ?: continue
            actions[i] = a.copy(category = "")
        }

        fn()
    }

    fun toggle(name: String) {
        for (i in categories.indices) {
            val c = categories[i].takeIf { it.name == name } ?: continue
            categories[i] = c.copy(enabled = !c.enabled)
            break
        }

        fn()
    }
}
