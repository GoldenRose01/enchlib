@file:Suppress("UNCHECKED_CAST")

package goldenrose01.enchlib.compat

import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.registry.RegistryKeys
import java.lang.reflect.Method
import java.util.Optional

object MCCompat {

    // ---------- riflessione di base ----------
    private fun findMethod(
        target: Any,
        name: String,
        paramCount: Int? = null
    ): Method? = target.javaClass.methods.firstOrNull {
        it.name == name && (paramCount == null || it.parameterCount == paramCount)
    }

    private fun findMethodAssignable(target: Any, name: String, vararg args: Any): Method? {
        val argTypes = args.map { it.javaClass }
        return target.javaClass.methods.firstOrNull { m ->
            if (m.name != name || m.parameterCount != args.size) return@firstOrNull false
            m.parameterTypes.zip(argTypes).all { (p, a) -> p.isAssignableFrom(a) }
        }
    }

    private fun optionalOrNull(v: Any?): Any? =
        if (v is Optional<*>) v.orElse(null) else v

    // ---------- Registry ENCHANTMENT ----------
    private fun getEnchRegistry(server: MinecraftServer): Any? {
        val rm = server.registryManager
        // get(RegistryKey) | getOrThrow(RegistryKey)
        val m = findMethodAssignable(rm, "get", RegistryKeys.ENCHANTMENT)
            ?: findMethodAssignable(rm, "getOrThrow", RegistryKeys.ENCHANTMENT)
        return m?.invoke(rm, RegistryKeys.ENCHANTMENT)
    }

