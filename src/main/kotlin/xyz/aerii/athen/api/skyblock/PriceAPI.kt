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
import kotlin.time.Duration.Companion.minutes

@Priority
object PriceAPI {
    private val auctions = Int2ObjectOpenHashMap<Auction>(8192)
    private val bazaar = Int2ObjectOpenHashMap<Bazaar>(2048)

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
        Beacon.get("https://lb.tricked.dev/lowestbins", false) {
            onSuccess<Map<String, Long>> {
                auctions.clear()
                for ((k, v) in it) auctions[k.hashCode()] = Auction(v, 0, 0)
            }
        }

        Beacon.get("https://api.hypixel.net/skyblock/bazaar", false) {
            onSuccess<Map<String, Any>> {
                val products = it["products"] as? Map<String, Any> ?: return@onSuccess
                bazaar.clear()

                for ((k, v) in products) {
                    val a = (v as? Map<String, Any>)?.get("quick_status") as? Map<String, Any> ?: continue

                    val b = (a["buyPrice"] as? Number)?.toInt() ?: continue
                    val s = (a["sellPrice"] as? Number)?.toInt() ?: continue

                    bazaar[k.hashCode()] = Bazaar(b, s, 0, 0)
                }
            }
        }
    }

    data class Auction(val lbin: Long, val p3d: Long, val p7d: Long)
    data class Bazaar(val buy: Int, val sell: Int, val bo: Int, val so: Int)
    data class Price(val auction: Auction?, val bazaar: Bazaar?)
}