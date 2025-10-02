@file:Suppress("UNCHECKED_CAST")

package goldenrose01.enchlib.compat

import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.registry.RegistryKeys
import java.lang.reflect.Method
import java.util.Optional

object MCCompat {

    // ---------- riflessione util ----------
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
                val s = rid?.toString()
                if (s == id.toString()) return e
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    // ---------- NBT helpers ----------
    fun getOrCreateNbt(stack: ItemStack): NbtCompound {
        // getOrCreateNbt()
        try {
            val m = stack.javaClass.getMethod("getOrCreateNbt")
            (m.invoke(stack) as? NbtCompound)?.let { return it }
        } catch (_: Throwable) { }

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
            m.invoke(stack) as? NbtCompound
        } catch (_: Throwable) { null }
    }

    fun setNbt(stack: ItemStack, nbt: NbtCompound) {
        try {
            val m = stack.javaClass.methods.firstOrNull {
                it.name == "setNbt" && it.parameterCount == 1
            } ?: return
            m.invoke(stack, nbt as Any)
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
                is Optional<*> -> if (r.isPresent) (r.get() as? String) ?: "" else ""
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

        // NON usare put(String, NbtElement) con una String!
    }

    fun putShort(cmp: NbtCompound, key: String, value: Short) {
        // putShort(String,short)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "putShort" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java
            }
            if (m != null) { m.invoke(cmp, key, value); return }
        } catch (_: Throwable) {}

        // setShort(String,short)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "setShort" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java
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

    /** Inserisce un NbtCompound in una NbtList, gestendo firme diverse (add(e) | add(index,e)). */
    fun listAdd(list: NbtList, element: NbtCompound) {
        // add(E)
        try {
            val m = list.javaClass.methods.firstOrNull { it.name == "add" && it.parameterCount == 1 }
            if (m != null) { m.invoke(list, element as Any); return }
        } catch (_: Throwable) {}

        // add(int, E)
        try {
            val m = list.javaClass.methods.firstOrNull { it.name == "add" && it.parameterCount == 2 }
            if (m != null) {
                val size = listSize(list)
                m.invoke(list, size, element as Any)
                return
            }
        } catch (_: Throwable) {}
    }

    /**
     * Scrive un elemento NBT sotto chiave:
     * - tenta prima put(String, NbtList) / put(String, NbtCompound)
     * - poi put(String, NbtElement) (passando SEMPRE un NbtElement)
     * Mai passare String/Short qui: usa putString/putShort sopra.
     */
    fun nbtPut(cmp: NbtCompound, key: String, element: Any) {
        // se non è un NbtElement riconosciuto, non rischiare overload generici
        val elem = when (element) {
            is NbtList, is NbtCompound -> element
            is NbtElement -> element
            else -> return // ignoriamo: niente String/Short qui
        }

        // put(String, NbtList) / put(String, NbtCompound)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "put" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java &&
                        NbtElement::class.java.isAssignableFrom(it.parameterTypes[1])
            }
            if (m != null) { m.invoke(cmp, key, elem as Any); return }
        } catch (_: Throwable) {}

        // set(String, NbtElement)
        try {
            val m = cmp.javaClass.methods.firstOrNull {
                it.name == "set" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java &&
                        NbtElement::class.java.isAssignableFrom(it.parameterTypes[1])
            }
            if (m != null) { m.invoke(cmp, key, elem as Any); return }
        } catch (_: Throwable) {}
    }

    // ---------- utilities ----------
    fun suggestStringsForEnchantments(server: MinecraftServer): List<String> {
        val ids = listEnchantmentIds(server)
        val out = ArrayList<String>(ids.size * 2)
        for (id in ids) {
            out.add(id.toString())
            val mPath = id.javaClass.methods.firstOrNull { it.name == "getPath" && it.parameterCount == 0 }
            val path = try { mPath?.invoke(id) as? String } catch (_: Throwable) { null }
            if (path != null) out.add(path)
        }
        return out
    }

    fun storedOrRegularKey(stack: ItemStack): String =
        if (stack.item == Items.ENCHANTED_BOOK) "StoredEnchantments" else "Enchantments"


    /** Converte "ns:path" o "path" (aggiunge "minecraft:" se manca). Ritorna null se invalido. */
    fun parseEnchantmentId(input: String): Identifier? {
        val s = input.trim()
        val id = if (":" in s) s else "minecraft:$s"
        return Identifier.tryParse(id)
    }

    /** Determina la chiave NBT corretta per l'item (Libro: StoredEnchantments, altrimenti Enchantments). */
    fun resolveEnchantmentListKey(stack: ItemStack): String = storedOrRegularKey(stack)

    /** Restituisce tripletta (nbt, list, key) assicurando che la lista esista (creandola se necessario). */
    fun ensureEnchantmentsList(stack: ItemStack): Triple<NbtCompound, NbtList, String> {
        val key = resolveEnchantmentListKey(stack)
        val nbt = getOrCreateNbt(stack)
        var list = getOrCreateList(nbt, key)
        // se la lista non è ancora realmente collegata al compound, agganciala
        runCatching {
            // evitiamo overload sbagliati usando nbtPut (accetta solo NbtElement)
            nbtPut(nbt, key, list)
        }.onFailure { _ ->
            // estremo fallback: se la put non è andata, creiamo una nuova lista e ritentiamo
            list = NbtList()
            nbtPut(nbt, key, list)
        }
        return Triple(nbt, list, key)
    }

    /** Trova l'indice di un enchant nella lista confrontando la stringa 'id'. Ritorna -1 se non trovato. */
    fun findEnchantmentIndexById(list: NbtList, id: Identifier): Int {
        val target = id.toString()
        val size = listSize(list)
        for (i in 0 until size) {
            val cmp = listGetCompound(list, i) ?: continue
            val cur = getString(cmp, "id")
            if (cur == target) return i
        }
        return -1
    }

    /** Inserisce/aggiorna un enchantment nella lista dell'item. Restituisce true se ha modificato qualcosa. */
    fun upsertEnchantment(stack: ItemStack, id: Identifier, level: Int): Boolean {
        if (level <= 0) return false
        val (nbt, list, _) = ensureEnchantmentsList(stack)
        val idx = findEnchantmentIndexById(list, id)
        if (idx >= 0) {
            // aggiorna
            val cmp = listGetCompound(list, idx) ?: return false
            putShort(cmp, "lvl", level.toShort())
            // ri-aggancia nbt (alcune versioni non servono, ma è harmless)
            nbtPut(nbt, resolveEnchantmentListKey(stack), list)
            setNbt(stack, nbt)
            return true
        } else {
            // crea nuovo compound {id:"ns:path", lvl:S}
            val ench = NbtCompound()
            putString(ench, "id", id.toString())
            putShort(ench, "lvl", level.toShort())
            listAdd(list, ench)
            nbtPut(nbt, resolveEnchantmentListKey(stack), list)
            setNbt(stack, nbt)
            return true
        }
    }

    /** Rimuove un enchantment dalla lista dell'item. Restituisce true se lo ha rimosso. */
    fun removeEnchantment(stack: ItemStack, id: Identifier): Boolean {
        val (nbt, list, key) = ensureEnchantmentsList(stack)
        val idx = findEnchantmentIndexById(list, id)
        if (idx < 0) return false

        // rimozione compat: proviamo removeAt, poi riflessione generica
        runCatching {
            list.removeAt(idx)
        }.onFailure {
            // fallback generico: metodi 'remove(int)' o 'remove(Object)'
            val mIdx = list.javaClass.methods.firstOrNull { it.name == "remove" && it.parameterCount == 1 && it.parameterTypes[0] == Int::class.javaPrimitiveType }
            if (mIdx != null) {
                mIdx.invoke(list, idx)
            } else {
                val mGet = list.javaClass.methods.firstOrNull { it.name == "get" && it.parameterCount == 1 }
                val el = mGet?.invoke(list, idx)
                val mRemObj = list.javaClass.methods.firstOrNull { it.name == "remove" && it.parameterCount == 1 }
                if (el != null && mRemObj != null) {
                    mRemObj.invoke(list, el)
                }
            }
        }

        nbtPut(nbt, key, list)
        setNbt(stack, nbt)
        return true
    }

    /** Restituisce elenco (id,lvl) degli enchant presenti nell'item. */
    fun readEnchantments(stack: ItemStack): List<Pair<String, Int>> {
        val (_, list, _) = ensureEnchantmentsList(stack)
        val out = ArrayList<Pair<String, Int>>(listSize(list))
        val size = listSize(list)
        for (i in 0 until size) {
            val cmp = listGetCompound(list, i) ?: continue
            val id = getString(cmp, "id")
            // lvl può essere short → via riflessione: getShort(String) oppure get(String) as number
            var lvl = 0
            runCatching {
                val m = cmp.javaClass.methods.firstOrNull {
                    it.name == "getShort" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
                }
                if (m != null) {
                    lvl = (m.invoke(cmp, "lvl") as? Short)?.toInt() ?: 0
                } else {
                    val mGet = cmp.javaClass.methods.firstOrNull { it.name == "get" && it.parameterCount == 1 }
                    val any = mGet?.invoke(cmp, "lvl")
                    if (any is Number) lvl = any.toInt()
                }
            }
            if (id.isNotEmpty()) out.add(id to lvl)
        }
        return out
    }

}
