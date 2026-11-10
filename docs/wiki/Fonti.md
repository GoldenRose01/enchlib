# 📚 Fonti e versione

[Home](Home) · [API](API)

Documentazione aggiornata a **EnchLib 1.7.0 / Minecraft 26.3 e 26.4 Snapshot 7** il **10 novembre 2026**. Le due build sono state compilate e i metadati dei JAR verificati; non è ancora stata completata la verifica in gioco.

| Argomento | Fonte |
|---|---|
| Versioni | `gradle.properties`, `fabric.mod.json` |
| Schema operativo | `config/GlobalConfigIO.kt` |
| Installazione e migrazione dei preset | `config/GlobalConfigBootstrap.kt` |
| Comandi reali | `commands/EnchLibCommands.kt`, `commands/DebugCommands.kt` |
| Permessi e sincronizzazione | `network/ConfigSyncNetworking.kt` |
| Categorie mob | `config/MobCategoryManager.kt` |
| API pubblica | `api/EnchLibAPI.kt` |
| Filtro commerci | `src/main/java/goldenrose01/enchlib/mixin/VillagerTradeMixin.java`, `src/main/java/goldenrose01/enchlib/trading/VillagerEnchantmentRules.java` |
| Menu dei commerci | `src/client/java/goldenrose01/enchlib/modmenu/ClothConfigScreen.java`, `src/client/java/goldenrose01/enchlib/modmenu/MobCategoryScreen.java` |
| Modifiche dalla 1.6.1 | `CHANGELOG.md`, [Novità 1.7.0](Novita-1.7.0) |
| Catalogo preset | `src/main/resources/config/*.config` |

Alcuni file storici del progetto descrivono schemi o comandi differenti. Questa wiki usa lo schema operativo e i rami realmente registrati, distinguendo preset da valori attivi e proprietà configurabili da effetti effettivamente integrati.

Il catalogo degli incantamenti vanilla riporta i valori dei preset inclusi, non afferma che siano i massimi vanilla né che tutte le mod li applichino. Le voci di Eldrich Surge sono collegate alla sua wiki per evitarne la duplicazione.

La struttura editoriale di indice, schede, oggetti e incompatibilità prende spunto da [Enchantment — Minecraft Wiki](https://minecraft.wiki/w/Enchantment). Il contenuto specifico di EnchLib proviene dal suo codice e dai suoi preset.

[Sorgenti](https://github.com/GoldenRose01/enchlib) · [Guida API estesa](https://github.com/GoldenRose01/enchlib/blob/main/docs/API_INTEGRATION.md)
