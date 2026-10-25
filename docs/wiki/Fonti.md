# 📚 Fonti e versione

[Home](Home) · [API](API)

Documentazione del checkout **EnchLib 1.6.1 / Minecraft 26.3**, verificato il **25 ottobre 2026**.

| Argomento | Fonte |
|---|---|
| Versioni | `gradle.properties`, `fabric.mod.json` |
| Schema operativo | `config/GlobalConfigIO.kt` |
| Installazione e migrazione dei preset | `config/GlobalConfigBootstrap.kt` |
| Comandi reali | `commands/EnchLibCommands.kt`, `commands/DebugCommands.kt` |
| Permessi e sincronizzazione | `network/ConfigSyncNetworking.kt` |
| Categorie mob | `config/MobCategoryManager.kt` |
| API pubblica | `api/EnchLibAPI.kt` |
| Catalogo preset | `src/main/resources/config/*.config` |

Alcuni file storici del progetto descrivono schemi o comandi differenti. Questa wiki usa lo schema operativo e i rami realmente registrati, distinguendo preset da valori attivi e proprietà configurabili da effetti effettivamente integrati.

Il catalogo degli incantamenti vanilla riporta i valori dei preset inclusi, non afferma che siano i massimi vanilla né che tutte le mod li applichino. Le voci di Eldrich Surge sono collegate alla sua wiki per evitarne la duplicazione.

La struttura editoriale di indice, schede, oggetti e incompatibilità prende spunto da [Enchantment — Minecraft Wiki](https://minecraft.wiki/w/Enchantment). Il contenuto specifico di EnchLib proviene dal suo codice e dai suoi preset.

[Sorgenti](https://github.com/GoldenRose01/enchlib) · [Guida API estesa](https://github.com/GoldenRose01/enchlib/blob/main/docs/API_INTEGRATION.md)
