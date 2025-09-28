# EnchLib - Libreria per Incantesimi Minecraft

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.17.2+-blue)
![Java Version](https://img.shields.io/badge/Java-22-orange)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)

EnchLib è una libreria per Minecraft Fabric che fornisce un sistema completo e altamente configurabile per la gestione degli incantesimi. Progettata per server e modpack, offre controllo granulare su ogni aspetto degli incantesimi tramite file di configurazione esterni.

```text
    Up-to-date:
    questa documentazione è aggiornata alla versione v1.0.1.1 (release per MC 1.21.8).
    Vedi la sezione Roadmap v1.0.1.2 per i miglioramenti pianificati.
   ```
## 🌟 Caratteristiche Principali

- **Sistema di Configurazione (per-mondo):** file .json5 in saves/<world>/config/enchlib/ con autopopolamento dal registry runtime per personalizzare ogni aspetto degli incantesimi vanilla e non
- **Comandi Potenti**: `/plusec` per gestione incantesimi e `/plusec-debug` per debugging
- **API Pubblica (in preparazione)**: Interfacce per altre mod per integrare il sistema
- **Validazione Completa**: Sistema di controllo errori e fallback automatici
- **Validazione**: controllo di base dei dati e fallback automatici
- **Compat con Fabric 1.21.8:** uso di RegistryKeys.ENCHANTMENT e ItemEnchantmentsComponent.Builder
- **Tab Creativa**: Showcase degli incantesimi disponibili
```text
    Le storiche 9 .config globali della 1.0.0 sono in corso di migrazione verso formato JSON per-mondo.
    In v1.0.1.1 è attivo solo AviableEnch.json5
```
## 📋 Requisiti

- **Minecraft**: 1.21.8 o superiore
- **Fabric Loader**: 0.17.2 o superiore  
- **Fabric API**: Versione compatibile con 1.21.8
- **Java**: 22 o superiore
- **Fabric Kotlin**: Per supporto Kotlin

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

3. **Installa** il file `.jar` risultante nella cartella `mods` di Minecraft
4. **Avvia Mondo/server:** si genererà la cartella `config` interna a quella del mondo
## 📁 Struttura del Progetto

```
enchlib/
├─ src/
│  ├─ main/
│  │  ├─ kotlin/goldenrose01/enchlib/
│  │  │  ├─ EnchLib.kt                  # Classe principale
│  │  │  ├─ EnchLibClient.kt            # Client initialization  
│  │  │  ├─ EnchLibDataGenerator.kt     # Data generation
│  │  │  ├─ api/                        # API pubblica
│  │  │  ├─ commands/                   # Comandi /plusec
│  │  │  ├─ config/                     # ConfigManager
│  │  │  ├─ item/                       # Tab creativa
│  │  │  ├─ registry/                   # Registry incantesimi
│  │  │  └─ utils/                      # Utilities (EnchLogger)
│  │  ├─ resources/
│  │  │  ├─ assets/enchlib/             # Assets (icone, lang)
│  │  │  ├─ data/enchlib/               # Datapack generati
│  │  │  ├─ dumpench/                   # file .json5 previsti
│  │  │  ├─ config/                     # File .config di default
│  │  │  ├─ fabric.mod.json             # Metadata mod
│  │  │  ├─ enchlib.mixins.json         # Config mixin
│  │  │  └─ enchlib.client.mixins.json  # Config mixin client
│  │  └─ java/mixin/                    # Mixin Java
│  └─ client/                           # Source set client
├─ build.gradle                         # Config Gradle
└─ gradle.properties                    # Versioni dipendenze
```

## ⚙️ File di Configurazione

Percorso per-mondo
```swift
saves/<world>/config/enchlib/
```
Formato json:
```json5
{
  "enchantments": [
    { "id": "minecraft:sharpness", "level": 1 },
    { "id": "minecraft:unbreaking", "level": 1 }
  ]
}
```

- Il file viene generato automaticamente alla prima apertura mondo.
- In questa versione, i livelli vengono inizializzati a 1 (vedi Roadmap per max level).
- Puoi modificare a mano e poi usare /plusec-debug reload.
- 
### 📜 AviableEnch.json5
Definisce quali incantesimi sono disponibili e da quali sorgenti:
```
 { "id": "minecraft:aqua_affinity", "enabled": true },
 { "id": "minecraft:bane_of_arthropods", "enabled": true },
```

### 📊 EnchantmentsDetails.json5  
Definisce le caratteristiche di ogni enchant:
```json5
"enchantments": [
    {
      "id": "minecraft:aqua_affinity",
      "name": "Aqua Affinity",
      "max_level": 1,
      "applicable_to": [
        "helmet"
      ],
      "description": "Increases underwater mining speed.",
      "enc_category": [
        "Utility"
      ],
      "mob_category": [
        "none"
      ],
      "rarity": "common",
      "levels": [
        {
          "level": 1,
          "mining_speed_multiplier": 1.2
        }
      ]
    },
    {
      "id": "minecraft:bane_of_arthropods",
      "name": "Bane of Arthropods",
      "max_level": 10,
      "applicable_to": [
        "sword",
        "axe"
      ],
      "description": "Increases damage against arthropods.",
      "enc_category": [
        "Damage"
      ],
      "mob_category": [
        "arthropods"
      ],
      "rarity": "uncommon",
      "levels": [
        {
          "level": 1,
          "extra_damage": 2.5
        },
        {
          "level": 2,
          "extra_damage": 5
        },
        {
          "level": 3,
          "extra_damage": 7.5
        },
        {
          "level": 4,
          "extra_damage": 10
        },
        {
          "level": 5,
          "extra_damage": 12.5
        },
        {
          "level": 6,
          "extra_damage": 15
        },
        {
          "level": 7,
          "extra_damage": 17.5
        },
        {
          "level": 8,
          "extra_damage": 20
        },
        {
          "level": 9,
          "extra_damage": 22.5
        },
        {
          "level": 10,
          "extra_damage": 25
        }
      ]
    },
```

### 🔢 Mob_category.json
Segnala il Mob e la sua categoria (interazione con eventuali incantesimi):
```
{
  "mobs": [
    { "name": "Allay", "type": "Passive", "categories": ["magik","flying"] },
    { "name": "Armadillo", "type": "Passive", "categories": ["animals","cubic"] },
    { "name": "Axolotl", "type": "Pet", "categories": ["animals","water","arthropods"] },
    { "name": "Bat", "type": "Passive", "categories": ["animals","flying"] },
    { "name": "Bee", "type": "Neutral", "categories": ["flying"] },
    { "name": "Blaze", "type": "Hostile", "categories": ["magik","hell","flying"] },
    { "name": "Bogged", "type": "Hostile", "categories": ["undead","fungi"] },
    { "name": "Breeze", "type": "Hostile", "categories": ["magik","flying"] },
    { "name": "Camel", "type": "Passive", "categories": ["animals"] },
    { "name": "Cat", "type": "Pet", "categories": ["animals"] },
    { "name": "Cave spider", "type": "H-Neutral", "categories": ["arthropods"] },
    { "name": "Chicken", "type": "Passive", "categories": ["animals"] },
    { "name": "Cod", "type": "Passive", "categories": ["water"] },
    { "name": "Copper Golem", "type": "Golem", "categories": ["magik","cubic"] },
    { "name": "Cow", "type": "Passive", "categories": ["animals"] },
    { "name": "Creeper", "type": "Hostile", "categories": ["fungi","arthropods"] },
    { "name": "Dog", "type": "Pet", "categories": ["animals"] },
    { "name": "Dolphin", "type": "Neutral", "categories": ["animals","water"] },
    { "name": "Donkey", "type": "Passive", "categories": ["animals"] },
    { "name": "Drowned", "type": "H-Neutral", "categories": ["undead","water"] },
```

### 🔧 Uncompatibility.json5
Compatibilità con strumenti e gruppi(da configurare) :
```
{
  "enchantments": [
    { "id": "minecraft:aqua_affinity", "incompatible_with": [] },
    { "id": "minecraft:bane_of_arthropods", "incompatible_with": ["minecraft:smite", "minecraft:sharpness"] },
    { "id": "minecraft:binding_curse", "incompatible_with": [] },
    { "id": "minecraft:blast_protection", "incompatible_with": ["minecraft:fire_protection", "minecraft:projectile_protection", "minecraft:protection"] },
    { "id": "minecraft:breach", "incompatible_with": [] },
    { "id": "minecraft:channeling", "incompatible_with": ["minecraft:riptide"] },
    { "id": "minecraft:depth_strider", "incompatible_with": ["minecraft:frost_walker"] },
    { "id": "minecraft:efficiency", "incompatible_with": [] },
```


### 💎 Rarity.config (prevista introduzione)
Sistema di rarità completo:
```
# Rarity.config
legendary=0.02,5,treasure_only|boss_drop
common=0.25,100,enchanting_table|villager_trade
```

## 🎮 Comandi

### `/plusec` - Gestione Incantesimi
```bash
/plusec add <enchantment> [level]     # Aggiungi incantesimo all'item in mano
/plusec remove <enchantment>          # Rimuovi incantesimo all'item in mano
/plusec list                          # Lista incantesimi sull'item in mano
/plusec clear                         # Rimuovi tutti incantesimi sull'item in mano
/plusec info <enchantment>            # Info dettagliate incantesimo (legge parte del json)
```

### `/plusec-debug` - Debugging e Statistiche
```bash
/plusec-debug --stats                 # Statistiche sistema
/plusec-debug --check-configs         # Verifica configurazioni
/plusec-debug --dump-config           # Dump completo config
/plusec-debug --check-conflicts       # Controlla conflitti
/plusec-debug --reload-configs        # Ricarica configurazioni
/plusec-debug --registry-info         # Info registry incantesimi
/plusec-debug --debug-toggle          # Toggle debug mode
```

## 🔌 API per Sviluppatori (future implementation)

EnchLib fornisce un'API pubblica per altre mod:

```kotlin

```

## 🛠️ Sviluppo e Contributi

### Setup Ambiente di Sviluppo
1. Clona il repository
2. Apri in IntelliJ IDEA o VS Code
3. Esegui `./gradlew genSources` per generare le sources
4. Configura SDK Java 22

### Build e Test
```bash
# Build completo
./gradlew build

# Test rapido
./gradlew runClient

# Generazione dati
./gradlew runDatagen
```

### Struttura Codice
- **Kotlin**: Linguaggio principale per logica mod
- **Java**: Solo per mixin quando necessario
- **Split Sources**: Client e common separati per sicurezza
- **Fabric Loom**: Build system ottimizzato

## 🐛 Debugging e Troubleshooting

### Problemi Comuni

**1. Errori di Configurazione**
```bash
/plusec-debug --check-configs
# Controlla file .config per errori di sintassi
```

**2. Conflitti Incantesimi**
```bash
/plusec-debug --check-conflicts  
# Verifica incompatibilità circolari
```

**3. Performance Issues**
```bash
/plusec-debug --stats
# Mostra statistiche sistema
```

### Log Files
I log dettagliati sono disponibili in:
- `logs/latest.log`: Log generale Minecraft
- Debug mode: Output verboso in console

### Modalità Debug
```bash
/plusec-debug --debug-toggle
# Attiva output verboso per troubleshooting
```

## 📚 Documentazione Estesa

### File di Configurazione Avanzati

Ogni file `.config` supporta:
- **Commenti**: Linee che iniziano con `#`
- **Validazione**: Controllo automatico errori
- **Fallback**: Valori di default se mancanti
- **Hot Reload**: Ricaricamento tramite comando

### Sistema di Rarità

Il sistema di rarità influenza:
- Probabilità tavolo incantesimi
- Loot casse e fishing
- Costi villager
- Bilanciamento generale

### Compatibilità Strumenti

Supporta gruppi multi-livello:
```
tools → pickaxes → minecraft:diamond_pickaxe
weapons → swords → minecraft:netherite_sword
armor → chest_armor → minecraft:diamond_chestplate
```

## 🔄 Aggiornamenti e Migrazione

🧪 Stato attuale & Limitazioni (v1.0.1.1)

✅ Generazione forzata del file AviableEnch.json5 alla prima apertura mondo

✅ Autopopolamento con tutti gli incantesimi (vanilla + mod) dal registry runtime

✅ /plusec add, /plusec list, /plusec-debug reload, /plusec-debug validate operativi

⚠️ Livelli iniziali attualmente = 1 (non il max nativo)

⚠️ /plusec list mostra la config, non l’oggetto in mano

⚠️ /plusec add non applica ancora l’incantesimo all’item del giocatore

⚠️ Autocomplete assente

⚠️ /plusec del/remove temporaneamente assente

### Breaking Changes
- v1.0.0: Prima release stabile

### Stile & Architettura

Kotlin come linguaggio principale

Nessuna serializzazione di classi Mojang su disco (solo DTO semplici)

Runtime registry (RegistryKeys.ENCHANTMENT, RegistryEntry) per compatibilità con Yarn 1.21.8

Config per-mondo, merge non distruttivo pianificato

### 🐛 Troubleshooting

Il file non viene creato: verifica log; il bootstrap copia dal template (se presente) o autopopola. Controlla path saves/<world>/config/enchlib/.

Reload fallisce: controlla la sintassi JSON5 (virgole finali, commenti). In caso di errore, il sistema rigenera un file valido.

Comandi senza output atteso: ricorda che in v1.0.1.1 add/list agiscono/mostrano la config e non gli incantesimi effettivi sull’item.

### 🗓️ Changelog
v1.0.1.1

Compatibilità Minecraft 1.21.8 (Fabric/Yarn)

Autopopolamento AviableEnch.json5 per-mondo (dal registry runtime: vanilla + mod)

Reload live della config e validazione di base

/plusec add (scrive in config), /plusec list (mostra config), /plusec-debug reload|validate

### 🧭 Roadmap – v1.0.1.2
Obiettivi principali

1. Livello iniziale corretto (MAX)
   - Leggere il max level nativo per ogni incantesimo in autopopulate

   - Policy configurabile: initialLevelPolicy = MAX | ONE (default: MAX)

2. Autocomplete e ID robusti

    - Suggerimenti Brigadier per /plusec add

    - Normalizzazione input: sharpness → minecraft:sharpness se manca il namespace

3. Applicazione agli oggetti

    - /plusec add aggiorna anche l’Item in mano (ItemEnchantmentsComponent.Builder)

    - Merge livelli coerente

4. Lista duale

   - plusec list (default): mostra gli incantesimi dell’oggetto

   - plusec list --json: mostra la config

5. Rimozione

   - /plusec remove|del <id> con flag --item-only (default), --config-only, --both

4. Validate potenziato

    - validate json / validate item con report dettagliati

## 📄 Licenza

Questo progetto è rilasciato sotto licenza **CC0-1.0** (Creative Commons Zero). 
Puoi usare, modificare e distribuire liberamente senza restrizioni.

## 🙏 Ringraziamenti

- **Fabric Team**: Per l'eccellente mod loader
- **Yarn Team**: Per i mapping aggiornati  
- **Kotlin Team**: Per il supporto Fabric Kotlin
- **Community**: Per feedback e testing

## 📞 Supporto e Community

- **Issues**: [GitHub Issues](https://github.com/GoldenRose01/enchlib/issues)
- **Discussions**: [GitHub Discussions](https://github.com/GoldenRose01/enchlib/discussions)
---

*Developed with ❤️ by GoldenRose01 for the Minecraft Fabric community*
