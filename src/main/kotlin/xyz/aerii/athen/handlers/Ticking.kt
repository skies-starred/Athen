package xyz.aerii.athen.handlers

import xyz.aerii.library.handlers.delegate.AbstractTickable

class Ticking<T>(ticks: Int = 1, block: () -> T) : AbstractTickable<T>(ticks, { Chronos.ticks.client }, block)