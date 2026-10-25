# ⌨️ Comandi

[Home](Home) · [Configurazione](Configurazione) · [FAQ](FAQ)

I due comandi richiedono permessi da gamemaster, normalmente **operatore di livello 2**. Per i comandi che agiscono su un oggetto, impugna un oggetto valido nella mano principale.

## `/plusec`: oggetto impugnato

| Comando | Azione |
|---|---|
| `/plusec add <id> [livello]` | Aggiunge o aggiorna un incantamento; livello predefinito 1 |
| `/plusec remove <id>` | Rimuove l’incantamento selezionato |
| `/plusec list` | Elenca quelli sull’oggetto |
| `/plusec clear` | Svuota gli incantamenti, compresi quelli memorizzati sui libri |
| `/plusec info <id>` | Mostra informazioni sull’ID e sulla configurazione |
| `/plusec repair` | Ripara l’oggetto |
| `/plusec setdurability <percentuale>` | Imposta la durabilità residua in percentuale |
| `/plusec damage <punti>` | Modifica il danno dell’oggetto; il segno determina danno o riparazione |

```text
/plusec add minecraft:unbreaking 3
/plusec list
/plusec repair
```

Il ramo `addid` citato da alcune vecchie istruzioni non è registrato nel comando corrente: usa gli ID completi nel ramo `add`. Autocompletamento e controlli del registry aiutano a evitare ID storici non validi.

## `/plusec-debug`: configurazione

| Comando | Azione |
|---|---|
| `show-path` | Mostra la cartella attiva |
| `reload` | Ricarica la configurazione del mondo |
| `validate` | Confronta gli ID configurati con il registry |
| `list-enabled` | Elenca gli ID abilitati |
| `toggle <id> <true/false>` | Imposta la disponibilità |
| `setmax <id> <livello>` | Imposta il livello massimo configurato |
| `config read <id>` | Legge disponibilità e livello massimo |
| `config write enabled <id> <true/false>` | Scrive la disponibilità |
| `config write max-level <id> <livello>` | Scrive il massimo |
| `mob get <entity-id>` | Legge le categorie del mob |
| `mob set <entity-id> <categorie>` | Imposta un elenco separato da virgole; `none` lo svuota |

Anteponi `/plusec-debug` a ciascun ramo:

```text
/plusec-debug show-path
/plusec-debug setmax minecraft:sharpness 10
/plusec-debug mob set minecraft:zombie undead,hostile
/plusec-debug reload
/plusec-debug validate
```

I metodi di scrittura salvano subito i valori. Un massimo configurato non garantisce che un sistema esterno lo usi: consulta [API](API).

Fonti: [EnchLibCommands](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/commands/EnchLibCommands.kt), [DebugCommands](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/commands/DebugCommands.kt).
