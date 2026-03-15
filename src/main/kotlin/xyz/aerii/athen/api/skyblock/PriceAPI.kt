@file:Suppress("UNCHECKED_CAST")

package xyz.aerii.athen.api.skyblock

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Chronos
import kotlin.time.Duration.Companion.minutes

@Priority
object PriceAPI {
    private val lbin = Long2LongOpenHashMap(8192).apply { defaultReturnValue(-1) }
    private val bazaar = Int2ObjectOpenHashMap<Bazaar>(2048)

    init {
        fn()
        Chronos.Time every 5.minutes repeat ::fn
    }

    fun ItemStack.price(): Price? =
        getData(DataTypes.SKYBLOCK_ID)?.skyblockId?.price()

    fun String.price(): Price? {
        val hash = hashCode()

        val a = lbin[hash.toLong()].takeIf { it != -1L }
        val b = bazaar[hash]

        if (a == null && b == null) return null
        return Price(a, b)
    }

    private fun fn() {
        Beacon.get("https://lb.tricked.dev/lowestbins", false) {
            onSuccess<Map<String, Long>> {
                lbin.clear()
                for ((k, v) in it) lbin[k.hashCode().toLong()] = v
            }
        }

        Beacon.get("https://api.hypixel.net/skyblock/bazaar", false) {
            onSuccess<Map<String, Any>> {
                val products = it["products"] as? Map<String, Any> ?: return@onSuccess
                bazaar.clear()

                for ((k, v) in products) {
                    val a = (v as? Map<String, Any>)?.get("quick_status") as? Map<String, Any> ?: continue

                    val buy = (a["buyPrice"] as? Number)?.toInt() ?: continue
                    val sell = (a["sellPrice"] as? Number)?.toInt() ?: continue

                    bazaar[k.hashCode()] = Bazaar(buy, sell)
                }
            }
        }
    }

    data class Bazaar(val buy: Int, val sell: Int)
    data class Price(val lbin: Long?, val bazaar: Bazaar?)
}