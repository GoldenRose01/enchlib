package goldenrose01.enchlib.registry

// In Yarn 1.21 gli incantesimi si trovano nel package net.minecraft.enchantment,
// mentre il registry è in net.minecraft.registry e gli identificatori in net.minecraft.util.
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * Contiene una cache locale di tutti gli incantesimi vanilla. Permette di
 * recuperare rapidamente gli incantesimi senza interrogare ogni volta il
 * registry globale. È anche la base per eventuali incantesimi custom futuri.
 */
object EnchantmentRegistry {
    // Mappa che associa l’identificatore (stringa) all’istanza di Enchantment.
    // Usare stringhe rende il codice più portabile tra diverse versioni/mappings.
    private val enchantments: MutableMap<String, Enchantment> = mutableMapOf()

    /** Popola il registro locale con tutti gli incantesimi vanilla. */
    fun initialize() {
        enchantments.clear()
        // Registries.ENCHANTMENT implementa Iterable<Enchantment>. Facendo il cast
        // evitiamo l’ambiguità su iterator() causata da estensioni Kotlin.
        for (enchant in Registries.ENCHANTMENT as Iterable<Enchantment>) {
            val key: Identifier = Registries.ENCHANTMENT.getId(enchant)
            enchantments[key.toString()] = enchant
        }
    }

    /** Restituisce l’incantesimo associato all’ID (es. "minecraft:sharpness"), o null. */
    fun get(id: String): Enchantment? = enchantments[id]

    /** Restituisce una copia immutabile di tutti gli incantesimi registrati. */
    fun all(): Map<String, Enchantment> = enchantments.toMap()
}
