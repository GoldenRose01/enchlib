package goldenrose01.enchlib.registry

import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * Registro locale di tutti gli incantesimi vanilla. Popolato all'avvio e
 * interrogabile in modo efficiente.
 */
object EnchantmentRegistry {
    private val enchantments: MutableMap<Identifier, Enchantment> = mutableMapOf()

    /** Popola il registro con tutti gli incantesimi del gioco. */
    fun initialize() {
        enchantments.clear()
        for (enchant in Registries.ENCHANTMENT) {
            val id = Registries.ENCHANTMENT.getId(enchant)
            enchantments[id] = enchant
        }
    }

    /** Ritorna un incantesimo a partire dal suo Identifier. */
    fun get(id: Identifier): Enchantment? = enchantments[id]

    /** Ritorna una vista di sola lettura di tutti gli incantesimi. */
    fun all(): Map<Identifier, Enchantment> = enchantments.toMap()
}
