# EnchLib – Libreria per Incantesimi Minecraft

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.17.2%2B-blue)
![Java Version](https://img.shields.io/badge/Java-22-orange)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)

EnchLib è una libreria Fabric che offre un sistema **completo** e **configurabile** per la gestione degli incantesimi (vanilla e di altre mod), con comandi server-side e config **per-mondo**.

```text
Up-to-date:
Questa documentazione riflette lo stato corrente dopo l’integrazione di:
- nuovo comando /plusec (add/remove/list) che applica realmente NBT all’oggetto
- suggerimenti dinamici (autocomplete) per gli ID incantesimo
- layer di compatibilità riflessiva (MCCompat) per Yarn/Fabric 1.21.8
```

## 🌟 Novità principali (questa build)

* **/plusec “senza protezioni”**: `add` replica l’effetto di `/enchant` **senza** i check di compatibilità di Minecraft.
* **Autocomplete robusto**: suggerisce sia `namespace:id` che il solo `id` (es. `sharpness` → `minecraft:sharpness`) leggendo **il registry runtime** (anche incantesimi di altre mod).
* **Applicazione NBT affidabile**: supporto sia a `Enchantments` che a `StoredEnchantments` (libro incantato).
* **Compat layer (MCCompat)**: astrae accesso a Registry/Identifier/NBT (metodi come `getOrCreateNbt`, `getOrCreateList`, `listAdd`, `nbtPut`, ecc.) per evitare rotture con cambi mapping / firme nuove.

## 📋 Requisiti

* **Minecraft**: 1.21.8
* **Fabric Loader**: 0.17.2+
* **Fabric API**: compatibile con 1.21.8
* **Java**: 22
* **Fabric Kotlin**: abilitato

## 🚀 Installazione rapida

```bash
git clone https://github.com/GoldenRose01/enchlib.git
cd enchlib
./gradlew build
```

Metti il `.jar` in `mods/`. Avvia un mondo/server: verranno create le cartelle di config **nel mondo**.

## 📁 Struttura del progetto

```
enchlib/
├─ src/main/kotlin/goldenrose01/enchlib/
│  ├─ EnchLib.kt                # Mod initializer
│  ├─ commands/
│  │  └─ EnchLibCommands.kt     # /plusec add|remove|list (nuovo)
│  ├─ compat/
│  │  └─ MCCompat.kt            # Layer compat riflessivo (nuovo/esteso)
│  ├─ config/                   # gestione config per-mondo
│  └─ utils/                    # logger & helper
├─ src/main/resources/
│  ├─ config/                   # template base copiati nel mondo (nuovo flusso)
│  └─ assets|data|...           # risorse standard
└─ build.gradle / gradle.properties
```

## ⚙️ Config per-mondo

Percorso:

```
.minecraft/saves/<nome-mondo>/config/enchlib/
```

* All’avvio del **mondo**, EnchLib copia **dinamicamente** i file base da `src/main/resources/config/` se non esistono e/o li **autopopola** dal registry runtime.
* Formato preferito: **JSON5** (commenti supportati).
  Esempio minimale:

  ```json5
  {
    "enchantments": [
      { "id": "minecraft:sharpness", "level": 1 },
      { "id": "minecraft:unbreaking", "level": 1 }
    ]
  }
  ```
* File/concetti già previsti:

    * `AviableEnch.json5` – quali incantesimi sono abilitati
    * `EnchantmentsDetails.json5` – metadati (max level, categorie, rarità, ecc.)
    * `Uncompatibility.json5` – incompatibilità tra incantesimi
    * (Migration dalle vecchie `.config` globali in corso: i template vengono presi da **resources/config**)

> Nota: i comandi lavorano **sull’oggetto in mano**; i file JSON5 regolano la policy globale/per-mondo.

## 🎮 Comandi

### `/plusec`

Gestione diretta, **applica realmente** NBT sull’oggetto in mano:

```
/plusec add <enchantment_id> <level>
/plusec remove <enchantment_id>
/plusec list
```

**Esempi**

```
/plusec add minecraft:unbreaking 3
/plusec add sharpness 5                  # namespace implicito (=minecraft)
/plusec remove minecraft:looting
/plusec list
```

Caratteristiche:

* **Autocomplete**: propone sia `namespace:id` che `id` (anche da altre mod).
* **Item detection**: se l’oggetto in mano è un **libro incantato**, usa `StoredEnchantments`; altrimenti `Enchantments`.
* **No “protezioni”**: non esegue i check vanilla di compatibilità; applica ciò che chiedi.

Messaggi d’errore comuni:

* “Devi essere un giocatore…” → comando eseguito dalla console senza target player.
* “Tieni un oggetto nella mano principale.” → mano vuota.
* “ID incantesimo non valido…” → ID malformato o inesistente nel registry corrente.

> `/plusec-debug` e altri comandi amministrativi/diagnostici restano disponibili se già presenti nel tuo build; non sono stati modificati in questa iterazione.

## 🧩 MCCompat (nuovo layer di compatibilità)

Per rendere stabile la mod su 1.21.8 (mappings/firmware aggiornati) **senza** dipendere da costrutti instabili (es. `Registries.ENCHANTMENT`), è stato introdotto `compat/MCCompat.kt`, che fornisce:

* **Registry (riflessivo)**:

    * `listEnchantmentIds(server): List<Identifier>`
    * `getEnchantment(server, id: Identifier): Enchantment?`
    * `parseEnchantmentId(input: String): Identifier?` (`sharpness` → `minecraft:sharpness`)
* **NBT helpers** (tolleranti a firme/metodi differenti):

    * `getOrCreateNbt(stack)`, `getNbtOrNull(stack)`, `setNbt(stack, nbt)`
    * `getOrCreateList(nbt, key)`, `listAdd(list, element)`, `listGetCompound(list, i)`, `listSize(list)`
    * `nbtPut(cmp, key, element)`, `putString(cmp, key, value)`, `putShort(cmp, key, value)`
    * `storedOrRegularKey(stack)` → `"StoredEnchantments"` o `"Enchantments"`
* **Operazioni alto livello**:

    * `upsertEnchantment(stack, id, level): Boolean`
    * `removeEnchantment(stack, id): Boolean`
    * `readEnchantments(stack): List<Pair<String, Int>>`
* **Suggerimenti**:

    * `suggestStringsForEnchantments(server): List<String>` (namespace + id “short”)

Questo consente di:

* evitare crash dovuti a cambi di firma/metodo tra versioni,
* supportare sia oggetti normali sia libri incantati senza boilerplate,
* restare compatibili con il registry di **qualsiasi** mod caricata.

## 🛠️ Build & Dev

```bash
./gradlew genSources
./gradlew build clean
./gradlew runClient
```

* **Java 22**
* **Fabric Loom 1.11.8**
* **Kotlin** come linguaggio principale
* Niente serializzazione diretta di classi Mojang su disco: si usano DTO/JSON5

## 🐛 Troubleshooting

* **Il file config non si crea**
  Verifica che `resources/config/` contenga i template. All’avvio mondo EnchLib li copia in `saves/<world>/config/enchlib/`. Controlla `logs/latest.log`.
* **Autocomplete non propone ID attesi**
  Il registry viene letto **a runtime**: assicurati che la mod che aggiunge incantesimi sia caricata.
* **L’incantesimo non appare**
  Verifica di avere un item valido in mano. Su **libri incantati** viene usata la lista `StoredEnchantments`.

## 🔄 Roadmap (prossimi step)

* **Policy livelli iniziali**: `MAX` vs `ONE` in autopopolamento (configurabile).
* **Report validazione avanzata**: diff tra config e runtime, conflitti, merge regole.
* **Altri comandi di qualità**:

    * `clear` (rimuovi tutti gli incantesimi dall’item)
    * `info <id>` (ispezione dettagli)
    * opzioni per leggere/scrivere direttamente su file config via comando.

## 🗓️ Changelog sintetico

* **Questa build**

    * `/plusec add/remove/list` operativi **sull’oggetto in mano** (no protezioni).
    * **Autocomplete nativo** (vanilla + mod) via registry runtime.
    * **MCCompat**: strato di compatibilità riflessivo per Registry/NBT e utility di alto livello.
    * Flusso config per-mondo: copia template da `resources/config/` → `saves/<world>/config/enchlib/`.

## 📄 Licenza

Rilasciato sotto **CC0-1.0**.

## 🙏 Grazie

* Fabric & Yarn Teams
* Kotlin Team
* Community (feedback & testing)

---

*Developed with ❤️ by GoldenRose01 — Minecraft Fabric community*
