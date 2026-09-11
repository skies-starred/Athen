@file:Suppress("UNCHECKED_CAST")

package foo.starred.athen.api.skyblock

import com.google.gson.JsonObject
import foo.starred.athen.annotations.Priority
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.modules.impl.ModSettings
import foo.starred.athen.utils.api
import foo.starred.athen.utils.id
import foo.starred.snowbird.api.scheduling.scheduler.data.tasks.base.SchedulerTask
import foo.starred.snowbird.utils.asJsonObjectOrNull
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration.Companion.minutes

@Priority
object PriceAPI {
    private val auctions = Int2ObjectOpenHashMap<Auction>(8192)
    private val bazaar = Int2ObjectOpenHashMap<Bazaar>(2048)

    private var task: SchedulerTask? = null

    init {
        fn()

        task = Scheduler.repeat(ModSettings.priceFetch.value.minutes) { fn() }
        ModSettings.priceFetch.state.onChange {
            task?.cancel()
            task = Scheduler.repeat(it.minutes) { fn() }
        }
    }

    fun ItemStack.price(): Price? =
        id()?.price()

    fun String.price(): Price? {
        val hash = hashCode()

        val a = auctions[hash]
        val b = bazaar[hash]

        if (a == null && b == null) return null
        return Price(a, b)
    }

    private fun fn() {
        "prices".api.request(log = false) {
            success<JsonObject> {
                val ah = it["auction_house"].asJsonObjectOrNull ?: return@success
                val bz = it["bazaar"].asJsonObjectOrNull ?: return@success

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
