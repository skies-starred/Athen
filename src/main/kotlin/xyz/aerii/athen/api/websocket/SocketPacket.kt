package xyz.aerii.athen.api.websocket

object SocketPacket {
    sealed class WebSocket {
        enum class ServerBound(val id: Int) {
            Auth(100)
        }

        enum class ClientBound(val id: Int) {
            AuthSuccess(500),
            AuthError(501),

            Error(502),
            Warn(503)
        }
    }

    sealed class IRC {
        enum class ServerBound(val id: Int) {
            Create(101),
            Join(102),
            Pin(103),
            Chat(104),
            Leave(105),
            List(106)
        }

        enum class ClientBound(val id: Int) {
            Join(504),
            Left(505),
            Chat(506),
            Error(507),
            Warn(508),
            List(509);

            companion object {
                val all: Set<Int> = entries.map { it.id }.toSet()
            }
        }
    }
}