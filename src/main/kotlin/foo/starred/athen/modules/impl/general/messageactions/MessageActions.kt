@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general.messageactions

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import foo.starred.athen.Athen
import foo.starred.athen.Athen.GSON
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.GameEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.general.messageactions.actions.base.IMessageAction
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageActionEntry
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageMatchType
import foo.starred.athen.modules.impl.general.messageactions.actions.data.MessageResolvedEntry
import foo.starred.athen.modules.impl.general.messageactions.actions.impl.NoAction
import foo.starred.athen.modules.impl.general.messageactions.data.MessageActionsData
import foo.starred.athen.modules.impl.general.messageactions.ui.data.MessageActionsCategoryEntry
import foo.starred.athen.modules.impl.general.messageactions.ui.impl.MessageActionsGUI
import foo.starred.athen.utils.command
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.api.format.extension.component.colored
import foo.starred.kontext.api.storage.impl.klass.KlassJsonStore
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.lang.reflect.Type

@LoadableAthenModule
object MessageActions : Module(
    "Message actions",
    "Allows you to run actions when you receive a message.",
    ConfigCategory.GENERAL
) {
    private val _unused by config.button("Open manager") { MessageActionsGUI.open() }
    private val _unused0 by config.information("Share command: <red>/athen [import|export] messageactions")

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
        .registerTypeAdapter(MessageActionsData::class.java, JsonDeserializer { json, _, context ->
            fun array(element: JsonElement?): JsonArray? {
                if (element == null) return null
                if (element.isJsonArray) return element.asJsonArray
                if (!element.isJsonPrimitive) return null
                if (!element.asJsonPrimitive.isString) return null

                return runCatching { JsonParser.parseString(element.asString).asJsonArray }.getOrNull()
            }

            val root = json.asJsonObject
            val data = root.getAsJsonObject("@${Athen.Data.id}:data") ?: root
            val actions = ObjectArrayList<MessageActionEntry>()
            val categories = ObjectArrayList<MessageActionsCategoryEntry>()

            array(data.get("actions"))?.forEach {
                actions.add(context.deserialize(it, MessageActionEntry::class.java))
            }

            array(data.get("categories"))?.forEach {
                categories.add(context.deserialize(it, MessageActionsCategoryEntry::class.java))
            }

            MessageActionsData(actions, categories)
        })
        .create()

    private var set = emptySet<String>()
    private var cancel = emptyArray<MessageResolvedEntry>()
    private var normal = emptyArray<MessageResolvedEntry>()

    val store = KlassJsonStore(Athen.Data.id, "features/MessageActions", MessageActionsData::class.java, ::MessageActionsData, gson)

    init {
        fn()

        on<GameEvent.Stop> {
            disk()
        }

        on<MessageEvent.Chat.Intercept> {
            var text: String? = null

            for (entry in cancel) {
                if (!entry.matches(stripped, set)) continue
                cancel()

                if (entry.action == NoAction) return@on
                val action = entry.action(text ?: message.colored().also { text = it })
                if (entry.source.delay > 0.0) Scheduler.schedule(entry.delay) { action.run() } else action.run()
            }
        }

        on<MessageEvent.Chat.Receive> {
            var text: String? = null

            for (entry in normal) {
                if (!entry.matches(stripped, set)) continue

                val action = entry.action(text ?: message.colored().also { text = it })
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
                MinecraftClient.clipboard = store.export()
                "Exported ${store.data.actions.size} actions to clipboard!".mod()
            }

            "import" / "messageactions" {
                val a = MinecraftClient.clipboard
                if (a.isEmpty()) return@invoke "No data found in clipboard!".mod()

                if (!store.import(a)) return@invoke "Invalid config data!".mod()

                fn()
                "Imported ${store.data.actions.size} actions and ${store.data.categories.size} categories!".mod()
            }
        }
    }

    fun fn() {
        set = HashSet<String>(store.data.categories.size).also { set ->
            for ((name, enabled1) in store.data.categories) {
                if (!enabled1) set.add(name)
            }
        }

        val a = ObjectArrayList<MessageResolvedEntry>()
        val b = ObjectArrayList<MessageResolvedEntry>()
        for (a0 in store.data.actions) {
            val r = MessageResolvedEntry(a0)
            if (a0.cancel) a.add(r) else if (a0.action != NoAction) b.add(r)
        }

        cancel = a.toArray(emptyArray())
        normal = b.toArray(emptyArray())
    }

    fun disk() {
        store.save()
    }

    fun add(pattern: String, match: MessageMatchType, action: IMessageAction, category: String, cancel: Boolean, delay: Double): Boolean {
        if (pattern.isBlank()) return false
        store.data.actions.add(MessageActionEntry(pattern, match, action, true, category, cancel, delay))
        fn()
        return true
    }

    fun remove(index: Int): Boolean {
        if (index !in store.data.actions.indices) return false
        store.data.actions.removeAt(index)
        fn()
        return true
    }

    fun update(index: Int, entry: MessageActionEntry): Boolean {
        if (index !in store.data.actions.indices) return false
        if (entry.pattern.isBlank()) return false

        store.data.actions[index] = entry
        fn()
        return true
    }

    fun add(name: String): Boolean {
        if (name.isBlank()) return false
        if (store.data.categories.any { it.name == name }) return false

        store.data.categories.add(MessageActionsCategoryEntry(name))
        fn()
        return true
    }

    fun remove(name: String) {
        store.data.categories.removeIf { it.name == name }

        for (i in store.data.actions.indices) {
            val a = store.data.actions[i].takeIf { it.category == name } ?: continue
            store.data.actions[i] = a.copy(category = "")
        }

        fn()
    }

    fun toggle(name: String) {
        for (i in store.data.categories.indices) {
            val c = store.data.categories[i].takeIf { it.name == name } ?: continue
            store.data.categories[i] = c.copy(enabled = !c.enabled)
            break
        }

        fn()
    }
}
