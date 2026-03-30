@file:Suppress("UNCHECKED_CAST")

package xyz.aerii.athen.api.skyblock

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.modules.impl.ModSettings
import xyz.aerii.athen.utils.api
import xyz.aerii.athen.utils.asJsonObjectOrNull
import kotlin.time.Duration.Companion.minutes

@Priority
object PriceAPI {
    private val auctions = Int2ObjectOpenHashMap<Auction>(8192)
    private val bazaar = Int2ObjectOpenHashMap<Bazaar>(2048)
    private val url = "prices?gzip=true".api

    private var task: Chronos.Task? = null

    init {
        fn()

        task = Chronos.Time every ModSettings.priceFetch.value.minutes repeat ::fn
        ModSettings.priceFetch.state.onChange {
            task?.cancel()
            task = Chronos.Time every it.minutes repeat ::fn
        }
    }

    fun ItemStack.price(): Price? =
        getData(DataTypes.SKYBLOCK_ID)?.skyblockId?.price()

    fun String.price(): Price? {
        val hash = hashCode()

        val a = auctions[hash]
        val b = bazaar[hash]

        if (a == null && b == null) return null
        return Price(a, b)
    }

    private fun fn() {
        Beacon.get(url, false) {
            onJsonSuccess {
                val ah = it["auction_house"].asJsonObjectOrNull ?: return@onJsonSuccess
                val bz = it["bazaar"].asJsonObjectOrNull ?: return@onJsonSuccess

                auctions.clear()
                for ((k, v) in ah.entrySet()) {
                    val a = v.asJsonObject
                    auctions[k.hashCode()] = Auction(a["lbin"].asLong, a["p3d"].asLong, a["p7d"].asLong)
                }

                bazaar.clear()
                for ((k, v) in bz.entrySet()) {
                    val a = v.asJsonObject
                    bazaar[k.hashCode()] = Bazaar(a["ib"].asNumber.toInt(), a["is"].asNumber.toInt(), a["tb"].asNumber.toInt(), a["ts"].asNumber.toInt())
                }
            }
        }
    }

    data class Auction(val lbin: Long, val p3d: Long, val p7d: Long)
    data class Bazaar(val buy: Int, val sell: Int, val bo: Int, val so: Int)
    data class Price(val auction: Auction?, val bazaar: Bazaar?)
}