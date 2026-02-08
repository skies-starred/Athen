package xyz.aerii.athen.events.core

abstract class Event {
    var isCancelled = false
        private set

    fun post(): Boolean {
        EventBus.post(this)
        return isCancelled
    }

    interface Cancellable {
        fun cancel() {
            (this as Event).isCancelled = true
        }
    }
}

abstract class CancellableEvent : Event(), Event.Cancellable