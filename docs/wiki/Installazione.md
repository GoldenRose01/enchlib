# 📦 Installazione

[Home](Home) · [Configurazione](Configurazione)

| Componente | Versione documentata |
|---|---|
| Minecraft | 26.3 |
| EnchLib | 1.6.1 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Fabric Language Kotlin | 1.13.11+kotlin.2.3.21 |
| Java | 25 |

1. Prepara un’installazione Fabric compatibile con la versione di Minecraft.
2. Metti EnchLib e le sue dipendenze nella cartella `mods` dell’istanza.
3. Avvia e apri un mondo: la configurazione viene associata a quel salvataggio.
4. Usa `/plusec-debug show-path` per verificare la cartella attiva.

Per l’editor integrato consulta le opzioni offerte dalla schermata della mod; su un server le modifiche richiedono i permessi previsti. Per sincronizzare la configurazione all’editor client, EnchLib deve essere presente anche sul client.

Eldrich Surge è un’integrazione separata: installarla è necessario soltanto per le sue meccaniche e i suoi incantamenti.

Fonti: [versioni](https://github.com/GoldenRose01/enchlib/blob/main/gradle.properties), [metadati Fabric](https://github.com/GoldenRose01/enchlib/blob/main/src/main/resources/fabric.mod.json).
