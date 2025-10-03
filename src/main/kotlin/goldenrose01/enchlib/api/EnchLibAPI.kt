package goldenrose01.enchlib.api

import goldenrose01.enchlib.config.GlobalConfigIO
import goldenrose01.enchlib.utils.EnchLogger

/**
 * EnchantLibAPI
 *
 * API minimale e stabile per accedere/modificare la configurazione **globale**
 * (cartella: ~/.minecraft/config/enchlib).
 *
 * - getEnabledEnchantments(): List<String>
 * - isEnabled(id): Boolean
 * - getMaxLevel(id): Int?
 * - setEnabled(id, enabled)
 * - setMaxLevel(id, level)   (level >= 1)
 * - save()                    (flush esplicito opzionale)
 *
 * Nota: le funzioni scrivono subito su disco per semplicità e atomicità.
 */
object EnchantLibAPI {

    /** Ritorna gli ID abilitati (ordinati). */
    fun getEnabledEnchantments(): List<String> =
        GlobalConfigIO.readAvailable()
            .filter { it.enabled }
            .map { it.id }
            .sorted()

    /** true se l'id è presente e abilitato. */
    fun isEnabled(id: String): Boolean =
        GlobalConfigIO.readAvailable().any { it.id == id && it.enabled }

    /** Max level configurato per l'id, se presente. */
    fun getMaxLevel(id: String): Int? =
        GlobalConfigIO.readDetails()
            .enchantments
            .firstOrNull { it.id == id }
            ?.max_level

    /** Abilita/disabilita un incantesimo in AviableEnch.json5 (upsert). */
    fun setEnabled(id: String, enabled: Boolean) {
        val list = GlobalConfigIO.readAvailable().toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) list[i] = GlobalConfigIO.AvailableEnch(id, enabled)
        else list += GlobalConfigIO.AvailableEnch(id, enabled)
        list.sortBy { it.id }
        GlobalConfigIO.writeAvailable(list)
        EnchLogger.info("EnchantLibAPI: setEnabled($id, $enabled)")
    }

    /** Imposta il max level (>=1) in EnchantmentsDetails.json5 (upsert). */
    fun setMaxLevel(id: String, level: Int) {
        val lvl = level.coerceAtLeast(1)
        val root = GlobalConfigIO.readDetails()
        val list = root.enchantments
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) list[i].max_level = lvl
        else list += GlobalConfigIO.EnchantmentDetails(id = id, max_level = lvl)
        list.sortBy { it.id }
        GlobalConfigIO.writeDetails(root)
        EnchLogger.info("EnchantLibAPI: setMaxLevel($id, $lvl)")
    }

    /** Scrive eventuali buffer (qui i metodi scrivono già; funzione lasciata per simmetria). */
    fun save() {
        // No-op: l’API scrive immediatamente
    }
}
