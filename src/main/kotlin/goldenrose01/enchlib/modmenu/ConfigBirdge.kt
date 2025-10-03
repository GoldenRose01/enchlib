package goldenrose01.enchlib.modmenu

import goldenrose01.enchlib.config.GlobalConfigIO

/**
 * Ponte chiamato via riflessione dallo shim Java.
 * Nessun import client.
 */
object ConfigBridge {

    data class Snapshot(
        val available: MutableList<GlobalConfigIO.AvailableEnch>,
        val details: GlobalConfigIO.EnchantmentsDetailsRoot
    )

    fun loadSnapshot(): Snapshot {
        val ava = GlobalConfigIO.readAvailable().sortedBy { it.id }.toMutableList()
        val det = GlobalConfigIO.readDetails()

        val ids = ava.map { it.id }.toSet()
        val have = det.enchantments.map { it.id }.toSet()
        val missing = ids - have
        if (missing.isNotEmpty()) {
            det.enchantments.addAll(missing.map { id -> GlobalConfigIO.EnchantmentDetails(id = id) })
        }
        det.enchantments.sortBy { it.id }
        return Snapshot(ava, det)
    }

    fun saveSnapshot(snap: Snapshot) {
        GlobalConfigIO.writeAvailable(snap.available)
        GlobalConfigIO.writeDetails(snap.details)
    }

    fun parseCsv(input: String): MutableList<String> =
        input.split(',').mapNotNull { it.trim().ifEmpty { null } }.toMutableList()

    fun joinCsv(list: List<String>): String = list.joinToString(", ")
}
