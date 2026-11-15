# 📜 Project Context — EnchLib

> Questo documento è la **fonte unica di verità e comprensione** del progetto **EnchLib**.
> È collocato nella cartella `project_context/` (completamente disaccoppiata dal sistema di build Gradle) per consentire a qualunque assistente o sviluppatore di comprendere immediatamente l'architettura della libreria, le API esposte per altre mod, la gestione della configurazione, le meccaniche integrate, le convenzioni di codice e le istruzioni di build e rilascio.

---

## 1. Panoramica del Progetto (Project Overview)

* **Nome Mod / Libreria:** EnchLib (`enchlib`)
* **Piattaforma e Mod Loader:** Fabric (Minecraft 26.3 e Minecraft 26.4 Snapshot 8)
* **Versione Mod Corrente:** `1.8.0`
* **Linguaggi:** Kotlin 2.3.21, Java 25 (`options.release = 25`)
* **Dipendenze Fondamentali:**
  * Fabric Loader (`0.19.5+`)
  * Fabric API (`0.161.0+26.3` / `0.166.2+26.4`)
  * Fabric Language Kotlin (`1.13.11+kotlin.2.3.21`)
  * `kotlinx.serialization` (JSON5 & JSON)
  * `kotlinx.coroutines`
* **Mod Esterne di Appoggio (`External_mod/`):**
  * `fabric-api-0.166.2+26.3.jar`
  * `modmenu-21.0.1.jar`
  * `cloth-config-fabric-26.3.159.jar`
  * `yet_another_config_lib_v3-3.9.7+26.3-fabric.jar` (YACL v3)
* **JAR di Rilascio (`jar/`):**
  * `enchlib-mc26.3-1.8.0.jar`
  * `enchlib-mc26.4-snapshot-8-1.8.0.jar`
  * *(e i relativi archivi sources)*

---

## 2. Architettura del Codice e Mappa dei Package

La libreria è organizzata in split source sets (`src/main` per il codice comune/server e `src/client` per le interfacce client):

### A. Package Comuni (`src/main/kotlin/goldenrose01/enchlib` e `src/main/java`)

| Package | Scopo e Classi Principali |
|---|---|
| `goldenrose01.enchlib` | Entry point comune `EnchLib.kt` (inizializzazione mod, registrazione comandi, listener del ciclo di vita del server, reload configurazione). |
| `goldenrose01.enchlib.api` | API pubbliche consumabili da altre mod (come Eldritch Surge):<br>• `EnchLibApi.kt`: interfaccia principale per verificare compatibilità, livelli massimi e moltiplicatori.<br>• `EnchantmentMetadata.kt`: modelli dati per rarità, livelli e slot.<br>• `MobCategoryApi.kt`: risoluzione della categoria dei mob (undead, void, arthropods, hell, rebel, fungi, ecc.). |
| `goldenrose01.enchlib.commands` | `EnchLibCommands.kt`: implementazione dei comandi `/plusec` e `/plusec-debug` per manipolare e ispezionare gli incantesimi sull'oggetto impugnato, ricaricare le configurazioni e verificare i registry a runtime. |
| `goldenrose01.enchlib.compat` | Adapter e classi di interoperabilità con mod terze e datapack. |
| `goldenrose01.enchlib.config` | Sistema di gestione configurazione file-based JSON5:<br>• Caricamento da template predefiniti (`src/main/resources/config/`).<br>• Gestione configurazioni per mondo in `<cartella-mondo>/config/enchlib/`.<br>• Serializzazione e parsing con supporto commenti e trailing comma. |
| `goldenrose01.enchlib.trading` | `VillagerEnchantmentRules.java` e `VillagerTradeMixin.java`: controllo e filtraggio degli incantesimi generati nelle offerte dei villici (esclusione automatica delle maledizioni `minecraft:curse`, whitelist/blacklist per libro o equipaggiamento). |
| `goldenrose01.enchlib.tree` | `TreeCutHandler.kt` e `TreeCutMode.kt`: sistema integrato di taglio alberi configurabile (`/gameset treecut <off|fell|chop>`). In modalità `fell` i tronchi cadono in sequenza; in `chop` l'intero albero viene abbattuto. Si integra con l'incantesimo Lumberjack di Eldritch Surge. |
| `goldenrose01.enchlib.weapons` | `AxesAreWeaponsConfig.kt`, `AxesAreWeaponsEnchantmentMixin.java`, `AxesAreWeaponsItemStackMixin.java`: sistema "Axes as Weapons" (`/gameset axesarewepons <true|false>`). Le asce consumano solo 1 durabilità in attacco (come le spade) e possono ricevere incantesimi da spada (Saccheggio, Aspetto di Fuoco, Affilatezza, ecc.) sia al tavolo che all'incudine. |
| `goldenrose01.enchlib.network` | Pacchetti di sincronizzazione e gestione autorizzazioni (solo OP di livello 2+ possono modificare le impostazioni di mondo). |

