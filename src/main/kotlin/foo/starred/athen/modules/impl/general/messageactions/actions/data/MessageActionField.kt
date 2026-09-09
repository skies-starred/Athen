package foo.starred.athen.modules.impl.general.messageactions.actions.data

data class MessageActionField(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val defaultValue: String = "",
    val numeric: Boolean = false
)
