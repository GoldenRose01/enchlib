package goldenrose01.enchlib.registry

import net.minecraft.enchantment.Enchantment

/**
 * Registry locale “no-op” per 1.21.x: non interroga il registry di gioco,
 * ma fornisce API compatibili per codice esistente (/plusec-debug, ecc.).
 *
 * Nota: i comandi usano RegistryEntry + Data Components e NON dipendono
 * da questa cache per applicare o rimuovere incantesimi.
 */
object EnchantmentRegistry {
    // Contenitore locale (vuoto per compatibilità)
    private val enchantments: MutableMap<String, Enchantment> = mutableMapOf()

    /** Inizializzazione “no-op”: nessun accesso al registry runtime. */
    fun initialize() {
        enchantments.clear()
    }

    /** Restituisce l’incantesimo per ID (o null se non presente). */
    fun get(id: String): Enchantment? = enchantments[id]

    /** Copia immutabile degli incantesimi presenti nella cache locale. */
    fun all(): Map<String, Enchantment> = enchantments.toMap()
}
