package xyz.aerii.athen.utils

import com.google.gson.JsonObject
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import xyz.aerii.athen.Athen
import xyz.aerii.athen.Athen.apiUrl
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Typo.stripped
import kotlin.io.encoding.Base64
import kotlin.jvm.optionals.getOrNull

data class Stack(
    val index: Int,
    val name: String?,
    val skyblockId: String?,
    val lore: List<String>?
)

data class ProfileResponse(
    val dungeons: DungeonsData?,
    val armorData: String?,
    val talismanBagData: String?,
    val abiphoneContacts: List<String>?,
    val consumedRiftPrism: Boolean?
)

data class DungeonsData(
    val catacombsLevel: Int?,
    val secrets: Int?,
    val totalRuns: Int?,
    val catacombs: CatacombsData?,
    val masterCatacombs: CatacombsData?
)

data class CatacombsData(
    val fastestTimeSPlus: Map<Int, Long>?
)

data class PlayerStats(
    var loading: Boolean = true,
    var catLevel: Int? = null,
    var secrets: Int? = null,
    var secretAvg: Double? = null,
    var normalPB: Map<Int, Long>? = null,
    var masterPB: Map<Int, Long>? = null,
    var armor: String? = null,
    var bagData: String? = null,
    var abiphoneContacts: Int = 0,
    var consumedPrism: Boolean = false
)

fun fetchPlayerStats(
    uuid: String,
    username: String,
    onSuccess: (PlayerStats) -> Unit,
    onError: ((Throwable) -> Unit)? = null
) {
    Beacon.get("$apiUrl/stats/$uuid", false) {
        onJsonSuccess { json ->
            onSuccess(json.parseProfile().toPlayerStats())
        }

        onError {
            Athen.LOGGER.error("Failed to fetch stats for $username", it)
            onError?.invoke(it) ?: onSuccess(PlayerStats(loading = false))
        }
    }
}

/**
 * Decodes a compressed Base64 string to create item data.
 */
fun String.parseItem(): List<Stack>? {
    if (isEmpty()) return null

    return runCatching {
        val nbtCompound = NbtIo.readCompressed(Base64.decode(this).inputStream(), NbtAccounter.unlimitedHeap())
        val nbtList = nbtCompound.getList("i").getOrNull() ?: return null

        nbtList.indices.mapNotNull { i ->
            val compound = nbtList.getCompound(i).getOrNull()?.takeIf { it.size() > 0 } ?: return@mapNotNull null

            val tag = compound.get("tag")?.asCompound()?.get()
            val display = tag?.get("display")?.asCompound()?.get()
            val skyblockId = tag?.get("ExtraAttributes")?.asCompound()?.get()?.get("id")?.asString()?.get()
            val name = display?.get("Name")?.asString()?.get()
            val lore = display?.get("Lore")?.asList()?.get()?.mapNotNull { it.asString().getOrNull() }
            Stack(i, name, skyblockId, lore)
        }
    }.getOrNull()
}

/**
 * Decodes a compressed Base64 string to calculate magical power
 */
fun String.calculateMP(abiphoneContacts: Int, consumedPrism: Boolean): Int? {
    if (isEmpty()) return null

    return runCatching {
        val nbtCompound = NbtIo.readCompressed(Base64.decode(this).inputStream(), NbtAccounter.unlimitedHeap())
        val itemList = nbtCompound.getList("i").getOrNull() ?: return null

        val mpMap = mutableMapOf<String, Int>()
        var hasAbicase = false

        for (i in itemList.indices) {
            val compound = itemList.getCompound(i).getOrNull()?.takeIf { it.size() > 0 } ?: continue

            val tag = compound.get("tag")?.asCompound()?.get()
            val extraAttr = tag?.get("ExtraAttributes")?.asCompound()?.get()
            val id = extraAttr?.get("id")?.asString()?.get() ?: continue
            val display = tag.get("display")?.asCompound()?.get()
            val lore = display?.get("Lore")?.asList()?.get()?.mapNotNull { it.asString().getOrNull() } ?: emptyList()

            if (lore.any { it.contains("§7§4☠ §cRequires") }) continue
            if (id == "ABICASE") hasAbicase = true

            val rarity = lore.lastOrNull()?.stripped()?.let {
                when {
                    it.contains("COMMON") -> 3
                    it.contains("UNCOMMON") -> 5
                    it.contains("RARE") -> 8
                    it.contains("EPIC") -> 12
                    it.contains("LEGENDARY") -> 16
                    it.contains("MYTHIC") -> 22
                    it.contains("SPECIAL") -> 3
                    it.contains("VERY SPECIAL") -> 5
                    else -> 0
                }
            } ?: 0

            val itemId = if (id.startsWith("PARTY_HAT_") || id.startsWith("BALLOON_HAT_")) "PARTY_HAT" else id
            val currentMP = mpMap[itemId] ?: 0
            if (rarity > currentMP) mpMap[itemId] = rarity
        }

        var totalMP = mpMap.values.sum()

        if (hasAbicase) totalMP += (abiphoneContacts / 2)

        if (consumedPrism) totalMP += 11

        totalMP
    }.getOrNull()
}

fun JsonObject.parseProfile(): ProfileResponse {
    val dungeons = getAsJsonObject("dungeons")?.let { d ->
        DungeonsData(
            catacombsLevel = d.get("catacombs_level")?.takeIf { !it.isJsonNull }?.asInt,
            secrets = d.get("secrets")?.takeIf { !it.isJsonNull }?.asInt,
            totalRuns = d.get("total_runs")?.takeIf { !it.isJsonNull }?.asInt,
            catacombs = d.getAsJsonObject("catacombs")?.let { c ->
                CatacombsData(fastestTimeSPlus = c.getAsJsonObject("fastest_time_s_plus")?.parsePB())
            },
            masterCatacombs = d.getAsJsonObject("master_catacombs")?.let { mc ->
                CatacombsData(fastestTimeSPlus = mc.getAsJsonObject("fastest_time_s_plus")?.parsePB())
            }
        )
    }

    return ProfileResponse(
        dungeons = dungeons,
        armorData = get("armor_data")?.takeIf { !it.isJsonNull }?.asString,
        talismanBagData = get("talisman_bag_data")?.takeIf { !it.isJsonNull }?.asString,
        abiphoneContacts = getAsJsonArray("abiphone_contacts")?.map { it.asString },
        consumedRiftPrism = get("consumed_rift_prism")?.takeIf { !it.isJsonNull }?.asBoolean
    )
}

fun JsonObject.parsePB(): Map<Int, Long> = buildMap {
    for ((k, v) in entrySet()) {
        k.toIntOrNull()?.let { put(it, v.asLong) }
    }
}

fun ProfileResponse.toPlayerStats(): PlayerStats {
    return PlayerStats(
        loading = false,
        catLevel = dungeons?.catacombsLevel,
        secrets = dungeons?.secrets,
        secretAvg = dungeons?.let {
            val secrets = it.secrets
            val runs = it.totalRuns
            if (secrets != null && runs != null && runs > 0) secrets.toDouble() / runs else null
        },
        normalPB = dungeons?.catacombs?.fastestTimeSPlus,
        masterPB = dungeons?.masterCatacombs?.fastestTimeSPlus,
        armor = armorData,
        bagData = talismanBagData,
        abiphoneContacts = abiphoneContacts?.size ?: 0,
        consumedPrism = consumedRiftPrism ?: false
    )
}