    fun listEnchantmentIds(server: MinecraftServer): List<Identifier> {
        return try {
            val reg = getEnchRegistry(server) ?: return emptyList()

            // prefer getIds()
            findMethod(reg, "getIds", 0)?.let { m ->
                val raw = m.invoke(reg) as? Iterable<*> ?: return emptyList()
                return raw.mapNotNull { it as? Identifier }
            }

            // fallback ids()
            findMethod(reg, "ids", 0)?.let { m ->
                val raw = m.invoke(reg) as? Iterable<*> ?: return emptyList()
                return raw.mapNotNull { it as? Identifier }
            }

            // fallback iterator + getId(T)
            val itM = findMethod(reg, "iterator", 0) ?: return emptyList()
            val it = itM.invoke(reg) as? java.util.Iterator<*> ?: return emptyList()
            val getIdM = findMethod(reg, "getId", 1) ?: return emptyList()
            val out = ArrayList<Identifier>()
            while (it.hasNext()) {
                val e = it.next()
                val rid = getIdM.invoke(reg, e)
                when (rid) {
                    is Identifier -> out.add(rid)
                    else -> rid?.toString()?.let { s -> Identifier.tryParse(s)?.let(out::add) }
                }
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun getEnchantment(server: MinecraftServer, id: Identifier): Enchantment? {
        return try {
            val reg = getEnchRegistry(server) ?: return null

            // get(Identifier)
            reg.javaClass.methods.firstOrNull {
                it.name == "get" && it.parameterCount == 1 && it.parameterTypes[0] == Identifier::class.java
            }?.let { m ->
                (m.invoke(reg, id) as? Enchantment)?.let { return it }
            }

            // fallback iterator + match id
            val itM = findMethod(reg, "iterator", 0) ?: return null
            val it = itM.invoke(reg) as? java.util.Iterator<*> ?: return null
            val getIdM = findMethod(reg, "getId", 1) ?: return null
            while (it.hasNext()) {
                val e = it.next() as? Enchantment ?: continue
                val rid = getIdM.invoke(reg, e)
                val s = when (rid) {
                    is Identifier -> rid.toString()
                    else -> rid?.toString()
                }
                if (s == id.toString()) return e
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    // ---------- NBT helpers (solo riflessione: niente firme generiche dirette) ----------
    fun getOrCreateNbt(stack: ItemStack): NbtCompound {
        // getOrCreateNbt()
        try {
            val m = stack.javaClass.getMethod("getOrCreateNbt")
            (m.invoke(stack) as? NbtCompound)?.let { return it }
        } catch (_: Throwable) {}

        // getNbt()
        getNbtOrNull(stack)?.let { return it }

        // crea e setta
        val created = NbtCompound()
        setNbt(stack, created)
        return created
    }

    fun getNbtOrNull(stack: ItemStack): NbtCompound? {
        return try {
            val m = stack.javaClass.methods.firstOrNull { it.name == "getNbt" && it.parameterCount == 0 }
                ?: return null
            val r = m.invoke(stack)
            r as? NbtCompound
        } catch (_: Throwable) { null }
    }

    fun setNbt(stack: ItemStack, nbt: NbtCompound) {
        try {
            val m = stack.javaClass.methods.firstOrNull {
                it.name == "setNbt" && it.parameterCount == 1
            } ?: return
            m.invoke(stack, nbt)
        } catch (_: Throwable) {
            // alcune versioni non richiedono set esplicito
        }
    }

    /** getList(String): Optional<NbtList> (nuovo)  |  getList(String,int): NbtList (vecchio) */
    fun getOrCreateList(nbt: NbtCompound, key: String): NbtList {
        // Optional<NbtList>
        try {
            val m = nbt.javaClass.methods.firstOrNull {
                it.name == "getList" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            }
            if (m != null) {
                val ret = optionalOrNull(m.invoke(nbt, key))
                if (ret is NbtList) return ret
            }
        } catch (_: Throwable) {}

        // getList(String,int)
        try {
            val m = nbt.javaClass.methods.firstOrNull {
                it.name == "getList" && it.parameterCount == 2 && it.parameterTypes[0] == String::class.java
            }
            if (m != null) {
                val ret = m.invoke(nbt, key, 10 /* TAG_Compound */)
                if (ret is NbtList) return ret
            }
        } catch (_: Throwable) {}

        return NbtList()
    }

    fun getString(cmp: NbtCompound, key: String): String {
        return try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "getString" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            } ?: return ""
            when (val r = m.invoke(cmp, key)) {
                is Optional<*> -> (r.orElse("") as? String) ?: ""
                is String -> r
                else -> ""
            }
        } catch (_: Throwable) { "" }
    }

    fun putString(cmp: NbtCompound, key: String, value: String) {
        // putString(String,String)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "putString" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java &&
                        it.parameterTypes[1] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, value); return }
        } catch (_: Throwable) {}

        // setString(String,String)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "setString" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java &&
                        it.parameterTypes[1] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, value); return }
        } catch (_: Throwable) {}
    }

    fun putShort(cmp: NbtCompound, key: String, value: Short) {
        // putShort(String,short)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "putShort" && it.parameterCount == 2 && it.parameterTypes[0] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, value); return }
        } catch (_: Throwable) {}

        // setShort(String,short)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "setShort" && it.parameterCount == 2 && it.parameterTypes[0] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, value); return }
        } catch (_: Throwable) {}
    }

    fun listSize(list: NbtList): Int {
        return try {
            val m = list.javaClass.methods.firstOrNull { it.name.equals("size", true) && it.parameterCount == 0 }
            (m?.invoke(list) as? Int) ?: list.size
        } catch (_: Throwable) {
            list.size
        }
    }

    fun listGetCompound(list: NbtList, index: Int): NbtCompound? {
        // getCompound(int)
        try {
            val m = list.javaClass.methods.firstOrNull {
                it.name.equals("getCompound", true) && it.parameterCount == 1
            }
            val r = m?.invoke(list, index)
            if (r is NbtCompound) return r
        } catch (_: Throwable) { }

        // get(int) -> NbtElement -> asCompound()
        try {
            val mGet = list.javaClass.methods.firstOrNull { it.name == "get" && it.parameterCount == 1 }
            val el = mGet?.invoke(list, index) ?: return null
            val asCmp = el.javaClass.methods.firstOrNull { it.name.equals("asCompound", true) && it.parameterCount == 0 }
            val r = asCmp?.invoke(el)
            if (r is NbtCompound) return r
        } catch (_: Throwable) {}

        return null
    }

    /** Sempre e solo riflessione: niente invocazione diretta a put(String, NbtElement). */
    fun nbtPut(cmp: NbtCompound, key: String, element: Any) {
        // put(String, NbtElement)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "put" && it.parameterCount == 2 && it.parameterTypes[0] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, element); return }
        } catch (_: Throwable) {}

        // set(String, NbtElement) – in caso esista
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "set" && it.parameterCount == 2 && it.parameterTypes[0] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, element); return }
        } catch (_: Throwable) {}
        // se non troviamo nulla… pazienza: molte versioni aggiornano il reference della lista direttamente
    }

    // ---------- utilities ----------
    fun suggestStringsForEnchantments(server: MinecraftServer): List<String> {
        val ids = listEnchantmentIds(server)
        val out = ArrayList<String>(ids.size * 2)
        for (id in ids) {
            out.add(id.toString())
            // id.getPath() | getPath()
            val mPath = id.javaClass.methods.firstOrNull { it.name == "getPath" && it.parameterCount == 0 }
            val path = try { mPath?.invoke(id) as? String } catch (_: Throwable) { null }
            if (path != null) out.add(path)
        }
        return out
    }

    fun storedOrRegularKey(stack: ItemStack): String =
        if (stack.item == Items.ENCHANTED_BOOK) "StoredEnchantments" else "Enchantments"
}
