# EnchLib - Libreria Avanzata per Incantesimi Minecraft

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.17.2+-blue)
![Java Version](https://img.shields.io/badge/Java-22-orange)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)

EnchLib è una potente libreria per Minecraft Fabric che fornisce un sistema completo e altamente configurabile per la gestione degli incantesimi. Progettata per server e modpack, offre controllo granulare su ogni aspetto degli incantesimi tramite file di configurazione esterni.

## 🌟 Caratteristiche Principali

- **Sistema di Configurazione Avanzato**: 9 file `.config` per personalizzare ogni aspetto degli incantesimi
- **Comandi Potenti**: `/plusec` per gestione incantesimi e `/plusec-debug` per debugging
- **API Pubblica**: Interfacce per altre mod per integrare il sistema
- **Validazione Completa**: Sistema di controllo errori e fallback automatici
- **Split Client/Server**: Architettura sicura e ottimizzata
- **Data Generation**: Generazione automatica di tag e risorse
- **Tab Creativa**: Showcase degli incantesimi disponibili

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

2. **Esegui lo script di installazione**:
   ```bash
   chmod +x install_files.sh
   ./install_files.sh
   ```

3. **Compila la mod**:
   ```bash
   ./gradlew build
   ```

4. **Installa** il file `.jar` risultante nella cartella `mods` di Minecraft

## 📁 Struttura del Progetto

```
enchlib/
├─ src/
│  ├─ main/
│  │  ├─ kotlin/goldenrose01/enchlib/
│  │  │  ├─ EnchLib.kt                 # Classe principale
│  │  │  ├─ EnchLibClient.kt           # Client initialization  
│  │  │  ├─ EnchLibDataGenerator.kt    # Data generation
│  │  │  ├─ api/                       # API pubblica
│  │  │  ├─ commands/                  # Comandi /plusec
│  │  │  ├─ config/                    # ConfigManager
│  │  │  ├─ item/                      # Tab creativa
│  │  │  ├─ registry/                  # Registry incantesimi
│  │  │  └─ utils/                     # Utilities (EnchLogger)
│  │  ├─ resources/
│  │  │  ├─ assets/enchlib/            # Assets (icone, lang)
│  │  │  ├─ data/enchlib/             # Datapack generati
│  │  │  ├─ config/                    # File .config di default
│  │  │  ├─ fabric.mod.json           # Metadata mod
│  │  │  ├─ enchlib.mixins.json       # Config mixin
│  │  │  └─ enchlib.client.mixins.json # Config mixin client
│  │  └─ java/mixin/                  # Mixin Java
│  └─ client/                         # Source set client
├─ build.gradle                       # Config Gradle
├─ gradle.properties                  # Versioni dipendenze
└─ install_files.sh                   # Script installazione
```

## ⚙️ File di Configurazione

EnchLib utilizza 9 file `.config` nella cartella `.minecraft/config/enchlib/`:

### 📜 AviableEnch.config
Definisce quali incantesimi sono disponibili e da quali sorgenti:
```
minecraft:sharpness=enchanting_table,chest_loot,villager_trade
minecraft:mending=chest_loot,villager_trade,fishing
```

### 📊 EnchMultiplierCSV.config  
Moltiplicatori di effetto per ogni livello (formato CSV):
```
minecraft:sharpness, 1, 0.5, 0.0
minecraft:sharpness, 2, 1.0, 0.0
minecraft:protection, 1, 0.04, 0.0
```

### 🔢 EnchLVLmax.config
Livelli massimi per ogni incantesimo:
```
minecraft:sharpness=5
minecraft:protection=4
minecraft:efficiency=5
```

### 🔧 EnchCompatibility.config
Compatibilità con strumenti e gruppi:
```
minecraft:sharpness=swords,axes
minecraft:efficiency=tools
minecraft:protection=armor
```

### 🏷️ MobCategory.config
Categorizzazione mob per effetti specifici:
```
minecraft:zombie=undead,hostile,humanoid
minecraft:spider=arthropod,neutral
minecraft:guardian=aquatic,hostile
```

### 📂 EnchCategory.config
Categorie di incantesimi per bilanciamento:
```
minecraft:sharpness=damage,combat
minecraft:protection=protection
minecraft:fortune=utility,mining
```

### ⚡ EnchUNcompatibility.config
Incompatibilità tra incantesimi:
```
minecraft:sharpness=minecraft:smite,minecraft:bane_of_arthropods
minecraft:silk_touch=minecraft:fortune
```

### 💎 EnchRarity.config e Rarity.config
Sistema di rarità completo:
```
# EnchRarity.config
minecraft:mending=legendary
minecraft:sharpness=common

# Rarity.config
legendary=0.02,5,treasure_only|boss_drop
common=0.25,100,enchanting_table|villager_trade
```

## 🎮 Comandi

### `/plusec` - Gestione Incantesimi
```bash
/plusec add <enchantment> [level]     # Aggiungi incantesimo
/plusec remove <enchantment>          # Rimuovi incantesimo  
/plusec list                          # Lista incantesimi item
/plusec clear                         # Rimuovi tutti incantesimi
/plusec info <enchantment>            # Info dettagliate incantesimo
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

## 🔌 API per Sviluppatori

EnchLib fornisce un'API pubblica per altre mod:

```kotlin
import goldenrose01.enchlib.api.EnchLibAPI
import goldenrose01.enchlib.registry.EnchantmentRegistry
import goldenrose01.enchlib.config.ConfigManager

// Accesso al registry incantesimi
val sharpness = EnchantmentRegistry.get("minecraft:sharpness")

// Verifica configurazioni
val isEnabled = ConfigManager.isEnchantmentEnabled("minecraft:fortune")
val maxLevel = ConfigManager.getMaxLevel("minecraft:efficiency")

// Future espansioni API
EnchLibAPI.registerCustomEnchantment(...)
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

### Updating da Versioni Precedenti
1. Backup cartella `config/enchlib/`
2. Installa nuova versione
3. Esegui `/plusec-debug --check-configs`
4. Aggiorna configurazioni se necessario

### Breaking Changes
- v1.0.0: Prima release stabile
- Future versions: Compatibilità retroattiva garantita

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
- **Wiki**: [Documentazione Completa](https://github.com/GoldenRose01/enchlib/wiki)

---

*Developed with ❤️ by GoldenRose01 for the Minecraft Fabric community*