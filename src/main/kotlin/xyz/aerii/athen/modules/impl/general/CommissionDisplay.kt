@file:Suppress("Unused", "ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.util.FormattedCharSequence
import tech.thatgravyboat.skyblockapi.api.area.mining.Commission
import tech.thatgravyboat.skyblockapi.api.area.mining.CommissionArea
import tech.thatgravyboat.skyblockapi.api.area.mining.CommissionsAPI
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.handlers.Ticking
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.fcs
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.formatted

@Load
@OnlyIn(islands = [SkyBlockIsland.DWARVEN_MINES, SkyBlockIsland.MINESHAFT, SkyBlockIsland.CRYSTAL_HOLLOWS])
object CommissionDisplay : Module(
    "Commission display",
    "Displays your commissions without you having to open the tab menu!",
    Category.GENERAL
) {
    private val styleExpandable by config.expandable("Text styles")
    private val titleStyle = config.textInput("General title", "<red>Commissions:").childOf { styleExpandable }.custom("titleStyle")
    private val noneStyle = config.textInput("None available text", "<red>No commissions available!").childOf { styleExpandable }.custom("noneStyle")
    private val commissionStyle by config.textInput("Commission text", "<gray>- <r>#name: #progress").childOf { styleExpandable }
    private val `commissionStyle$perc` by config.switch("Colored percent", true).childOf { styleExpandable }
    private val unused by config.textParagraph("Variable: <red>#name<r>, <red>#progress").childOf { styleExpandable }

    private val ex0 = listOf("§cCommissions:", "§7- §fExample: §640%", "§7- §fExample: §e70%", "§7- §fExample: §c7%").fcs

    private var fcs0 = noneStyle.value.parse().visualOrderText
    private var fcs1 = titleStyle.value.parse().visualOrderText
    private val display = Ticking(20) {
        val area = CommissionArea.currentArea ?: return@Ticking listOf(fcs1, fcs0)
        val commissions = CommissionsAPI.commissions.filter { it.area == area }.takeIf { it.isNotEmpty() } ?: return@Ticking listOf(fcs1, fcs0)

        buildList {
            add(fcs1)
            for (c in commissions) add(c.prs())
        }
    }

    init {
        noneStyle.state.onChange { fcs0 = it.parse().visualOrderText }
        titleStyle.state.onChange { fcs1 = it.parse().visualOrderText }

        config.hud("Commission display") {
            if (it) return@hud sizedText(ex0)
            sizedText(display.value ?: return@hud null)
        }
    }

    private fun Commission.prs(): FormattedCharSequence {
        val p = (progress * 100f).coerceIn(0f, 100f)
        val c = when {
            !`commissionStyle$perc` -> ""
            p >= 100f -> "<${Mocha.Green.argb}>"
            p >= 75f -> "<${Mocha.Teal.argb}>"
            p >= 50f -> "<yellow>"
            p >= 25f -> "<gold>"
            else -> "<red>"
        }

        return commissionStyle
            .replace("#name", name)
            .replace("#progress", "$c${p.formatted()}%")
            .parse(true)
            .visualOrderText
    }
}