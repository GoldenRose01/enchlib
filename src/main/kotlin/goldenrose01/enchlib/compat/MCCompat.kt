package goldenrose01.enchlib.compat

import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.ItemEnchantments

object MCCompat {

    private fun enchantmentRegistry(server: MinecraftServer): Registry<Enchantment> =
        server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)

    private fun enchantmentComponent(stack: ItemStack) =
        if (stack.item == Items.ENCHANTED_BOOK) DataComponents.STORED_ENCHANTMENTS else DataComponents.ENCHANTMENTS

    fun listEnchantmentIds(server: MinecraftServer): List<Identifier> =
        enchantmentRegistry(server).keySet().toList()

    fun getEnchantment(server: MinecraftServer, id: Identifier): Enchantment? =
        enchantmentRegistry(server).getOptional(id).orElse(null)

    fun suggestStringsForEnchantments(server: MinecraftServer): List<String> =
        listEnchantmentIds(server).flatMap { id -> listOf(id.toString(), id.path) }.distinct()

    fun storedOrRegularKey(stack: ItemStack): String =
        if (stack.item == Items.ENCHANTED_BOOK) "StoredEnchantments" else "Enchantments"

    fun parseEnchantmentId(input: String): Identifier? {
        val trimmed = input.trim()
        return Identifier.tryParse(if (":" in trimmed) trimmed else "minecraft:$trimmed")
    }

    fun resolveEnchantmentListKey(stack: ItemStack): String = storedOrRegularKey(stack)

    fun upsertEnchantment(stack: ItemStack, id: Identifier, level: Int, server: MinecraftServer): Boolean {
        if (level <= 0) return false
        val key = ResourceKey.create(Registries.ENCHANTMENT, id)
        val registry = enchantmentRegistry(server)
        val holder = registry.get(key).orElse(null) ?: return false
        val component = enchantmentComponent(stack)
        val mutable = ItemEnchantments.Mutable(stack.get(component) ?: ItemEnchantments.EMPTY)
        mutable.set(holder, level)
        stack.set(component, mutable.toImmutable())
        return true
    }

    fun removeEnchantment(stack: ItemStack, id: Identifier): Boolean {
        val component = enchantmentComponent(stack)
        val current = stack.get(component) ?: return false
        val mutable = ItemEnchantments.Mutable(current)
        mutable.removeIf { holder ->
            holder.unwrapKey().map { it.identifier() == id }.orElse(false)
        }
        val updated = mutable.toImmutable()
        if (updated == current) return false
        stack.set(component, updated)
        return true
    }

    fun readEnchantments(stack: ItemStack): List<Pair<String, Int>> {
        val current = stack.get(enchantmentComponent(stack)) ?: return emptyList()
        return current.keySet().mapNotNull { holder ->
            val key = holder.unwrapKey().orElse(null) ?: return@mapNotNull null
            key.identifier().toString() to current.getLevel(holder)
        }
    }
}
