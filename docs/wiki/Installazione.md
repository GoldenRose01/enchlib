# 📦 Installazione

[Home](Home) · [Configurazione](Configurazione)

Le due build della **1.7.0** sono compilate per versioni distinte: scegli il JAR corrispondente alla tua istanza.

| Componente | Build 26.3 | Build snapshot |
|---|---|---|
| Minecraft | 26.3 | 26.4 Snapshot 7 (`26.4-snapshot-7`) |
| EnchLib | 1.7.0 | 1.7.0 |
| JAR | `enchlib-mc26.3-1.7.0.jar` | `enchlib-mc26.4-snapshot-7-1.7.0.jar` |
| Fabric Loader usato nella build | 0.19.5 | 0.20.0 |
| Fabric API usata nella build | 0.161.0+26.3 | 0.165.4+26.4 |
| Fabric Language Kotlin | 1.13.11+kotlin.2.3.21 | 1.13.11+kotlin.2.3.21 |
| Java | 25 | 25 |

La Fabric API indicata per lo snapshot richiede Fabric Loader **0.20.0 o successivo**. Installa il JAR normale; quello con suffisso `-sources` contiene i sorgenti. Entrambe le build sono state compilate e i metadati verificati; il test in gioco resta da completare. Il supporto snapshot qui documentato riguarda Snapshot 7, non implica compatibilità con snapshot successivi.

1. Prepara un’installazione Fabric compatibile con la versione di Minecraft.
2. Metti EnchLib e le sue dipendenze nella cartella `mods` dell’istanza.
3. Avvia e apri un mondo: la configurazione viene associata a quel salvataggio.
4. Usa `/plusec-debug show-path` per verificare la cartella attiva.

Per l’editor integrato consulta le opzioni offerte dalla schermata della mod; su un server le modifiche richiedono i permessi previsti. Per sincronizzare la configurazione all’editor client, EnchLib deve essere presente anche sul client.

Eldrich Surge è un’integrazione separata: installarla è necessario soltanto per le sue meccaniche e i suoi incantamenti.

## Aggiornamento dalla 1.6.1

Sostituisci il vecchio JAR con quello della 1.7.0 per la stessa versione di Minecraft. I file del mondo possono essere conservati: se i nuovi campi dei commerci sono assenti, si applica automaticamente il preset che esclude le maledizioni. Le offerte già salvate non vengono riscritte. Vedi [Novità 1.7.0](Novita-1.7.0).

Fonti: [versioni](https://github.com/GoldenRose01/enchlib/blob/main/gradle.properties), [metadati Fabric](https://github.com/GoldenRose01/enchlib/blob/main/src/main/resources/fabric.mod.json).
