# EnchLib - Libreria dinamica per incantesimi Minecraft

![Minecraft Version](https://img.shields.io/badge/Minecraft-26.2-brightgreen)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.2+-blue)
![Java Version](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)

EnchLib è una libreria Fabric scritta in Kotlin che sostituisce gli incantesimi hardcoded con un sistema **file-based e runtime**. Gli enchantments possono essere abilitati, configurati e controllati interamente da file `.json5` generati nel mondo e tramite comandi dedicati.

## 🌟 Caratteristiche Principali

- **Config per mondo** – I file vengono creati in `saves/<mondo>/config/enchlib/` e possono essere versionati o modificati server-side.
- **JSON5 support** – I file di configurazione accettano commenti e trailing comma grazie a un parser dedicato.
- **Comandi potenti** – `/plusec` e `/plusec-debug` consentono di gestire incantesimi sugli item, diagnosticare problemi e aggiornare i file.
- **Sync dinamico** – Gli incantesimi vengono aggiunti ai file di configurazione solo quando utilizzati, evitando conflitti con mod che registrano enchant a runtime.
- **Compatibile con Fabric Kotlin** – Il progetto utilizza Kotlin, `kotlinx.serialization` e riflessione per lavorare con le API 26.2.

## 📋 Requisiti

- **Minecraft**: 26.2 o superiore
- **Fabric Loader**: 0.19.2 o superiore
- **Fabric API**: compatibile con 26.2
- **Java**: 25 o superiore
- **Fabric Kotlin**: richiesto

### Toolchain aggiornata

| Componente | Versione |
| --- | --- |
| Minecraft | `26.2` |
| Fabric Loader | `0.19.2` |
| Fabric API | `0.148.0+26.2` |
| Fabric Loom | `1.16-SNAPSHOT` (`net.fabricmc.fabric-loom`) |
| Fabric Language Kotlin | `1.13.11+kotlin.2.3.21` |
| Kotlin Gradle Plugin | `2.3.21` |
| kotlinx.serialization | `1.10.0` |
| kotlinx.coroutines | `1.10.2` |
| Gradle Wrapper | `9.4.0` |
| Java | `25` |

> Nota 26.2: Fabric usa il nuovo Loom non-remapping (`net.fabricmc.fabric-loom`) e non richiede più Yarn mappings nel build script.

## 🚀 Installazione Rapida

1. **Clona il repository**:
   ```bash
   git clone https://github.com/GoldenRose01/enchlib.git
   cd enchlib
   ```

2. **Compila la mod**:
   ```bash
   ./gradlew build
   ```

3. **Installa** il file `.jar` risultante nella cartella `mods` di Minecraft.

## 📁 Struttura del Progetto

```
enchlib/
├─ src/
│  ├─ main/
│  │  ├─ kotlin/goldenrose01/enchlib/
│  │  │  ├─ EnchLib.kt                 # Classe principale
│  │  │  ├─ commands/                  # Implementazione comandi /plusec
│  │  │  ├─ config/                    # Gestione JSON5 per incantesimi
│  │  │  ├─ registry/                  # Registrazioni runtime
│  │  │  └─ utils/                     # Helper e logger
│  │  ├─ resources/
│  │  │  ├─ assets/enchlib/            # Asset grafici e lingue
│  │  │  ├─ data/enchlib/              # Datapack e tag generati
│  │  │  └─ fabric.mod.json            # Metadata mod
│  └─ client/                          # Codice client-side dedicato
├─ build.gradle                        # Configurazione Gradle
└─ gradle.properties                   # Versioni dipendenze
```

## ⚙️ Configurazione JSON5

I file di configurazione vengono creati (se mancanti) nella cartella del mondo `saves/<mondo>/config/enchlib/`:

- `AvailableEnch.json5` – Abilita o disabilita gli incantesimi.
- `EnchantmentDetails.json5` – Definisce livelli massimi, rarity, categorie e moltiplicatori.
- `Uncompatibility.json5` – Regole di incompatibilità tra enchant.
- `Mob_category.json5` – Categorizzazione di mob per effetti mirati.

Tutti i file nascono con lo scheletro `{ "enchantments": [] }`. Gli incantesimi vengono aggiunti gradualmente quando vengono usati nei comandi o interrogati.

## 🎮 Comandi Principali

### `/plusec`

Gestione diretta degli incantesimi su un item:

- `add <enchantment> <level>` – Aggiunge l'incantesimo (usa l'autocomplete Brigadier).
- `addid <namespace:id> <level>` – Aggiunge l'incantesimo solo nei JSON.
- `remove <enchantment>` – Rimuove l'incantesimo selezionato.
- `clear` – Svuota tutti gli incantesimi dell'item corrente (incluse le liste dei libri incantati).
- `list` – Elenca gli incantesimi applicati sull'item.
- `info <enchantment>` – Mostra stato runtime, abilitazione e dettagli configurati.

### `/plusec-debug`

Strumenti diagnostici dedicati:

- `show-path` – Mostra il percorso dei file di configurazione globale.
- `reload` – Ricarica le configurazioni.
- `validate` – Confronta gli ID configurati con il registry runtime.
- `list-enabled` – Elenca gli incantesimi abilitati.
- `toggle <id> <true|false>` e `setmax <id> <livello>` – Scrivono i valori nei JSON5 globali.
- `config read <id>` – Legge abilitazione e livello massimo configurati.
- `config write enabled <id> <true|false>` – Scrive lo stato di abilitazione.
- `config write max-level <id> <livello>` – Scrive il livello massimo.

Le scritture vengono salvate subito in `AviableEnch.json5` o `EnchantmentsDetails.json5`.

## 🆕 Note di Rilascio

Le novità dell'ultima versione sono disponibili in [docs/releases/v1.0.1.md](docs/releases/v1.0.1.md).