### B. Package Client (`src/client/java` e `src/client/kotlin`)
* `goldenrose01.enchlib.EnchlibClient.kt`: entrypoint client.
* `goldenrose01.enchlib.modmenu`: integrazione con ModMenu (`ModMenuIntegration.java`), schermate di configurazione interattive basate su Cloth Config (`ClothConfigScreen.java`) o YACL per modificare categorie mob, regole villici, tree cut e axes as weapons.
* `goldenrose01.enchlib.network.ClientConfigSync`: ricezione e applicazione della snapshot di configurazione inviata dal server.

---

## 3. Funzionalità Chiave della Libreria

### A. Configurazione per Mondo (World-Specific JSON5)
Ogni mondo mantiene una cartella dedicata `<world>/config/enchlib/` con i file:
* `AviableEnch.json5`: abilita o disabilita singoli incantesimi (vanilla o moddati).
* `EnchantmentsDetails.json5`: definizioni dettagliate, descrizioni e proprietà.
* `MobCategory.config`: mappatura di tutti i 90+ mob viventi di Minecraft (inclusi Breeze, Bogged, Armadillo, Nautilus, Creaking, ecc.) nelle categorie canoniche e geografiche.
* `EnchCompatibility.config` / `EnchUNcompatibility.config`: matrice di incompatibilità e conflitti tra incantesimi.
* `EnchLVLmax.config`: cap di livello massimo configurabile per incanto.
* `EnchMultiplier.config`: moltiplicatori di costo e potenza.
* `EnchRarity.config`: classificazione delle rarità.

### B. Comandi
1. **`/plusec`**:
   * Applicazione, rimozione e modifica di incantesimi e livelli sull'oggetto nella mano principale con sincronizzazione inventario lato client.
2. **`/gameset axesarewepons <true|false>`**:
   * Attiva o disattiva il comportamento delle asce come armi (durabilità consumata ridotta a 1 e accettazione incantesimi da spada).
3. **`/gameset treecut <off|fell|chop>`**:
   * Regola la modalità di taglio alberi con ascia in sneak.

### C. Integrazione con Eldritch Surge
* Eldritch Surge consulta `EnchLibMobCategories` e `EnchLibApi` (se presenti nel classpath) per verificare le categorie dei mob target e i cap personalizzati.
* EnchLib rileva la presenza di Eldritch Surge per potenziare automaticamente il tree felling quando l'ascia possiede `eldrich-surge:lumberjack`.

---

## 4. Script e Istruzioni di Build

1. **Compilazione JAR Minecraft 26.3:**
   ```powershell
   ./gradlew jar
   ```
   Produce: `jar/enchlib-mc26.3-1.8.0.jar`

2. **Compilazione JAR Minecraft 26.4 Snapshot 8:**
   ```powershell
   ./gradlew jar "-Pminecraft_version=26.4-snapshot-8" "-Pfabric_api_version=0.166.2+26.4"
   ```
   Produce: `jar/enchlib-mc26.4-snapshot-8-1.8.0.jar`

3. **Verifica e Test:**
   ```powershell
   ./gradlew check
   ```

> [!IMPORTANT]
> **Non cancellare mai** i file JAR presenti nella cartella `jar/`.
