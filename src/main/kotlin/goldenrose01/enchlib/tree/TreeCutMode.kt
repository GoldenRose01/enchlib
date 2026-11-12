package goldenrose01.enchlib.tree

enum class TreeCutMode(val id: String) {
    OFF("off"),
    FELL("fell"),
    CHOP("chop");

    companion object {
        fun fromString(name: String): TreeCutMode =
            entries.firstOrNull { it.id.equals(name, ignoreCase = true) } ?: OFF
    }
}
