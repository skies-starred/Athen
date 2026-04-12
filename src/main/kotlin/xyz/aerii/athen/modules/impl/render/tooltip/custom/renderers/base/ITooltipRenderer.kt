package xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.base

interface ITooltipRenderer {
    fun TooltipContext.render()

    fun r(context: TooltipContext) {
        context.render()
    }
}