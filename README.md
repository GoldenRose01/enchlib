# EnchLib - Libreria dinamica per incantesimi Minecraft

![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen)
![EnchLib Version](https://img.shields.io/badge/EnchLib-1.5.0-blue)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.5+-blue)
![Java Version](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)

EnchLib è una libreria Fabric scritta in Kotlin che sostituisce gli incantesimi hardcoded con un sistema **file-based e runtime**. Gli enchantments possono essere abilitati, configurati e controllati interamente da file `.json5` generati nel mondo e tramite comandi dedicati.

## 🌟 Caratteristiche Principali

- **Config per mondo** – Impostazioni e preset vengono creati in `<cartella-mondo>/config/enchlib/`; ogni mondo mantiene i propri valori.
- **Client/server sincronizzati** – Il server possiede e invia la configurazione del mondo ai client con EnchLib; solo gli operatori di livello 2 o superiore possono inviare modifiche.
- **JSON5 support** – I file di configurazione accettano commenti e trailing comma grazie a un parser dedicato.
- **Comandi potenti** – `/plusec` e `/plusec-debug` consentono di gestire incantesimi sugli item, diagnosticare problemi e aggiornare i file.
- **Categorie mob estendibili** – Le categorie sono associate agli ID del registry e possono includere mob aggiunti da altre mod.
- **Sync dinamico** – Gli incantesimi vengono aggiunti ai file di configurazione solo quando utilizzati, evitando conflitti con mod che registrano enchant a runtime.
- **Compatibile con Fabric Kotlin** – Il progetto utilizza Kotlin, `kotlinx.serialization` e riflessione per lavorare con le API 26.3.

## 📋 Requisiti

- **Minecraft**: 26.3
- **Fabric Loader**: 0.19.5 o superiore
- **Fabric API**: compatibile con 26.3
- **Java**: 25 o superiore
- **Fabric Kotlin**: richiesto

### Toolchain aggiornata

| Componente | Versione |
| --- | --- |
| Minecraft | `26.3` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.161.0+26.3` |
| Fabric Loom | `1.17.21` (`net.fabricmc.fabric-loom`) |
| Fabric Language Kotlin | `1.13.11+kotlin.2.3.21` |
| Kotlin Gradle Plugin | `2.3.21` |
| kotlinx.serialization | `1.10.0` |
| kotlinx.coroutines | `1.10.2` |
| Gradle Wrapper | `9.6.0` |
| Java | `25` |

> Nota 26.3: Fabric usa il flusso Loom non-remapping (`net.fabricmc.fabric-loom`) e non richiede Yarn mappings nel build script.

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

Impostazioni e preset sono specifici del mondo e si trovano in `<cartella-mondo>/config/enchlib/` (ad esempio `saves/<mondo>/config/enchlib/` in singleplayer):

- `AviableEnch.json5` – Abilita o disabilita gli incantesimi in quel mondo.
- `EnchantmentsDetails.json5` – Definisce cap, rarità, categorie, incompatibilità e regole dei tavoli in quel mondo.
- `EnchLVLmax.config`, `EnchRarity.config`, `EnchCompatibility.config`, `EnchUNcompatibility.config` – Preset modificabili specifici del mondo; vengono copiati all'avvio e usati per i valori iniziali e i comandi di ripristino.
- `MobCategory.defaults.config` – Preset delle categorie mob da usare per ripristinare i valori iniziali.
- `MobCategory.json` – Associa categorie ai mob vanilla e modded tramite ID del registry; ha priorità sul formato `.config` storico.
- `MobCategory.config` – Configurazione categorie mob in formato properties, mantenuta per retrocompatibilità.

Per associare categorie ai mob, modifica `MobCategory.json` oppure `MobCategory.config` nella cartella di configurazione del mondo. Usa gli ID del registry per supportare sia Minecraft sia le altre mod:

```properties
minecraft:zombie=undead,hostile
othermod:crystal_beast=magical,boss
```

Il server carica le proprie impostazioni quando apre il mondo e le sincronizza ai client EnchLib collegati. L'interfaccia Mod Menu legge i valori ricevuti dal server; le modifiche vengono accettate soltanto dagli operatori con livello 2 o superiore. In singleplayer il proprietario del mondo può modificare normalmente. I comandi debug hanno lo stesso requisito operatore. Le assegnazioni sono disponibili al server e alle mod tramite `MobCategoryManager`.

Per usare Mod Menu e sincronizzare i valori, installa lo stesso JAR EnchLib sia sul server sia sui client che devono aprire l'interfaccia. In singleplayer basta l'installazione client, perché il server integrato usa la stessa istanza. In multiplayer la cartella `config/enchlib` del server è autorevole. Al primo avvio della 1.5.0, i vecchi file globali vengono migrati una sola volta nel primo mondo aperto; ogni altro mondo riceve una copia indipendente dei preset predefiniti.

Il catalogo editabile in Excel, l'esportatore JSON e le istruzioni per caricare i dati in un mondo sono in [`doc/`](doc/README.md). Con Mod Menu e Cloth Config installati, EnchLib offre schede con ricerca, sezioni richiudibili, slider per le probabilità e ripristino dei preset. Le modifiche vengono applicate con Save & Done; senza un mondo aperto la schermata è in sola lettura. Yet Another Config Lib resta disponibile come alternativa quando Cloth Config non è installato. La schermata YACL offre tre macro menu: disponibilità degli incantesimi, dettagli degli incantesimi e categorie dei mob. I dettagli contengono incompatibilità, rarità, livello massimo e categorie di oggetti; le modifiche sono salvate nella configurazione del mondo dal server. I mob sono elencati per ID e le categorie possono essere cambiate una voce alla volta. In multiplayer, soltanto gli operatori possono salvare modifiche agli enchantment o ai mob.

Le regole degli oggetti accettano gruppi, ID specifici e esclusioni. Le regole positive si uniscono; quelle con `!` escludono gli oggetti corrispondenti. `!all` limita l'incantesimo ai libri. Gli ID specifici si inseriscono nel campo dedicato, separati da virgola. Nei tavoli gestiti dall'integrazione Eldritch Surge la rarità applica un peso alla probabilità del tavolo: comune 100, non comune 60, rara 30, molto rara 5; la percentuale del tavolo viene moltiplicata per questo peso.

I file JSON5 vengono creati con valori validi e separati per disponibilità e dettagli. Gli enchantment registrati dalle mod vengono aggiunti alla configurazione quando EnchLib li importa o quando sono usati nei comandi.

## 🎮 Comandi Principali

### `/plusec`

Gestione diretta degli incantesimi su un item:

- `add <enchantment> <level>` – Aggiunge l'incantesimo (usa l'autocomplete Brigadier).
- `addid <namespace:id> <level>` – Aggiunge l'incantesimo solo nei JSON.
- `remove <enchantment>` – Rimuove l'incantesimo selezionato.
- `clear` – Svuota tutti gli incantesimi dell'item corrente (incluse le liste dei libri incantati).
- `list` – Elenca gli incantesimi applicati sull'item.
- `info <enchantment>` – Mostra stato runtime, abilitazione e dettagli configurati.
- `repair`, `setdurability <percentuale>` e `damage <+/-punti>` – Riparano o modificano la durabilità dell'oggetto tenuto.

### `/plusec-debug`

Strumenti diagnostici dedicati:

- `show-path` – Mostra il percorso dei file di configurazione del mondo attivo.
- `reload` – Ricarica le configurazioni.
- `validate` – Confronta gli ID configurati con il registry runtime.
- `list-enabled` – Elenca gli incantesimi abilitati.
- `toggle <id> <true|false>` e `setmax <id> <livello>` – Scrivono i valori nei JSON5 del mondo attivo.
- `config read <id>` – Legge abilitazione e livello massimo configurati.
- `config write enabled <id> <true|false>` – Scrive lo stato di abilitazione.
- `config write max-level <id> <livello>` – Scrive il livello massimo.
- `mob get <entity-id>` – Mostra le categorie assegnate a un mob.
- `mob set <entity-id> <cat1,cat2>` – Imposta categorie per un mob vanilla o di un'altra mod; usa `none` per rimuoverle.

Le scritture vengono salvate subito in `AviableEnch.json5` o `EnchantmentsDetails.json5`.

## 🆕 Note di Rilascio

Le novità dell'ultima versione sono disponibili nelle [note EnchLib 1.5.0](docs/releases/v1.5.0.md) e nella [matrice Minecraft 26.x](docs/releases/minecraft-26.3.md).

## Integrazione per altre mod

Per integrare una mod in modo modulare consulta la [guida API per sviluppatori](docs/API_INTEGRATION.md). EnchLib rileva gli enchantment presenti nel registry e consente di modificarne i metadati; l'API permette inoltre di fornire preset, collegare le regole dei tavoli e condividere categorie mob. Le modifiche alle regole runtime (cap, disponibilità e probabilità dei tavoli) hanno effetto nei sistemi che consultano l'API EnchLib. La guida mostra sia l'integrazione opzionale, sia le chiamate Java/Kotlin per chi vuole un comportamento coordinato come quello di Eldritch Surge.
